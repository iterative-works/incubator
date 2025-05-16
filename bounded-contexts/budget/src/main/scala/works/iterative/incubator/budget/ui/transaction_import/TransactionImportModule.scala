package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.server.http.tapir.TapirEndpointModule
import works.iterative.tapir.BaseUri
import sttp.tapir.*
import zio.*
import sttp.tapir.ztapir.*
import works.iterative.incubator.components.ScalatagsAppShell
import scalatags.Text.Frag
import scalatags.Text.all.raw
import works.iterative.incubator.budget.ui.transaction_import.models.*

/** Tapir module for transaction import functionality.
  *
  * @param transactionImportView
  *   View for rendering HTML
  *
  * Category: Web Module Layer: UI/Presentation
  */
class TransactionImportModule(
    transactionImportView: TransactionImportView
) extends TapirEndpointModule[TransactionImportPresenter]:

    val fragBody = htmlBodyUtf8.map[Frag](raw)(_.render)

    /** Base endpoint for transaction import with common path prefix and error handling */
    private val baseEndpoint = endpoint
        .in("transactions" / "import")
        .errorOut(stringBody)
        .out(fragBody)

    /** GET endpoint for the initial form page */
    val importFormEndpoint = baseEndpoint
        .name("Transaction Import Form")
        .description("Display the transaction import form")
        .get

    /** POST endpoint for form submission and validation */
    val submitFormEndpoint = baseEndpoint
        .name("Submit Import Form")
        .description("Submit the import form for validation and processing")
        .post
        .in("submit")
        .in(formBody[Map[String, String]])
        .in(header[Option[String]]("HX-Request").description("HTMX request header"))

    /** Implementation of the GET endpoint for the initial form */
    private def getImportForm: ZIO[TransactionImportPresenter, String, Frag] =
        for
            viewModel <- TransactionImportPresenter.getImportViewModel()
        yield transactionImportView.renderImportForm(viewModel)

    /** Implementation of the POST endpoint for form submission - simplified synchronous approach */
    private def submitImportForm(
        formData: Map[String, String],
        htxRequestHeader: Option[String]
    ): ZIO[TransactionImportPresenter, String, Frag] =
        for
            // Parse form data
            command <- ZIO.succeed(TransactionImportCommand.fromFormData(formData))
            // Get view model from form data and mark as submitting
            baseViewModel <- TransactionImportPresenter.getImportViewModel().map: baseModel =>
                TransactionImportFormViewModel.applyFormData(baseModel, formData).submitting

            // Validate and process the command (synchronously)
            result <- TransactionImportPresenter.validateAndProcess(command)

            // Create view model based on result
            viewModel = result match
                case Left(errors) =>
                    // If we have errors, show them in the form and reset status to NotStarted
                    baseViewModel.copy(
                        isSubmitting = false,
                        importStatus = ImportStatus.NotStarted
                    ).withValidationErrors(errors)
                case Right(importResults) =>
                    // Show results directly in the form
                    baseViewModel.withImportResults(importResults)

            // Check if this is an HTMX request (for rendering)
            isHtmxRequest = htxRequestHeader.isDefined
        yield transactionImportView.renderImportForm(viewModel, isHtmxRequest)

    // Server endpoints
    val importFormServerEndpoint = importFormEndpoint.zServerLogic(_ => getImportForm)
    val submitFormServerEndpoint =
        submitFormEndpoint.zServerLogic { case (formData, htxRequestHeader) =>
            submitImportForm(formData, htxRequestHeader)
        }

    // List of all endpoints for documentation
    override def endpoints = List(
        importFormEndpoint,
        submitFormEndpoint
    )

    // List of all server endpoints for routing
    override def serverEndpoints = List(
        importFormServerEndpoint,
        submitFormServerEndpoint
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
