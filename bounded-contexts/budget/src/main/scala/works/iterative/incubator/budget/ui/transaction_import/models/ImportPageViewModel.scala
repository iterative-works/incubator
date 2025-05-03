package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/** View model for the transaction import page.
  *
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
  */
case class ImportPageViewModel(
    startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    endDate: LocalDate = LocalDate.now(),
    importStatus: ImportStatus = ImportStatus.NotStarted,
    importResults: Option[ImportResults] = None,
    validationError: Option[String] = None
):
    /** Determines if the date range is valid for import.
      *
      * @return
      *   true if no validation error exists and the dates are valid
      */
    def isValid: Boolean =
        validationError.isEmpty &&
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
end ImportPageViewModel
