package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.Instant
import java.time.temporal.ChronoUnit

/** Represents the results of a transaction import operation.
  *
  * @param transactionCount
  *   The number of transactions that were imported
  * @param errorMessage
  *   Optional error message if the import failed
  * @param startTime
  *   The time when the import operation started
  * @param endTime
  *   Optional time when the import operation completed
  *
  * Category: View Model Layer: UI/Presentation
  */
case class ImportResults(
    transactionCount: Int,
    errorMessage: Option[String] = None,
    startTime: Instant,
    endTime: Option[Instant] = None
):
    /** Calculate the time taken to complete the import operation in seconds.
      *
      * @return
      *   Option containing the completion time in seconds, or None if the operation hasn't
      *   completed
      */
    def completionTimeSeconds: Option[Long] =
        endTime.map(end => ChronoUnit.SECONDS.between(startTime, end))

    /** Determines if the import operation was successful.
      *
      * @return
      *   true if no error message is present, false otherwise
      */
    def isSuccess: Boolean = errorMessage.isEmpty
end ImportResults
