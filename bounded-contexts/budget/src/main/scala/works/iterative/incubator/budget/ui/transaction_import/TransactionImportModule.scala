package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.server.http.tapir.TapirEndpointModule
import works.iterative.tapir.BaseUri
import sttp.tapir.*
import java.time.LocalDate
import zio.*
import sttp.tapir.ztapir.*
import works.iterative.incubator.components.ScalatagsAppShell
import scalatags.Text.Frag
import scalatags.Text.all.raw

/** Tapir module for transaction import functionality.
  *
  * @param transactionImportView
  *   View for rendering HTML
  * 
  * Category: Web Module
  * Layer: UI/Presentation
  */
class TransactionImportModule(
    transactionImportView: TransactionImportView
) extends TapirEndpointModule[TransactionImportService]:

    val fragBody = htmlBodyUtf8.map[Frag](raw)(_.render)

    /** Base endpoint for transaction import with common path prefix and error handling */
    private val baseEndpoint = endpoint
        .in("transactions" / "import")
        .errorOut(stringBody)
        .out(fragBody)

    /** GET endpoint for the main import page */
    val importPageEndpoint = baseEndpoint
        .name("Transaction Import Page")
        .description("Display the transaction import form with date range selector")
        .get

    /** POST endpoint for validating date selections */
    val validateDatesEndpoint = baseEndpoint
        .name("Validate Date Range")
        .description("Validate a selected date range according to business rules")
        .post
        .in("validate-dates")
        .in(
            formBody[Map[String, String]] // Use the specific form body codec
        )

    /** POST endpoint for initiating transaction import */
    val importTransactionsEndpoint = baseEndpoint
        .name("Import Transactions")
        .description("Import transactions for the selected date range")
        .post
        .in(
            query[String]("startDate")
                .and(query[String]("endDate"))
        )

    /** GET endpoint for checking import status */
    val importStatusEndpoint = baseEndpoint
        .name("Import Status")
        .description("Check the current status of an ongoing import")
        .get
        .in("status")

    /** Implementation for the main import page */
    private def getImportPage: ZIO[TransactionImportService, String, Frag] =
        for
            viewModel <- TransactionImportService.getImportViewModel()
        yield transactionImportView.renderImportPage(viewModel)

    /** Implementation for date validation - handles form data */
    private def validateDates(
        formData: Map[String, String]
    ): ZIO[TransactionImportService, String, Frag] =
        val startDateStr = formData.getOrElse("startDate", "")
        val endDateStr = formData.getOrElse("endDate", "")

        for
            startDate <- ZIO.attempt(LocalDate.parse(startDateStr))
                .orElseFail("Invalid start date format")
            endDate <- ZIO.attempt(LocalDate.parse(endDateStr))
                .orElseFail("Invalid end date format")
            validationResult <- TransactionImportService.validateDateRange(startDate, endDate)
            errorMessage = validationResult.left.toOption
        yield transactionImportView.renderDateValidationResult(errorMessage, startDate, endDate)
        end for
    end validateDates

    /** Implementation for transaction import */
    private def importTransactions(
        startDateStr: String,
        endDateStr: String
    ): ZIO[TransactionImportService, String, Frag] =
        for
            startDate <- ZIO.attempt(LocalDate.parse(startDateStr))
                .orElseFail("Invalid start date format")
            endDate <- ZIO.attempt(LocalDate.parse(endDateStr))
                .orElseFail("Invalid end date format")
            _ <- TransactionImportService.validateDateRange(startDate, endDate).flatMap {
                case Left(error) => ZIO.fail(error)
                case Right(_)    => ZIO.unit
            }
            results <- TransactionImportService.importTransactions(startDate, endDate)
        yield transactionImportView.renderImportResults(results, startDate, endDate)

    /** Implementation for import status check */
    private def getImportStatus: ZIO[TransactionImportService, String, Frag] =
        for
            status <- TransactionImportService.getImportStatus()
        yield transactionImportView.renderImportStatus(status)

    // Server endpoint implementations
    val importPageServerEndpoint = importPageEndpoint.zServerLogic(_ => getImportPage)
    val validateDatesServerEndpoint =
        validateDatesEndpoint.zServerLogic { formData =>
            validateDates(formData)
        }
    val importTransactionsServerEndpoint =
        importTransactionsEndpoint.zServerLogic { case (startDate, endDate) =>
            importTransactions(startDate, endDate)
        }
    val importStatusServerEndpoint = importStatusEndpoint.zServerLogic(_ => getImportStatus)

    // List of all endpoints for documentation
    override def endpoints = List(
        importPageEndpoint,
        validateDatesEndpoint,
        importTransactionsEndpoint,
        importStatusEndpoint
    )

    // List of all server endpoints for routing
    override def serverEndpoints = List(
        importPageServerEndpoint,
        validateDatesServerEndpoint,
        importTransactionsServerEndpoint,
        importStatusServerEndpoint
    )
end TransactionImportModule

/** Companion object for creating TransactionImportModule */
object TransactionImportModule:
    /** Creates a TransactionImportModule with the given baseUri.
      *
      * @param baseUri
      *   The base URI for constructing URLs
      * @return
      *   A new TransactionImportModule instance
      */
    def apply(appShell: ScalatagsAppShell, baseUri: BaseUri): TransactionImportModule =
        given BaseUri = baseUri
        new TransactionImportModule(new TransactionImportView(appShell))
    end apply
end TransactionImportModule
