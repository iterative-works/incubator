package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/**
 * View model for the results panel component.
 *
 * @param importResults Optional results of the import operation
 * @param isVisible Whether the panel should be displayed
 * @param startDate The start date used for the import
 * @param endDate The end date used for the import
 */
case class ResultsPanelViewModel(
    importResults: Option[ImportResults],
    isVisible: Boolean,
    startDate: LocalDate,
    endDate: LocalDate
):
    /**
     * Determines if the success summary should be shown.
     *
     * @return true if results exist and are successful
     */
    def showSuccessSummary: Boolean = 
        importResults.exists(_.isSuccess)
    
    /**
     * Determines if the error message should be shown.
     *
     * @return true if results exist and contain an error
     */
    def showErrorMessage: Boolean = 
        importResults.exists(!_.isSuccess)
    
    /**
     * Determines if the retry button should be shown.
     *
     * @return true if results exist and contain an error
     */
    def showRetryButton: Boolean = 
        importResults.exists(!_.isSuccess)
    
    /**
     * Gets the number of transactions imported.
     *
     * @return the transaction count from results, or 0 if no results
     */
    def transactionCount: Int = 
        importResults.map(_.transactionCount).getOrElse(0)
        
    /**
     * Gets the error message if present.
     *
     * @return the error message from results, or None if no error
     */
    def errorMessage: Option[String] = 
        importResults.flatMap(_.errorMessage)
        
    /**
     * Gets the completion time in seconds if available.
     *
     * @return the completion time in seconds, or None if not completed
     */
    def completionTimeSeconds: Option[Long] = 
        importResults.flatMap(_.completionTimeSeconds)
        
    /**
     * Generates a unique error code for support.
     *
     * @return a string identifier for the error
     */
    def errorCode: String = 
        val timestamp = System.currentTimeMillis().toString.takeRight(8)
        s"IMPORT-${timestamp}"