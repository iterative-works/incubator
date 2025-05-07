package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import works.iterative.incubator.budget.ui.transaction_import.components.*
import works.iterative.tapir.BaseUri
import scalatags.Text.all.*
import java.time.LocalDate
import scala.annotation.unused
import works.iterative.scalatags.components.ScalatagsAppShell

/** View class for rendering the transaction import UI. Composes UI from individual components with
  * proper layout.
  * 
  * Category: View
  * Layer: UI/Presentation
  */
class TransactionImportView(appShell: ScalatagsAppShell)(using @unused baseUri: BaseUri):
    /** Render the main import form with unified validation.
      *
      * @param viewModel
      *   The form view model containing all data and validation state
      * @return
      *   HTML content
      */
    def renderImportForm(viewModel: TransactionImportFormViewModel): Frag =
        appShell.wrap(
            pageTitle = "Transaction Import",
            content = div(
                cls := "container mx-auto px-4 py-8",
                // Header
                h1(
                    cls := "text-2xl font-bold text-gray-800 mb-3 bg-blue-100 p-4 rounded-md",
                    "Import Transactions from Fio Bank"
                ),
                // Help text
                div(
                    cls := "mt-6 text-sm text-gray-500",
                    p(
                        "Note: This will import all transactions from the selected account and period. " +
                            "Transactions will be categorized using predefined rules."
                    )
                ),
                // The main form component
                TransactionImportForm.render(viewModel),
                // Results panel if applicable
                viewModel.importResults.map { results =>
                    div(
                        id := "results-panel-container",
                        cls := "mt-6",
                        ResultsPanel.render(
                            ResultsPanelViewModel(
                                importResults = Some(results),
                                isVisible = true,
                                startDate = viewModel.startDate,
                                endDate = viewModel.endDate
                            )
                        )
                    )
                }
            )
        )
    end renderImportForm
    
    /** Legacy method for backward compatibility */
    def renderImportPage(viewModel: ImportPageViewModel): Frag =
        // Convert legacy view model to new form view model
        val formViewModel = TransactionImportFormViewModel(
            accounts = viewModel.accounts,
            selectedAccountId = viewModel.selectedAccountId,
            startDate = viewModel.startDate,
            endDate = viewModel.endDate,
            fieldErrors = Map.empty[String, String] ++ (viewModel.validationError.map(e => "dateRange" -> e)) ++ 
                             (viewModel.accountValidationError.map(e => "accountId" -> e)),
            globalError = None,
            isSubmitting = viewModel.isLoading,
            importStatus = viewModel.importStatus,
            importResults = viewModel.importResults
        )
        
        renderImportForm(formViewModel)
    end renderImportPage

    /** Render just the import status component for HTMX updates.
      *
      * @param status
      *   The current import status
      * @return
      *   HTML content
      */
    def renderImportStatus(status: ImportStatus): Frag =
        val viewModel = StatusIndicatorViewModel(status = status, isVisible = true)
        StatusIndicator.render(viewModel)
    end renderImportStatus

    /** Render just the results panel for HTMX updates.
      *
      * @param results
      *   The import results
      * @param startDate
      *   The start date used for the import
      * @param endDate
      *   The end date used for the import
      * @return
      *   HTML content
      */
    def renderImportResults(
        results: ImportResults,
        startDate: LocalDate,
        endDate: LocalDate
    ): Frag =
        val viewModel = ResultsPanelViewModel(
            importResults = Some(results),
            isVisible = true,
            startDate = startDate,
            endDate = endDate
        )
        ResultsPanel.render(viewModel)
    end renderImportResults

    /** Legacy method for backward compatibility */
    def renderDateValidationResult(
        errorMessage: Option[String],
        startDate: LocalDate,
        endDate: LocalDate
    ): Frag =
        val viewModel = DateRangeSelectorViewModel(
            startDate = startDate,
            endDate = endDate,
            validationError = errorMessage
        )
        DateRangeSelector.render(viewModel)
    end renderDateValidationResult
    
    /** Legacy method for backward compatibility */
    def renderAccountValidationResult(
        errorMessage: Option[String],
        accountId: Option[String],
        accounts: List[AccountOption]
    ): Frag =
        val viewModel = AccountSelectorViewModel(
            accounts = accounts,
            selectedAccountId = accountId,
            validationError = errorMessage
        )
        // Use renderControl instead of render to avoid including the header in HTMX updates
        AccountSelector.renderControl(viewModel)
    end renderAccountValidationResult
end TransactionImportView
