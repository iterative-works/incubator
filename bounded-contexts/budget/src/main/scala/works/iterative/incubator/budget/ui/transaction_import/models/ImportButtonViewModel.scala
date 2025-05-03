package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/** View model for the import button component.
  *
  * @param isEnabled
  *   Whether the button is clickable
  * @param isLoading
  *   Whether import is in progress
  * @param startDate
  *   The selected start date for import
  * @param endDate
  *   The selected end date for import
  */
case class ImportButtonViewModel(
    isEnabled: Boolean,
    isLoading: Boolean,
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
      *   true if the button is either not enabled or is in loading state
      */
    def isDisabled: Boolean = !isEnabled || isLoading
end ImportButtonViewModel
