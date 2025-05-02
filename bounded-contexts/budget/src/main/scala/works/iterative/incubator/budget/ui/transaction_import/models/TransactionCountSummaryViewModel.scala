package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * View model for the transaction count summary component.
 *
 * @param transactionCount Number of imported transactions
 * @param startDate Start of import date range
 * @param endDate End of import date range
 * @param completionTimeSeconds Time taken to complete the import in seconds
 * @param isVisible Whether to display this summary
 */
case class TransactionCountSummaryViewModel(
    transactionCount: Int,
    startDate: LocalDate,
    endDate: LocalDate,
    completionTimeSeconds: Option[Long] = None,
    isVisible: Boolean = true
):
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    
    /**
     * Gets a formatted string representation of the start date.
     *
     * @return A human-readable date string
     */
    def formattedStartDate: String = startDate.format(dateFormatter)
    
    /**
     * Gets a formatted string representation of the end date.
     *
     * @return A human-readable date string
     */
    def formattedEndDate: String = endDate.format(dateFormatter)
    
    /**
     * Gets a formatted string representation of the completion time.
     *
     * @return A string with the completion time in seconds, or empty if not available
     */
    def formattedCompletionTime: String = 
        completionTimeSeconds.map(time => 
            if time < 1 then "less than 1 second" 
            else if time == 1 then "1 second" 
            else s"$time seconds"
        ).getOrElse("")
        
    /**
     * Gets a formatted string representation of the transaction count.
     *
     * @return A string with the transaction count
     */
    def formattedTransactionCount: String = 
        if transactionCount == 0 then "No transactions"
        else if transactionCount == 1 then "1 transaction"
        else s"$transactionCount transactions"