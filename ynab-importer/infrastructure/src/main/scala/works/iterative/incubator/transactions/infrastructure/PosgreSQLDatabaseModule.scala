package works.iterative.incubator.transactions
package infrastructure

import zio.*
import service.{TransactionRepository, SourceAccountRepository}

/** Combines all database-related components into a single module
  */
object PosgreSQLDatabaseModule:
    /** Creates a ZLayer with both repositories
      */
    val layer: ZLayer[Scope, Throwable, TransactionRepository & SourceAccountRepository] =
        // Create the shared data source
        val dataSourceLayer = PostgreSQLDataSource.managedLayer
        // Create the transactor from the data source
        val transactorLayer = dataSourceLayer >>> PostgreSQLTransactor.managedLayer
        // Create both repositories using the shared transactor
        val repoLayers = transactorLayer >>> (
            PostgreSQLTransactionRepository.layer ++
                PostgreSQLSourceAccountRepository.layer
        )

        // Return the combined layer
        repoLayers
    end layer

    /** Creates a ZLayer with both repositories and runs migrations first
      */
    def layerWithMigrations
        : ZLayer[Scope, Throwable, TransactionRepository & SourceAccountRepository] =
        // Create the shared data source
        val dataSourceLayer = PostgreSQLDataSource.managedLayer
        // Create the flyway migration service
        val flywayLayer = dataSourceLayer >>> FlywayMigrationService.layer

        // Run migrations before providing the repositories
        ZLayer.scoped {
            for
                migrator <- ZIO.service[FlywayMigrationService].provideSome[Scope](flywayLayer)
                _ <- migrator.migrate().tapError(err =>
                    ZIO.logError(s"Migration failed: ${err.getMessage}")
                )
            yield ()
        } >>> layer
    end layerWithMigrations

    /** Runs flyway migrations
      */
    val migrate: ZIO[Scope, Throwable, Unit] =
        val dataSourceLayer = PostgreSQLDataSource.managedLayer
        val flywayLayer = dataSourceLayer >>> FlywayMigrationService.layer

        ZIO.scoped {
            for
                migrator <- ZIO.service[FlywayMigrationService].provideSome[Scope](flywayLayer)
                result <- migrator.migrate()
                _ <- ZIO.logInfo(
                    s"Migration completed: ${result.migrationsExecuted} migrations executed"
                )
            yield ()
        }
    end migrate
end PosgreSQLDatabaseModule
