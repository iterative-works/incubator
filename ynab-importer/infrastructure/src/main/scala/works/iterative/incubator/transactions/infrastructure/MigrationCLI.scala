package works.iterative.incubator.transactions.infrastructure

import zio.*

/** Command-line interface for database migration tasks
  */
object MigrationCLI extends ZIOAppDefault:
    // Simple enum for commands
    enum Command:
        case Migrate, Clean, Info, Validate, Help

    // Parse command from args
    def parseCommand(args: Chunk[String]): Command =
        args.headOption match
            case Some("migrate")  => Command.Migrate
            case Some("clean")    => Command.Clean
            case Some("info")     => Command.Info
            case Some("validate") => Command.Validate
            case _                => Command.Help

    def printHelp: UIO[Unit] =
        Console.printLine(
            """
      |Database Migration CLI
      |
      |Available commands:
      |  migrate   - Run all pending migrations
      |  clean     - Remove all objects from the database
      |  info      - Print information about migrations
      |  validate  - Verify migrations are properly applied
      |  help      - Show this help
      """.stripMargin
        ).orDie

    def program: ZIO[ZIOAppArgs & PostgreSQLDataSource & Scope, Throwable, Unit] =
        for
            args <- getArgs
            command = parseCommand(args)
            _ <- command match
                case Command.Migrate =>
                    ZIO.logInfo("Running migrations...") *>
                        PosgreSQLDatabaseModule.migrate
                case Command.Clean =>
                    ZIO.logInfo("Cleaning database...") *>
                        ZIO.scoped {
                            for
                                ds <- ZIO.service[PostgreSQLDataSource].provideSome[Scope](
                                    PostgreSQLDataSource.managedLayer
                                )
                                migrator <- ZIO.service[FlywayMigrationService].provideSome[
                                    Scope & PostgreSQLDataSource
                                ](FlywayMigrationService.layer)
                                _ <- migrator.clean()
                            yield ()
                        }
                case Command.Info =>
                    ZIO.logInfo("Migration information:") *>
                        ZIO.scoped {
                            for
                                ds <- ZIO.service[PostgreSQLDataSource].provideSome[Scope](
                                    PostgreSQLDataSource.managedLayer
                                )
                                migrator <- ZIO.service[FlywayMigrationService].provideSome[
                                    Scope & PostgreSQLDataSource
                                ](FlywayMigrationService.layer)
                                _ <- migrator.info()
                            yield ()
                        }
                case Command.Validate =>
                    ZIO.logInfo("Validating migrations...") *>
                        ZIO.scoped {
                            for
                                ds <- ZIO.service[PostgreSQLDataSource].provideSome[Scope](
                                    PostgreSQLDataSource.managedLayer
                                )
                                migrator <- ZIO.service[FlywayMigrationService].provideSome[
                                    Scope & PostgreSQLDataSource
                                ](FlywayMigrationService.layer)
                                _ <- migrator.validate()
                                _ <- ZIO.logInfo("Migrations are valid.")
                            yield ()
                        }
                case Command.Help =>
                    printHelp
        yield ()

    override def run: ZIO[Scope & ZIOAppArgs, Throwable, Unit] =
        program.provideSome[Scope & ZIOAppArgs](PostgreSQLDataSource.managedLayer)
end MigrationCLI
