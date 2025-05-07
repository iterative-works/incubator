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

    /** GET endpoint for checking import status */
    val importStatusEndpoint = baseEndpoint
        .name("Import Status")
        .description("Check the current status of an ongoing import")
        .get
        .in("status")

    /** Implementation of the GET endpoint for the initial form */
    private def getImportForm: ZIO[TransactionImportPresenter, String, Frag] =
        for
            viewModel <- TransactionImportPresenter.getImportViewModel()
        yield transactionImportView.renderImportForm(viewModel)

    /** Implementation of the POST endpoint for form submission */
    private def submitImportForm(formData: Map[String, String], htxRequestHeader: Option[String])
        : ZIO[TransactionImportPresenter, String, Frag] =
        for
            // Parse form data
            command <- ZIO.succeed(TransactionImportCommand.fromFormData(formData))
            // Get base view model from form data (not submitting yet)
            baseViewModel = TransactionImportFormViewModel.fromFormData(formData)
            
            // Check if this is just a validation from HTMX field change or an actual form submission
            isHtmxFieldChange = htxRequestHeader.isDefined && formData.get("_triggeredBy").isDefined
            isActualSubmission = !isHtmxFieldChange
            
            // Only use submitting state for actual form submissions via the button
            initialViewModel = if isActualSubmission then baseViewModel.submitting else baseViewModel
            
            // Validate the command regardless
            result <- TransactionImportPresenter.validateAndProcess(command)
            
            // Create view model based on result
            viewModel = result match
                case Left(errors) => 
                    // If we have errors, don't show submitting state
                    baseViewModel.withValidationErrors(errors)
                case Right(importResults) => 
                    // Only process the import and show results if this is an actual submission
                    if isActualSubmission then
                        initialViewModel.withImportResults(importResults)
                    else
                        // For just validation, show the form is valid but don't process or show results
                        baseViewModel
                        
            // Check if this is an HTMX request
            isHtmxRequest = htxRequestHeader.isDefined
        yield transactionImportView.renderImportForm(viewModel, isHtmxRequest)

    /** Implementation for import status check */
    private def getImportStatus: ZIO[TransactionImportPresenter, String, Frag] =
        for
            status <- TransactionImportPresenter.getImportStatus()
        yield transactionImportView.renderImportStatus(status)

    // Server endpoints
    val importFormServerEndpoint = importFormEndpoint.zServerLogic(_ => getImportForm)
    val submitFormServerEndpoint = submitFormEndpoint.zServerLogic { case (formData, htxRequestHeader) => 
        submitImportForm(formData, htxRequestHeader) 
    }
    val importStatusServerEndpoint = importStatusEndpoint.zServerLogic(_ => getImportStatus)

    // List of all endpoints for documentation
    override def endpoints = List(
        importFormEndpoint,
        submitFormEndpoint,
        importStatusEndpoint
    )

    // List of all server endpoints for routing
    override def serverEndpoints = List(
        importFormServerEndpoint,
        submitFormServerEndpoint,
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
