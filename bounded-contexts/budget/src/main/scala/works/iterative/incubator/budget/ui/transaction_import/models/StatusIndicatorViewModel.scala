package works.iterative.incubator.budget.ui.transaction_import.models

/** View model for the status indicator component.
  *
  * @param status
  *   Current status of import operation
  * @param isVisible
  *   Whether the indicator should be displayed
  */
case class StatusIndicatorViewModel(
    status: ImportStatus,
    isVisible: Boolean = true
):
    /** Determines if the loading spinner should be shown.
      *
      * @return
      *   true if the status is InProgress
      */
    def showLoadingSpinner: Boolean = status == ImportStatus.InProgress

    /** Determines if the success checkmark should be shown.
      *
      * @return
      *   true if the status is Completed
      */
    def showSuccessIcon: Boolean = status == ImportStatus.Completed

    /** Determines if the error icon should be shown.
      *
      * @return
      *   true if the status is Error
      */
    def showErrorIcon: Boolean = status == ImportStatus.Error

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
end StatusIndicatorViewModel
