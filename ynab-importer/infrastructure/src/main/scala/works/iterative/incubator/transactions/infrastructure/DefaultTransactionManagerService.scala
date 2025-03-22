package works.iterative.incubator.transactions
package infrastructure

import zio.*
import service.*
import java.time.LocalDate

/** Default implementation of TransactionManagerService
  *
  * This implementation orchestrates the import and processing workflow.
  */
class DefaultTransactionManagerService(
    importService: TransactionImportService,
    processor: TransactionProcessor
) extends TransactionManagerService:

    import TransactionManagerService.ImportSummary
    
    override def importAndProcessTransactions(
        from: LocalDate,
        to: LocalDate
    ): Task[ImportSummary] =
        for
            // Step 1: Import transactions
            importedCount <- importService.importTransactions(from, to)
            
            // Step 2: Initialize processing state for imported transactions
            initializedCount <- processor.initializeNewTransactions()
            
            // Step 3: Process the imported transactions
            processedCount <- processor.processImportedTransactions()
        yield ImportSummary(importedCount, initializedCount, processedCount)
    
    override def importAndProcessNewTransactions(
        lastId: Option[Long] = None
    ): Task[ImportSummary] =
        for
            // Step 1: Import new transactions
            importedCount <- importService.importNewTransactions(lastId)
            
            // Step 2: Initialize processing state for imported transactions
            initializedCount <- processor.initializeNewTransactions()
            
            // Step 3: Process the imported transactions
            processedCount <- processor.processImportedTransactions()
        yield ImportSummary(importedCount, initializedCount, processedCount)
end DefaultTransactionManagerService

object DefaultTransactionManagerService:
    val layer: ZLayer[TransactionImportService & TransactionProcessor, Nothing, TransactionManagerService] =
        ZLayer {
            for
                importService <- ZIO.service[TransactionImportService]
                processor <- ZIO.service[TransactionProcessor]
            yield DefaultTransactionManagerService(importService, processor)
        }
end DefaultTransactionManagerService