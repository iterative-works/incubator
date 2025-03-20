package works.iterative.incubator.transactions.infrastructure

import zio.*

/** Command-line interface for database migration tasks
  */
object MigrationCLI extends ZIOAppDefault:
    // Simple enum for commands
    enum Command:
        case Migrate, Clean, Info, Validate, Help

    // Parse command from args and extract locations
    def parseCommand(args: Chunk[String]): (Command, List[String]) =
        val command = args.headOption match
            case Some("migrate")  => Command.Migrate
            case Some("clean")    => Command.Clean
            case Some("info")     => Command.Info
            case Some("validate") => Command.Validate
            case _                => Command.Help

        // Extract additional locations if provided
        val locations =
            if args.size > 1 && command != Command.Help then
                args.drop(1).toList
            else
                List.empty

        (command, locations)
    end parseCommand

    def printHelp: UIO[Unit] =
        Console.printLine(
            """
      |Database Migration CLI
      |
      |Available commands:
      |  migrate [locations...]  - Run all pending migrations (optional additional classpath locations)
      |  clean                   - Remove all objects from the database
      |  info                    - Print information about migrations
      |  validate                - Verify migrations are properly applied
      |  help                    - Show this help
      |
      |Examples:
      |  migrate                 - Run migrations from default locations
      |  migrate classpath:other/migrations - Run migrations from default and additional locations
      """.stripMargin
        ).orDie

    def program: ZIO[ZIOAppArgs & PostgreSQLDataSource & Scope, Throwable, Unit] =
        for
            args <- getArgs
            (command, locations) = parseCommand(args)
            // Use default config or append additional locations to default
            config = if locations.nonEmpty then
                FlywayConfig(locations =
                    FlywayConfig.DefaultLocation :: locations
                )
            else
                FlywayConfig.default
            _ <- command match
                case Command.Migrate =>
                    ZIO.logInfo(s"Running migrations with locations: ${
                            if locations.isEmpty then "default" else locations.mkString(", ")
                        }") *>
                        PosgreSQLDatabaseModule.migrate(locations)
                case Command.Clean =>
                    ZIO.logInfo("Cleaning database...") *>
                        ZIO.scoped {
                            for
                                ds <- ZIO.service[PostgreSQLDataSource]
                                migrator <- ZIO.service[FlywayMigrationService].provideSome[
                                    Scope & PostgreSQLDataSource
                                ](FlywayMigrationService.layerWithConfig(config))
                                _ <- migrator.clean()
                            yield ()
                        }
                case Command.Info =>
                    ZIO.logInfo("Migration information:") *>
                        ZIO.scoped {
                            for
                                ds <- ZIO.service[PostgreSQLDataSource]
                                migrator <- ZIO.service[FlywayMigrationService].provideSome[
                                    Scope & PostgreSQLDataSource
                                ](FlywayMigrationService.layerWithConfig(config))
                                _ <- migrator.info()
                            yield ()
                        }
                case Command.Validate =>
                    ZIO.logInfo("Validating migrations...") *>
                        ZIO.scoped {
                            for
                                ds <- ZIO.service[PostgreSQLDataSource]
                                // Use default config or append additional locations to default
                                migrator <- ZIO.service[FlywayMigrationService].provideSome[
                                    Scope & PostgreSQLDataSource
                                ](FlywayMigrationService.layerWithConfig(config))
                                _ <- migrator.validate()
                                _ <- ZIO.logInfo("Migrations are valid.")
                            yield ()
                        }
                case Command.Help =>
                    printHelp
        yield ()
        end for
    end program

    override def run: ZIO[Scope & ZIOAppArgs, Throwable, Unit] =
        program.provideSome[Scope & ZIOAppArgs](PostgreSQLDataSource.managedLayer)
end MigrationCLI
