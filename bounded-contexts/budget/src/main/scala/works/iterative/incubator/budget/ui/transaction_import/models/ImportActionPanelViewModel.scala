package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/** View model for the import action panel component that contains import button and status
  * indicator.
  *
  * @param isEnabled
  *   Whether the import action is enabled
  * @param status
  *   The current status of the import operation
  * @param startDate
  *   The selected start date for import
  * @param endDate
  *   The selected end date for import
  */
case class ImportActionPanelViewModel(
    isEnabled: Boolean,
    status: ImportStatus,
    startDate: LocalDate,
    endDate: LocalDate
):
    /** Determines if an import operation is currently in progress.
      *
      * @return
      *   true if the status is InProgress
      */
    def isLoading: Boolean = status == ImportStatus.InProgress

    /** Determines if the status indicator should be visible.
      *
      * @return
      *   true if the status is not NotStarted
      */
    def showStatusIndicator: Boolean = status != ImportStatus.NotStarted

    /** Gets a formatted string representation of the start and end dates.
      *
      * @return
      *   A string of the form "YYYY-MM-DD to YYYY-MM-DD"
      */
    def dateRangeString: String = s"${startDate} to ${endDate}"

    /** Gets a human-readable status text based on the current import status.
      *
      * @return
      *   Status text appropriate for the current status
      */
    def getStatusText: String = status match
        case ImportStatus.NotStarted => "Ready to import"
        case ImportStatus.InProgress => "Importing transactions..."
        case ImportStatus.Completed  => "Import completed successfully"
        case ImportStatus.Error      => "Import failed"
end ImportActionPanelViewModel
