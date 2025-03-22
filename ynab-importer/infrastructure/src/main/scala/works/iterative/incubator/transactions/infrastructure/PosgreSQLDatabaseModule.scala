package works.iterative.incubator.transactions
package infrastructure

import zio.*
import service.{
    TransactionRepository,
    SourceAccountRepository,
    TransactionProcessingStateRepository
}

/** Combines all database-related components into a single module
  */
object PosgreSQLDatabaseModule:
    /** Repository type that includes all our repository interfaces
      */
    type Repositories =
        TransactionRepository & SourceAccountRepository & TransactionProcessingStateRepository

    /** Creates a ZLayer with all repositories
      */
    val layer: ZLayer[Scope, Throwable, Repositories] =
        // Create the shared data source
        val dataSourceLayer = PostgreSQLDataSource.managedLayer
        // Create the transactor from the data source
        val transactorLayer = dataSourceLayer >>> PostgreSQLTransactor.managedLayer
        // Create all repositories using the shared transactor
        val repoLayers = transactorLayer >>> (
            PostgreSQLTransactionRepository.layer ++
                PostgreSQLSourceAccountRepository.layer ++
                PostgreSQLTransactionProcessingStateRepository.layer
        )

        // Return the combined layer
        repoLayers
    end layer

    /** Helper method to create FlywayConfig with optional additional locations
      */
    private def createFlywayConfig(additionalLocations: List[String] = List.empty): FlywayConfig =
        if additionalLocations.nonEmpty then
            FlywayConfig(locations = FlywayConfig.DefaultLocation :: additionalLocations)
        else
            FlywayConfig.default
    
    /** Creates a ZLayer with all repositories and runs migrations first
      *
      * @param additionalLocations
      *   Additional classpath locations to scan for migrations
      */
    def layerWithMigrations(
        additionalLocations: List[String] = List.empty
    ): ZLayer[Scope, Throwable, Repositories] =
        // Create the shared data source
        val dataSourceLayer = PostgreSQLDataSource.managedLayer

        // Create flyway config using the helper method
        val flywayConfig = createFlywayConfig(additionalLocations)

        // Create the flyway migration service with config
        val flywayLayer = dataSourceLayer >>> FlywayMigrationService.layerWithConfig(flywayConfig)

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

    /** Runs flyway migrations with custom locations
      *
      * @param additionalLocations
      *   Additional classpath locations to scan for migrations
      */
    def migrate(
        additionalLocations: List[String] = List.empty
    ): ZIO[Scope, Throwable, Unit] =
        val dataSourceLayer = PostgreSQLDataSource.managedLayer

        // Create flyway config using the helper method
        val flywayConfig = createFlywayConfig(additionalLocations)

        val flywayLayer = dataSourceLayer >>> FlywayMigrationService.layerWithConfig(flywayConfig)

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
