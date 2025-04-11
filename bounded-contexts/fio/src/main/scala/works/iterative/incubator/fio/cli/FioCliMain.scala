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
    PostgreSQLFioImportStateRepository,
    InMemoryFioAccountRepository,
    PostgreSQLFioAccountRepository
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
        case class ImportByDate(from: LocalDate, to: LocalDate, accountId: Option[Long]) extends Command
        case class ImportNew(accountId: Option[Long]) extends Command
        case object ListAccounts extends Command
        case class CreateAccount(sourceAccountId: Long, token: String) extends Command
        case object Help extends Command
    end Command

    // Command handler
    def executeCommand(command: Command): RIO[FioImportService & FioAccountRepository, Unit] =
        command match
            case Command.ImportByDate(from, to, accountId) =>
                for
                    _ <- Console.printLine(s"Importing transactions from $from to $to...")
                    service <- ZIO.service[FioImportService]
                    accountInfo = accountId.map(id => s" for account ID: $id").getOrElse("")
                    _ <- Console.printLine(s"Starting import$accountInfo...")
                    count <- accountId match
                        case Some(id) => service.importTransactionsForAccount(from, to, id)
                        case None => service.importFioTransactions(from, to)
                    _ <- Console.printLine(s"Imported $count transactions.")
                yield ()

            case Command.ImportNew(accountId) =>
                for
                    _ <- Console.printLine("Importing new transactions...")
                    service <- ZIO.service[FioImportService]
                    accountInfo = accountId.map(id => s" for account ID: $id").getOrElse("")
                    _ <- Console.printLine(s"Starting import$accountInfo...")
                    count <- accountId match
                        case Some(id) => service.importNewTransactionsForAccount(id)
                        case None => service.importNewTransactions()
                    _ <- Console.printLine(s"Imported $count transactions.")
                yield ()

            case Command.ListAccounts =>
                for
                    _ <- Console.printLine("Listing Fio source accounts...")
                    accountRepo <- ZIO.service[FioAccountRepository]
                    accounts <- accountRepo.getAll()
                    _ <- Console.printLine(s"Found ${accounts.size} Fio accounts:")
                    _ <- ZIO.foreach(accounts)(account => 
                        Console.printLine(
                            s"  - Account ID: ${account.id}, " +
                            s"Source Account ID: ${account.sourceAccountId}, " +
                            s"Last Sync: ${account.lastSyncTime.getOrElse("Never")}"
                        )
                    )
                yield ()
                
            case Command.CreateAccount(sourceAccountId, token) =>
                for
                    _ <- Console.printLine(s"Creating Fio account for source account ID: $sourceAccountId...")
                    accountRepo <- ZIO.service[FioAccountRepository]
                    id <- accountRepo.create(CreateFioAccount(sourceAccountId, token))
                    _ <- Console.printLine(s"Created Fio account with ID: $id")
                yield ()

            case Command.Help =>
                ZIO.succeed(printHelp())

    // Main application logic
    private def cliApp(args: Array[String]): ZIO[FioImportService & FioAccountRepository, Throwable, ExitCode] =
        for
            command <- parseCommand(args)
            _ <- executeCommand(command)
        yield ExitCode.success

    // Extract option value from args list
    private def getOptionValue(args: List[String], option: String): Option[String] =
        val index = args.indexOf(option)
        if index >= 0 && index < args.length - 1 then
            Some(args(index + 1))
        else
            // Check for --option=value format
            args.find(_.startsWith(s"$option=")).map(_.drop(option.length + 1))
    end getOptionValue

    // Parse command from command-line arguments
    def parseCommand(args: Array[String]): Task[Command] =
        val argsList = args.toList
        
        argsList match
            case Nil | "help" :: _ =>
                ZIO.succeed(Command.Help)

            case "import" :: rest =>
                val fromOpt = rest.find(_.startsWith("--from=")).map(_.drop(7))
                val toOpt = rest.find(_.startsWith("--to=")).map(_.drop(6))
                val accountId = getOptionValue(rest, "--account").flatMap(id => Try(id.toLong).toOption)
                
                if fromOpt.isEmpty || toOpt.isEmpty then
                    ZIO.fail(new IllegalArgumentException("Missing required parameters: --from=YYYY-MM-DD --to=YYYY-MM-DD"))
                else
                    for
                        from <- parseDate(fromOpt.get)
                        to <- parseDate(toOpt.get)
                    yield Command.ImportByDate(from, to, accountId)
                    end for

            case "import-new" :: rest =>
                val accountId = getOptionValue(rest, "--account").flatMap(id => Try(id.toLong).toOption)
                ZIO.succeed(Command.ImportNew(accountId))

            case "list-accounts" :: _ =>
                ZIO.succeed(Command.ListAccounts)
                
            case "create-account" :: rest =>
                val sourceAccountId = getOptionValue(rest, "--source-id")
                    .flatMap(id => Try(id.toLong).toOption)
                val token = getOptionValue(rest, "--token")
                
                (sourceAccountId, token) match
                    case (Some(id), Some(t)) => 
                        ZIO.succeed(Command.CreateAccount(id, t))
                    case _ =>
                        ZIO.fail(new IllegalArgumentException(
                            "Missing required parameters for create-account: --source-id=ID --token=TOKEN"
                        ))

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
    private def createRuntime: ZIO[Any, Nothing, ZLayer[Scope, Throwable, FioImportService & FioAccountRepository]] =
        // Use environment variables for configuration
        val fioToken = sys.env.get("FIO_TOKEN")
        val usePostgres = sys.env.getOrElse("USE_POSTGRES", "false").toBoolean
        val pgUrl = sys.env.getOrElse("PG_URL", "jdbc:postgresql://localhost:5432/incubator")
        val pgUsername = sys.env.getOrElse("PG_USERNAME", "incubator")
        val pgPassword = sys.env.getOrElse("PG_PASSWORD", "incubator")

        // Shared components
        val fioConfig = FioConfig(defaultToken = fioToken)

        // Runtime with in-memory repositories (for testing)
        val inMemoryRuntime =
            val repo =
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
                            lastSyncTime = None
                        )))

                    def save(key: Long, value: SourceAccount): UIO[Unit] =
                        ZIO.unit

                    // Required by CreateRepository[Long, CreateSourceAccount]
                    def create(value: CreateSourceAccount): UIO[Long] =
                        ZIO.succeed(1L)

            ZLayer.make[FioImportService & FioAccountRepository](
                FioClient.liveWithConfig(fioConfig),
                ZLayer.succeed(new InMemoryTransactionRepository(List.empty)),
                ZLayer.succeed(repo),
                InMemoryFioImportStateRepository.layer,
                InMemoryFioAccountRepository.layer,
                FioTransactionImportService.completeLayer
            )
        end inMemoryRuntime

        // PostgreSQL runtime for production use
        val postgresConfig = PostgreSQLConfig(
            jdbcUrl = pgUrl,
            username = pgUsername,
            password = pgPassword
        )

        val postgresRuntime = ZLayer.makeSome[Scope, FioImportService & FioAccountRepository](
            PostgreSQLDataSource.managedLayerWithConfig(postgresConfig),
            PostgreSQLTransactor.managedLayer,
            PostgreSQLSourceAccountRepository.layer,
            PostgreSQLFioImportStateRepository.layer,
            PostgreSQLFioAccountRepository.layer,
            PostgreSQLTransactionRepository.layer,
            FioClient.liveWithConfig(fioConfig),
            FioTransactionImportService.completeLayer
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
        |  fio import --from=YYYY-MM-DD --to=YYYY-MM-DD [--account=ID]  Import transactions for date range
        |  fio import-new [--account=ID]                                 Import new transactions since last import
        |  fio list-accounts                                              List available Fio accounts
        |  fio create-account --source-id=ID --token=TOKEN               Create a new Fio account
        |  fio help                                                       Show this help message
        |
        |Account Options:
        |  --account=ID                Specify Fio account ID for import commands
        |  --source-id=ID              Source account ID for create-account command
        |  --token=TOKEN               Fio API token for the account
        |
        |Environment Variables:
        |  FIO_TOKEN                   Default Fio API token (used when no account specified)
        |  USE_POSTGRES                Use PostgreSQL instead of in-memory storage (default: false)
        |  PG_URL                      PostgreSQL URL (default: jdbc:postgresql://localhost:5432/incubator)
        |  PG_USERNAME                 PostgreSQL username (default: incubator)
        |  PG_PASSWORD                 PostgreSQL password (default: incubator)
        """.stripMargin)

    // Helper to get first account ID for interactive selection
    def getFirstFioAccountId(repo: FioAccountRepository): Task[Option[Long]] =
        repo.getAll().map(accounts => accounts.headOption.map(_.id))

    // Entry point
    override def run =
        for
            args <- getArgs
            fioRuntime <- createRuntime
            exitCode <- cliApp(args.toArray).provideSome[Scope](fioRuntime)
        yield exitCode
end FioCliMain
