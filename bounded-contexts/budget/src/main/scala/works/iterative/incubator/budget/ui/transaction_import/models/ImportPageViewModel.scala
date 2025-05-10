package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/** View model for the transaction import page.
  *
  * @param accounts
  *   List of available accounts to choose from
  * @param selectedAccountId
  *   Currently selected account ID
  * @param startDate
  *   The start date for the transaction import range, defaults to the first day of the current
  *   month
  * @param endDate
  *   The end date for the transaction import range, defaults to the current date
  * @param importStatus
  *   The current status of the import operation
  * @param importResults
  *   Optional results of the import operation
  * @param validationError
  *   Optional error message related to input validation
  * @param accountValidationError
  *   Optional error message related to account selection validation
  *
  * Category: View Model
  * Layer: UI/Presentation
  */
case class ImportPageViewModel(
    accounts: List[AccountOption] = AccountSelectorViewModel.defaultAccounts,
    selectedAccountId: Option[String] = None,
    startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    endDate: LocalDate = LocalDate.now(),
    importStatus: ImportStatus = ImportStatus.NotStarted,
    importResults: Option[ImportResults] = None,
    validationError: Option[String] = None,
    accountValidationError: Option[String] = None
):
    /** Determines if the import form is valid for submission.
      *
      * @return
      *   true if no validation errors exist and all required fields have valid values
      */
    def isValid: Boolean =
        validationError.isEmpty &&
            accountValidationError.isEmpty &&
            selectedAccountId.isDefined &&
            startDate != null &&
            endDate != null &&
            !startDate.isAfter(endDate) &&
            !startDate.isAfter(LocalDate.now())

    /** Determines if an import operation is currently in progress.
      *
      * @return
      *   true if the import status is InProgress
      */
    def isLoading: Boolean = importStatus == ImportStatus.InProgress

    /** Determines if import results should be displayed.
      *
      * @return
      *   true if the import has either completed successfully or failed with an error
      */
    def showResults: Boolean =
        importStatus == ImportStatus.Completed ||
            importStatus == ImportStatus.Error
            
    /** Gets the account selector view model.
      *
      * @return
      *   An AccountSelectorViewModel populated with current values
      */
    def accountSelectorViewModel: AccountSelectorViewModel =
        AccountSelectorViewModel(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            validationError = accountValidationError
        )
end ImportPageViewModel
