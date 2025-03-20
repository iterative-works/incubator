package works.iterative.incubator.transactions.infrastructure

import zio.*
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

/**
 * Service for managing database migrations using Flyway
 */
trait FlywayMigrationService:
  def migrate(): Task[MigrateResult]
  def clean(): Task[Unit]
  def validate(): Task[Unit]
  def info(): Task[Unit]

class PostgreSQLFlywayMigrationService(dataSource: PostgreSQLDataSource) extends FlywayMigrationService:
  private val flyway = Flyway.configure()
    .dataSource(dataSource.dataSource)
    .load()

  override def migrate(): Task[MigrateResult] = 
    ZIO.attempt(flyway.migrate())
  
  override def clean(): Task[Unit] = 
    ZIO.attempt(flyway.clean()).unit
  
  override def validate(): Task[Unit] = 
    ZIO.attempt(flyway.validate()).unit
  
  override def info(): Task[Unit] = 
    ZIO.attempt(flyway.info()).unit

object FlywayMigrationService:
  val layer: ZLayer[PostgreSQLDataSource, Throwable, FlywayMigrationService] =
    ZLayer.fromFunction { (ds: PostgreSQLDataSource) => 
      new PostgreSQLFlywayMigrationService(ds)
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