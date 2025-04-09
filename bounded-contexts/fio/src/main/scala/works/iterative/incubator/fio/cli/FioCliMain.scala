package works.iterative.incubator.fio.cli

import zio.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.application.service.FioImportService
import works.iterative.incubator.fio.infrastructure.client.FioClient
import works.iterative.incubator.fio.infrastructure.config.FioConfig
import works.iterative.incubator.fio.infrastructure.service.FioTransactionImportService
import works.iterative.incubator.fio.infrastructure.persistence.InMemoryFioImportStateRepository
import works.iterative.incubator.transactions.domain.repository.{
    TransactionRepository,
    SourceAccountRepository
}
import works.iterative.incubator.transactions.infrastructure.persistence.{
    InMemoryTransactionRepository
}
import works.iterative.incubator.transactions.domain.model.SourceAccount

/**
 * Command-line interface for testing Fio integration
 */
object FioCliMain extends ZIOAppDefault:
    // Command options
    sealed trait Command
    object Command:
        case class ImportByDate(from: LocalDate, to: LocalDate) extends Command
        case object ImportNew extends Command
        case object ListAccounts extends Command
        case object Help extends Command

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
                
            case "import" :: rest if rest.exists(_.startsWith("--from=")) && rest.exists(_.startsWith("--to=")) =>
                val fromArg = rest.find(_.startsWith("--from=")).get.drop(7)
                val toArg = rest.find(_.startsWith("--to=")).get.drop(6)
                
                for
                    from <- parseDate(fromArg)
                    to <- parseDate(toArg)
                yield Command.ImportByDate(from, to)
                
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
                .left.map(e => new IllegalArgumentException(s"Invalid date format: $dateStr. Use YYYY-MM-DD", e))
        )

    // Application layers
    private def createRuntime =
        // Use environment variables for configuration
        val fioToken = sys.env.getOrElse("FIO_TOKEN", "")
        val usePostgres = sys.env.getOrElse("USE_POSTGRES", "false").toBoolean

        // Shared components
        val fioConfig = FioConfig(token = fioToken)
        
        // Runtime with in-memory repositories (for testing)
        val inMemoryRuntime = 
            FioClient.liveWithConfig(fioConfig) >+>
            ZLayer.succeed(new InMemoryTransactionRepository(List.empty)) >+>
            ZLayer.succeed {
                new SourceAccountRepository {
                    def save(sourceAccount: works.iterative.incubator.transactions.domain.model.SourceAccount): Task[Long] = 
                        ZIO.succeed(sourceAccount.id)
                    def get(id: Long): Task[Option[works.iterative.incubator.transactions.domain.model.SourceAccount]] = 
                        ZIO.succeed(Some(works.iterative.incubator.transactions.domain.model.SourceAccount(
                            id = id,
                            name = "Test Account",
                            accountId = "2200000001",
                            bankId = "2010",
                            currency = "CZK",
                            active = true
                        )))
                    def find(query: works.iterative.incubator.transactions.domain.query.SourceAccountQuery): Task[Seq[works.iterative.incubator.transactions.domain.model.SourceAccount]] = 
                        ZIO.succeed(Seq(works.iterative.incubator.transactions.domain.model.SourceAccount(
                            id = 1L,
                            name = "Test Account",
                            accountId = "2200000001",
                            bankId = "2010",
                            currency = "CZK",
                            active = true
                        )))
                }
            } >+>
            InMemoryFioImportStateRepository.layer >+>
            FioTransactionImportService.layerWithImportState
            
        // Use in-memory runtime since we don't have PostgreSQL layer support yet
        inMemoryRuntime
    
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
        """.stripMargin)
    
    // Entry point
    override def run =
        for
            args <- getArgs
            exitCode <- cliApp(args.toArray).provideLayer(createRuntime)
        yield exitCode