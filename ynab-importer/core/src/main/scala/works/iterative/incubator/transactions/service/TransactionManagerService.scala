package works.iterative.incubator.transactions
package service

import zio.*
import java.time.LocalDate

/** High-level service that coordinates transaction import and processing
  *
  * This service combines the transaction import and processing lifecycle into a single orchestrated
  * workflow.
  */
trait TransactionManagerService:
    /** Import and process transactions from a date range
      *
      * This method:
      *   1. Imports transactions from the specified date range 2. Initializes processing state for
      *      the newly imported transactions 3. Processes the imported transactions
      *
      * @param from
      *   Start date for the import range
      * @param to
      *   End date for the import range
      * @return
      *   A summary of the import and processing operations
      */
    def importAndProcessTransactions(
        from: LocalDate,
        to: LocalDate
    ): Task[TransactionManagerService.ImportSummary]

    /** Import and process new transactions since the last import
      *
      * This method:
      *   1. Imports new transactions since the last import 2. Initializes processing state for the
      *      newly imported transactions 3. Processes the imported transactions
      *
      * @param lastId
      *   Optional ID of the last imported transaction
      * @return
      *   A summary of the import and processing operations
      */
    def importAndProcessNewTransactions(
        lastId: Option[Long] = None
    ): Task[TransactionManagerService.ImportSummary]
end TransactionManagerService

object TransactionManagerService:
    /** Summary of an import and processing operation */
    case class ImportSummary(
        importedCount: Int,
        initializedCount: Int,
        processedCount: Int
    ):
        /** Total number of transactions affected */
        def totalAffected: Int = importedCount

        /** Were any transactions imported? */
        def hasImported: Boolean = importedCount > 0

        /** Were all imported transactions initialized? */
        def allInitialized: Boolean = initializedCount >= importedCount

        /** A human-readable summary string */
        def summaryString: String =
            s"Imported: $importedCount, Initialized: $initializedCount, Processed: $processedCount"
    end ImportSummary
end TransactionManagerService
