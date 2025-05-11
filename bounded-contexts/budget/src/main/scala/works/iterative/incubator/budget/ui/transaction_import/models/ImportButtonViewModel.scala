package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/** View model for the import button component.
  *
  * @param isEnabled
  *   Whether the button is clickable
  * @param isLoading
  *   Whether import is in progress
  * @param accountId
  *   The selected account ID for import
  * @param startDate
  *   The selected start date for import
  * @param endDate
  *   The selected end date for import
  *
  * Category: View Model Layer: UI/Presentation
  */
case class ImportButtonViewModel(
    isEnabled: Boolean,
    isLoading: Boolean,
    accountId: Option[String],
    startDate: LocalDate,
    endDate: LocalDate
):
    /** Gets the button text based on loading state.
      *
      * @return
      *   "Importing..." when loading, "Import Transactions" otherwise
      */
    def buttonText: String = if isLoading then "Importing..." else "Import Transactions"

    /** Determines if the button should be disabled.
      *
      * @return
      *   true if the button is in loading state (always enabled regardless of validation)
      */
    def isDisabled: Boolean = isLoading
end ImportButtonViewModel
