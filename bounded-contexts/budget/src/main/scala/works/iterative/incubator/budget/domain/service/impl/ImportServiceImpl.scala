package works.iterative.incubator.budget.domain.service.impl

import zio.*
import java.time.Instant
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.event.*
import works.iterative.incubator.budget.domain.service.{ImportService, RawTransaction}

/** Implementation of the ImportService that contains all business logic for transaction import.
  *
  * This implementation follows the functional core pattern, containing all business logic for
  * transaction import while delegating infrastructure concerns to repository interfaces.
  *
  * @param transactionRepository Repository for storing and retrieving Transaction entities
  * @param processingStateRepository Repository for storing and retrieving transaction processing states
  * @param sourceAccountRepository Repository for retrieving source accounts
  * @param eventPublisher Function to publish domain events
  */
case class ImportServiceImpl(
    transactionRepository: TransactionRepository,
    processingStateRepository: TransactionProcessingStateRepository,
    sourceAccountRepository: SourceAccountRepository,
    eventPublisher: DomainEvent => UIO[Unit]
) extends ImportService:

    /** Import a batch of raw transaction data from a source account
      *
      * This method handles:
      * 1. Checking for duplicate transactions
      * 2. Creating Transaction entities for new transactions
      * 3. Creating initial TransactionProcessingState for each transaction
      * 4. Publishing events for imported transactions
      *
      * @param sourceAccountId The ID of the source account to import from
      * @param rawTransactions The raw transaction data to import
      * @return A tuple containing the count of imported transactions and a list of detected duplicate IDs
      */
    override def importTransactions(
        sourceAccountId: Long,
        rawTransactions: Seq[RawTransaction]
    ): UIO[(Int, Seq[String])] =
        for
            // Validate that the source account exists - use a query to get all accounts and filter in memory
            allAccounts <- sourceAccountRepository.find(null) // Use null as a dummy query to get all accounts
            sourceAccount <- ZIO.fromOption(allAccounts.find(_.id == sourceAccountId))
                .orElseFail(new IllegalArgumentException(s"Source account $sourceAccountId not found"))
                .orDie
                
            // Import transactions one by one
            results <- ZIO.foreach(rawTransactions) { raw =>
                importTransaction(raw, sourceAccountId)
            }
            
            // Calculate results
            importedTransactions = results.flatten
            importedCount = importedTransactions.size
            duplicateIds = rawTransactions.map(_.externalId).diff(importedTransactions.map(_.id.transactionId))
            
            // Publish summary event
            importCompletedEvent <- createImportCompletedEvent(sourceAccountId, importedCount)
            _ <- eventPublisher(importCompletedEvent)
        yield (importedCount, duplicateIds)

    /** Check if a transaction already exists in the system
      *
      * @param transactionId The ID to check for duplicates
      * @return True if the transaction already exists, false otherwise
      */
    override def checkForDuplicate(transactionId: TransactionId): UIO[Boolean] =
        transactionRepository.findById(transactionId).map(_.isDefined)

    /** Import a single transaction, handling duplicate detection
      *
      * @param rawTransaction The raw transaction data to import
      * @param sourceAccountId The ID of the source account
      * @return The created Transaction if not a duplicate, None if duplicate
      */
    override def importTransaction(
        rawTransaction: RawTransaction,
        sourceAccountId: Long
    ): UIO[Option[Transaction]] =
        for
            // Create the transaction ID
            transactionId <- ZIO.succeed(TransactionId(sourceAccountId, rawTransaction.externalId))
            
            // Check for duplicates
            isDuplicate <- checkForDuplicate(transactionId)
            
            result <- if isDuplicate then
                // If duplicate, publish event and return None
                for
                    existingTx <- transactionRepository.findById(transactionId).map(_.get)
                    event = DuplicateTransactionDetected(
                        externalId = rawTransaction.externalId,
                        sourceAccountId = sourceAccountId,
                        existingTransactionId = transactionId,
                        occurredAt = Instant.now()
                    )
                    _ <- eventPublisher(event)
                yield None
            else
                // If new transaction, create it
                for
                    now <- ZIO.succeed(Instant.now())
                    transaction <- ZIO.succeed(Transaction(
                        id = transactionId,
                        date = rawTransaction.date,
                        amount = rawTransaction.amount,
                        currency = rawTransaction.currency,
                        counterAccount = rawTransaction.counterAccount,
                        counterBankCode = rawTransaction.counterBankCode,
                        counterBankName = rawTransaction.counterBankName,
                        variableSymbol = rawTransaction.variableSymbol,
                        constantSymbol = rawTransaction.constantSymbol,
                        specificSymbol = rawTransaction.specificSymbol,
                        userIdentification = rawTransaction.userIdentification,
                        message = rawTransaction.message,
                        transactionType = rawTransaction.transactionType,
                        comment = rawTransaction.comment,
                        importedAt = now
                    ))
                    
                    // Save the transaction
                    _ <- transactionRepository.save(transaction.id, transaction)
                    
                    // Create and save the initial processing state
                    processingState = TransactionProcessingState.initial(transaction)
                    _ <- processingStateRepository.save(processingState.transactionId, processingState)
                    
                    // Publish event
                    event = TransactionImported(
                        transactionId = transactionId,
                        sourceAccountId = sourceAccountId,
                        date = transaction.date,
                        amount = transaction.amount,
                        currency = transaction.currency,
                        occurredAt = now
                    )
                    _ <- eventPublisher(event)
                yield Some(transaction)
        yield result

    /** Create a domain event signaling an import completion
      *
      * @param sourceAccountId The source account ID
      * @param count The number of transactions imported
      * @return An ImportCompleted event
      */
    override def createImportCompletedEvent(sourceAccountId: Long, count: Int): UIO[ImportCompleted] =
        ZIO.succeed(ImportCompleted(
            sourceAccountId = sourceAccountId,
            count = count,
            occurredAt = Instant.now()
        ))
end ImportServiceImpl

/** Companion object for ImportServiceImpl */
object ImportServiceImpl:
    /** Create a ZLayer for the ImportServiceImpl */
    def layer(
        eventPublisher: DomainEvent => UIO[Unit]
    ): URLayer[TransactionRepository & TransactionProcessingStateRepository & SourceAccountRepository, ImportService] =
        ZLayer {
            for
                transactionRepository <- ZIO.service[TransactionRepository]
                processingStateRepository <- ZIO.service[TransactionProcessingStateRepository]
                sourceAccountRepository <- ZIO.service[SourceAccountRepository]
            yield ImportServiceImpl(
                transactionRepository,
                processingStateRepository,
                sourceAccountRepository,
                eventPublisher
            )
        }
end ImportServiceImpl