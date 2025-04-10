package works.iterative.incubator.fio.cli

import zio.*
import java.time.LocalDate
import scala.util.Try
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.application.service.FioImportService
import works.iterative.incubator.fio.infrastructure.client.FioClient
import works.iterative.incubator.fio.infrastructure.config.FioConfig
import works.iterative.incubator.fio.infrastructure.service.FioTransactionImportService
import works.iterative.incubator.fio.infrastructure.persistence.{
    InMemoryFioImportStateRepository,
    PostgreSQLFioImportStateRepository
}
import works.iterative.incubator.transactions.domain.repository.{
    TransactionRepository,
    SourceAccountRepository
}
import works.iterative.incubator.transactions.infrastructure.persistence.{
    InMemoryTransactionRepository,
    PostgreSQLSourceAccountRepository,
    PostgreSQLTransactionRepository
}
import works.iterative.incubator.transactions.infrastructure.config.{
    PostgreSQLConfig,
    PostgreSQLDataSource,
    PostgreSQLTransactor
}
import works.iterative.incubator.transactions.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery
import works.iterative.incubator.fio.infrastructure.client.FioClientLive
import works.iterative.incubator.transactions.application.service.TransactionImportService

/** Command-line interface for testing Fio integration
  */
object FioCliMain extends ZIOAppDefault:
    // Command options
    sealed trait Command
    object Command:
        case class ImportByDate(from: LocalDate, to: LocalDate) extends Command
        case object ImportNew extends Command
        case object ListAccounts extends Command
        case object Help extends Command
    end Command

    // Command handler
    def executeCommand(command: Command): RIO[FioImportService, Unit] =
        command match
            case Command.ImportByDate(from, to) =>
                for
                    _ <- Console.printLine(s"Importing transactions from $from to $to...")
                    service <- ZIO.service[FioImportService]
                    count <- service.importTransactions(from, to)
                    _ <- Console.printLine(s"Imported $count transactions.")
                yield ()

            case Command.ImportNew =>
                for
                    _ <- Console.printLine("Importing new transactions...")
                    service <- ZIO.service[FioImportService]
                    count <- service.importNewTransactions()
                    _ <- Console.printLine(s"Imported $count transactions.")
                yield ()

            case Command.ListAccounts =>
                for
                    _ <- Console.printLine("Listing Fio source accounts...")
                    service <- ZIO.service[FioImportService]
                    accounts <- service.getFioSourceAccounts()
                    _ <- Console.printLine(s"Found ${accounts.size} Fio accounts:")
                    _ <- ZIO.foreach(accounts)(id => Console.printLine(s"  - Account ID: $id"))
                yield ()

            case Command.Help =>
                ZIO.succeed(printHelp())

    // Main application logic
    private def cliApp(args: Array[String]): ZIO[FioImportService, Throwable, ExitCode] =
        for
            command <- parseCommand(args)
            _ <- executeCommand(command)
        yield ExitCode.success

    // Parse command from command-line arguments
    def parseCommand(args: Array[String]): Task[Command] =
        args.toList match
            case Nil | "help" :: _ =>
                ZIO.succeed(Command.Help)

            case "import" :: rest
                if rest.exists(_.startsWith("--from=")) && rest.exists(_.startsWith("--to=")) =>
                val fromArg = rest.find(_.startsWith("--from=")).get.drop(7)
                val toArg = rest.find(_.startsWith("--to=")).get.drop(6)

                for
                    from <- parseDate(fromArg)
                    to <- parseDate(toArg)
                yield Command.ImportByDate(from, to)
                end for

            case "import-new" :: _ =>
                ZIO.succeed(Command.ImportNew)

            case "list-accounts" :: _ =>
                ZIO.succeed(Command.ListAccounts)

            case cmd :: _ =>
                ZIO.fail(new IllegalArgumentException(s"Unknown command: $cmd"))

    // Parse date from string
    private def parseDate(dateStr: String): Task[LocalDate] =
        ZIO.fromEither(
            Try(LocalDate.parse(dateStr)).toEither
                .left.map(e =>
                    new IllegalArgumentException(
                        s"Invalid date format: $dateStr. Use YYYY-MM-DD",
                        e
                    )
                )
        )

    // Application layers
    private def createRuntime: ZIO[Any, Nothing, ZLayer[Scope, Throwable, FioImportService]] =
        // Use environment variables for configuration
        val fioToken = sys.env.getOrElse("FIO_TOKEN", "")
        val usePostgres = sys.env.getOrElse("USE_POSTGRES", "false").toBoolean
        val pgUrl = sys.env.getOrElse("PG_URL", "jdbc:postgresql://localhost:5432/incubator")
        val pgUsername = sys.env.getOrElse("PG_USERNAME", "incubator")
        val pgPassword = sys.env.getOrElse("PG_PASSWORD", "incubator")

        // Shared components
        val fioConfig = FioConfig(token = fioToken)

        // Runtime with in-memory repositories (for testing)
        val inMemoryRuntime =
            FioClient.liveWithConfig(fioConfig) >+>
                ZLayer.succeed(new InMemoryTransactionRepository(List.empty)) >+>
                ZLayer.succeed {
                    new SourceAccountRepository:
                        // Required by Repository[Long, SourceAccount, SourceAccountQuery]
                        def find(filter: SourceAccountQuery): UIO[Seq[SourceAccount]] =
                            ZIO.succeed(Seq(SourceAccount(
                                id = 1L,
                                name = "Test Account",
                                accountId = "2200000001",
                                bankId = "2010",
                                currency = "CZK",
                                active = true,
                                ynabAccountId = None,
                                lastSyncTime = None
                            )))

                        def load(id: Long): UIO[Option[SourceAccount]] =
                            ZIO.succeed(Some(SourceAccount(
                                id = id,
                                name = "Test Account",
                                accountId = "2200000001",
                                bankId = "2010",
                                currency = "CZK",
                                active = true,
                                ynabAccountId = None,
                                lastSyncTime = None
                            )))

                        def save(key: Long, value: SourceAccount): UIO[Unit] =
                            ZIO.unit

                        // Required by CreateRepository[Long, CreateSourceAccount]
                        def create(value: CreateSourceAccount): UIO[Long] =
                            ZIO.succeed(1L)
                } >+>
                InMemoryFioImportStateRepository.layer >+>
                FioTransactionImportService.layerWithImportState

        // PostgreSQL runtime for production use
        val postgresConfig = PostgreSQLConfig(
            jdbcUrl = pgUrl,
            username = pgUsername,
            password = pgPassword
        )

        val postgresRuntime = ZLayer.makeSome[Scope, FioImportService](
            PostgreSQLDataSource.managedLayerWithConfig(postgresConfig),
            PostgreSQLTransactor.managedLayer,
            PostgreSQLSourceAccountRepository.layer,
            PostgreSQLFioImportStateRepository.layer,
            PostgreSQLTransactionRepository.layer,
            FioClient.liveWithConfig(fioConfig),
            FioTransactionImportService.layerWithImportState
        )

        // Choose runtime based on configuration
        if usePostgres then
            Console.printLine("Using PostgreSQL repositories").orDie *>
                ZIO.succeed(postgresRuntime)
        else
            Console.printLine("Using in-memory repositories").orDie *>
                ZIO.succeed(inMemoryRuntime)
        end if
    end createRuntime

    // Print help message
    private def printHelp(): Unit =
        println("""
        |Fio Bank API CLI Tool
        |
        |Usage:
        |  fio import --from=YYYY-MM-DD --to=YYYY-MM-DD  Import transactions for date range
        |  fio import-new                                 Import new transactions since last import
        |  fio list-accounts                              List available Fio accounts
        |  fio help                                       Show this help message
        |
        |Environment Variables:
        |  FIO_TOKEN                 Fio API token (required)
        |  USE_POSTGRES              Use PostgreSQL instead of in-memory storage (default: false)
        |  PG_URL                    PostgreSQL URL (default: jdbc:postgresql://localhost:5432/incubator)
        |  PG_USERNAME               PostgreSQL username (default: incubator)
        |  PG_PASSWORD               PostgreSQL password (default: incubator)
        """.stripMargin)

    // Entry point
    override def run =
        for
            args <- getArgs
            fioRuntime <- createRuntime
            exitCode <- cliApp(args.toArray).provideSome[Scope](fioRuntime)
        yield exitCode
end FioCliMain
