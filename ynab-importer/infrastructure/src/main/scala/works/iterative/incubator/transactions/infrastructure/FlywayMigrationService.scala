package works.iterative.incubator.transactions.infrastructure

import zio.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

/** Service for managing database migrations using Flyway
  */
trait FlywayMigrationService:
    def migrate(): Task[MigrateResult]
    def clean(): Task[Unit]
    def validate(): Task[Unit]
    def info(): Task[Unit]
end FlywayMigrationService

/** Configuration for Flyway migrations
  *
  * @param locations
  *   Classpath locations to scan for migrations
  */
case class FlywayConfig(
    locations: List[String] = List(FlywayConfig.DefaultLocation)
)

object FlywayConfig:
    /** Default location for migrations */
    val DefaultLocation = "classpath:db/migration"
    
    /** Default config with standard locations */
    val default: FlywayConfig = FlywayConfig()

class PostgreSQLFlywayMigrationService(
    dataSource: PostgreSQLDataSource,
    config: FlywayConfig
) extends FlywayMigrationService:

    private val flyway =
        // Configure Flyway with datasource and migration locations
        val flywayConfig = Flyway.configure()
            .dataSource(dataSource.dataSource)
            .locations(config.locations*)

        flywayConfig.load()
    end flyway

    override def migrate(): Task[MigrateResult] =
        ZIO.attempt(flyway.migrate())

    override def clean(): Task[Unit] =
        ZIO.attempt(flyway.clean()).unit

    override def validate(): Task[Unit] =
        ZIO.attempt(flyway.validate()).unit

    override def info(): Task[Unit] =
        ZIO.attempt(flyway.info()).unit
end PostgreSQLFlywayMigrationService

object FlywayMigrationService:
    /** Creates a layer with default Flyway configuration
      */
    val layer: ZLayer[PostgreSQLDataSource, Throwable, FlywayMigrationService] =
        ZLayer.fromFunction { (ds: PostgreSQLDataSource) =>
            new PostgreSQLFlywayMigrationService(ds, FlywayConfig.default)
        }

    /** Creates a layer with custom Flyway configuration
      */
    def layerWithConfig(
        config: FlywayConfig
    ): ZLayer[PostgreSQLDataSource, Throwable, FlywayMigrationService] =
        ZLayer.fromFunction { (ds: PostgreSQLDataSource) =>
            new PostgreSQLFlywayMigrationService(ds, config)
        }

    def migrate: ZIO[FlywayMigrationService, Throwable, MigrateResult] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.migrate())

    def clean: ZIO[FlywayMigrationService, Throwable, Unit] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.clean())

    def validate: ZIO[FlywayMigrationService, Throwable, Unit] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.validate())

    def info: ZIO[FlywayMigrationService, Throwable, Unit] =
        ZIO.serviceWithZIO[FlywayMigrationService](_.info())
end FlywayMigrationService
