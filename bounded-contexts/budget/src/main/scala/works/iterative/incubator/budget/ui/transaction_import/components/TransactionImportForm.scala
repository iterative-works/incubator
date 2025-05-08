package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.*
import works.iterative.incubator.budget.ui.transaction_import.styles.HtmxStyles
import scalatags.Text.all.*

/** Component for the transaction import form.
  * Provides a unified form that handles all validation centrally.
  *
  * Category: UI Component
  * Layer: UI/Presentation
  */
object TransactionImportForm:
    /** Renders a transaction import form component.
      *
      * @param viewModel
      *   The view model containing form data and validation state
      * @return
      *   A Scalatags fragment representing the import form
      */
    def render(viewModel: TransactionImportFormViewModel): Frag =
        frag(
            // Include HTMX-specific styles
            HtmxStyles.getStyles,
            
            form(
                id := "transaction-import-form",
                cls := "bg-white rounded-lg py-6 w-full form",
                attr("hx-post") := "/transactions/import/submit",
                attr("hx-target") := "#transaction-import-container",
                attr("hx-swap") := "outerHTML"
            )(
            // Global error message if any
            viewModel.globalError.map { error =>
                div(
                    cls := "bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4"
                )(
                    error
                )
            },
            
            // Account selector
            div(
                cls := "mb-4 w-full",
                AccountSelector.render(
                    AccountSelectorViewModel(
                        accounts = viewModel.accounts,
                        selectedAccountId = viewModel.selectedAccountId,
                        validationError = viewModel.fieldErrors.get("accountId")
                    )
                )
            ),
            
            // Date range selector
            div(
                cls := "mb-4 w-full",
                DateRangeSelector.render(
                    DateRangeSelectorViewModel(
                        startDate = viewModel.startDate,
                        endDate = viewModel.endDate,
                        validationError = 
                            if viewModel.fieldErrors.contains("startDate") then viewModel.fieldErrors.get("startDate")
                            else if viewModel.fieldErrors.contains("endDate") then viewModel.fieldErrors.get("endDate")
                            else if viewModel.fieldErrors.contains("dateRange") then viewModel.fieldErrors.get("dateRange")
                            else None
                    )
                )
            ),
            
            // Import button
            div(
                cls := "flex",
                ImportButton.render(
                    ImportButtonViewModel(
                        isEnabled = true, // Always enable the button regardless of validation
                        isLoading = viewModel.isSubmitting,
                        accountId = viewModel.selectedAccountId,
                        startDate = viewModel.startDate,
                        endDate = viewModel.endDate
                    )
                )
            ),
            
            // HTMX Loading indicator - will be shown during request
            div(
                cls := "htmx-indicator mt-4 flex items-center justify-center",
                style := "display: none;" // Initially hidden, shown by HTMX during request
            )(
                StatusIndicator.render(
                    StatusIndicatorViewModel(
                        status = ImportStatus.InProgress,
                        isVisible = true
                    )
                )
            ),
            
            // Persistent status indicator - shown for completed or error states
            (if viewModel.importStatus != ImportStatus.NotStarted && !viewModel.isSubmitting then
                div(
                    id := "status-indicator-container",
                    cls := "mt-4 flex items-center justify-center"
                )(
                    StatusIndicator.render(
                        StatusIndicatorViewModel(
                            status = viewModel.importStatus,
                            isVisible = true
                        )
                    )
                )
            else
                frag() // Empty if not needed
            )
            )
        )
    end render
end TransactionImportForm