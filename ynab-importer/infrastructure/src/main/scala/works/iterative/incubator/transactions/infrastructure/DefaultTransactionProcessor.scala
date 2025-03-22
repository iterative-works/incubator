package works.iterative.incubator.transactions
package infrastructure

import zio.*
import service.*
import java.time.Instant

/** Default implementation of TransactionProcessor
  *
  * This implementation handles the transition of transactions through their processing lifecycle.
  */
class DefaultTransactionProcessor(
    transactionRepository: TransactionRepository,
    processingStateRepository: TransactionProcessingStateRepository
) extends TransactionProcessor:
    
    /** Initialize processing state for newly imported transactions
      *
      * This method finds transactions that don't have a corresponding processing state
      * and creates an initial processing state for each of them.
      */
    override def initializeNewTransactions(): Task[Int] =
        for
            // Fetch all transactions - in a real system with large volumes, we would need pagination
            transactions <- transactionRepository.find(TransactionQuery())
            count <- ZIO.foldLeft(transactions)(0) { (count, transaction) =>
                // For each transaction, check if it already has a processing state
                for
                    processingState <- processingStateRepository.load(transaction.id)
                    updatedCount <- processingState match
                        case Some(_) => 
                            // Already has a processing state, skip
                            ZIO.succeed(count)
                        case None =>
                            // Create and save initial processing state
                            val initialState = TransactionProcessingState.initial(transaction)
                            processingStateRepository.save(transaction.id, initialState)
                                .as(count + 1)
                yield updatedCount
            }
        yield count
    end initializeNewTransactions
    
    /** Process transactions that are in the imported state
      *
      * In a full implementation, this would apply AI categorization or other business
      * logic to move transactions from imported to categorized state.
      * For now, it's a simplified placeholder.
      */
    override def processImportedTransactions(): Task[Int] =
        for
            // Find all transactions in the imported state
            importedStates <- processingStateRepository.findByStatus(TransactionStatus.Imported)
            
            // Process each imported transaction (in parallel for better performance)
            count <- ZIO.foldLeft(importedStates)(0) { (count, state) =>
                for
                    // Load the transaction
                    transactionOpt <- transactionRepository.load(state.transactionId)
                    
                    updatedCount <- transactionOpt match
                        case None =>
                            // Transaction not found, skip
                            ZIO.succeed(count)
                        case Some(transaction) =>
                            // In a real system, this would call AI services, apply categorization rules, etc.
                            // For now, we'll just do some simple placeholder logic
                            
                            // Simple categorization based on transaction data
                            val suggestedPayee = transaction.counterBankName.orElse(transaction.message)
                            val suggestedCategory = Some("Uncategorized")
                            val suggestedMemo = transaction.comment
                            
                            // Create updated state with "AI" categorization
                            val updatedState = state.withAICategorization(
                                suggestedPayee,
                                suggestedCategory,
                                suggestedMemo
                            )
                            
                            // Save the updated state
                            processingStateRepository.save(state.transactionId, updatedState)
                                .as(count + 1)
                yield updatedCount
            }
        yield count
    end processImportedTransactions

    /** Find transactions ready for submission to YNAB
      */
    override def findTransactionsReadyForSubmission(): Task[Seq[(Transaction, TransactionProcessingState)]] =
        for
            // Find all processing states ready for submission
            readyStates <- processingStateRepository.findReadyToSubmit()
            
            // Load the corresponding transactions
            pairs <- ZIO.foldLeft(readyStates)(Seq.empty[(Transaction, TransactionProcessingState)]) { 
                (acc, state) =>
                    for
                        transactionOpt <- transactionRepository.load(state.transactionId)
                        
                        updatedAcc <- transactionOpt match
                            case None => ZIO.succeed(acc)
                            case Some(transaction) => ZIO.succeed(acc :+ (transaction, state))
                    yield updatedAcc
            }
        yield pairs
    end findTransactionsReadyForSubmission

    /** Mark transactions as submitted to YNAB
      */
    override def markTransactionsAsSubmitted(
        transactionIds: Seq[TransactionId],
        ynabIds: Seq[String],
        ynabAccountId: String
    ): Task[Int] =
        if transactionIds.length != ynabIds.length then
            ZIO.fail(new IllegalArgumentException(
                s"Transaction IDs and YNAB IDs must have the same length: ${transactionIds.length} != ${ynabIds.length}"
            ))
        else
            ZIO.foldLeft(transactionIds.zip(ynabIds))(0) { (count, pair) =>
                val (transactionId, ynabId) = pair
                for
                    // Load current processing state
                    stateOpt <- processingStateRepository.load(transactionId)
                    
                    updatedCount <- stateOpt match
                        case None => ZIO.succeed(count)
                        case Some(state) =>
                            // Update the state with YNAB submission info
                            val updatedState = state.withYnabSubmission(ynabId, ynabAccountId)
                            processingStateRepository.save(transactionId, updatedState)
                                .as(count + 1)
                yield updatedCount
            }
    end markTransactionsAsSubmitted
end DefaultTransactionProcessor

object DefaultTransactionProcessor:
    val layer: ZLayer[TransactionRepository & TransactionProcessingStateRepository, Nothing, TransactionProcessor] =
        ZLayer {
            for
                txRepo <- ZIO.service[TransactionRepository]
                stateRepo <- ZIO.service[TransactionProcessingStateRepository]
            yield DefaultTransactionProcessor(txRepo, stateRepo)
        }
end DefaultTransactionProcessor