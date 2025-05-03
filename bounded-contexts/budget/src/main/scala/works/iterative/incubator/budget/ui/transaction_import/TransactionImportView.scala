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
  */
class TransactionImportView(appShell: ScalatagsAppShell)(using @unused baseUri: BaseUri):
    /** Render the main import page.
      *
      * @param viewModel
      *   The view model containing all data needed for the page
      * @return
      *   HTML content
      */
    def renderImportPage(viewModel: ImportPageViewModel): Frag =
        // Create component view models from the page view model
        val dateRangeSelectorViewModel = DateRangeSelectorViewModel(
            startDate = viewModel.startDate,
            endDate = viewModel.endDate,
            validationError = viewModel.validationError
        )

        val importButtonViewModel = ImportButtonViewModel(
            isEnabled = viewModel.isValid,
            isLoading = viewModel.isLoading,
            startDate = viewModel.startDate,
            endDate = viewModel.endDate
        )

        val statusIndicatorViewModel = StatusIndicatorViewModel(
            status = viewModel.importStatus,
            isVisible = viewModel.importStatus != ImportStatus.NotStarted
        )

        val resultsPanelViewModel = ResultsPanelViewModel(
            importResults = viewModel.importResults,
            isVisible = viewModel.showResults,
            startDate = viewModel.startDate,
            endDate = viewModel.endDate
        )

        appShell.wrap(
            pageTitle = "Transaction Import",
            content = div(
                cls := "container mx-auto px-4 py-8",
                h1(
                    cls := "text-2xl font-bold text-gray-800 mb-6",
                    "Import Transactions from Fio Bank"
                ),
                div(
                    cls := "bg-white rounded-lg shadow-md p-6",
                    // Description
                    p(
                        cls := "text-gray-600 mb-6",
                        "Select a date range to import transactions from your Fio Bank account. " +
                            "The date range cannot exceed 90 days due to Fio Bank API limitations."
                    ),
                    // Date range selector
                    div(cls := "mb-6", DateRangeSelector.render(dateRangeSelectorViewModel)),
                    // Import action panel with button and status
                    div(
                        cls := "flex flex-col space-y-4",
                        // Import button
                        div(
                            id := "import-button-container",
                            cls := "flex justify-center",
                            ImportButton.render(importButtonViewModel)
                        ),
                        // Status indicator
                        div(
                            id := "status-indicator-container",
                            cls := "mt-4",
                            StatusIndicator.render(statusIndicatorViewModel)
                        )
                    ),
                    // Results panel (only visible after import)
                    div(
                        id := "results-panel-container",
                        cls := "mt-6",
                        ResultsPanel.render(resultsPanelViewModel)
                    )
                ),
                // Help text
                div(
                    cls := "mt-6 text-sm text-gray-500",
                    p(
                        "Note: This will import all transactions from the selected period. " +
                            "Transactions will be categorized using predefined rules."
                    )
                )
            )
        )
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

    /** Render the validation error message for date range.
      *
      * @param errorMessage
      *   The validation error message
      * @param startDate
      *   The start date that was validated
      * @param endDate
      *   The end date that was validated
      * @return
      *   HTML content
      */
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
end TransactionImportView
