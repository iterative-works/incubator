package works.iterative.incubator.budget.domain.service

import zio.*
import java.time.Instant
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.event.{TransactionImported, ImportCompleted, DuplicateTransactionDetected}

/** Service interface for the transaction import workflow
  *
  * The ImportService handles the workflow of importing transactions from various sources,
  * detecting duplicates, creating domain entities, and publishing appropriate events.
  *
  * Classification: Domain Service Interface
  */
trait ImportService:

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
    def importTransactions(
        sourceAccountId: Long, 
        rawTransactions: Seq[RawTransaction]
    ): UIO[(Int, Seq[String])] // Returns (importedCount, duplicateIds)

    /** Check if a transaction already exists in the system
      *
      * @param transactionId The ID to check for duplicates
      * @return True if the transaction already exists, false otherwise
      */
    def checkForDuplicate(transactionId: TransactionId): UIO[Boolean]

    /** Import a single transaction, handling duplicate detection
      *
      * @param rawTransaction The raw transaction data to import
      * @param sourceAccountId The ID of the source account
      * @return The created Transaction if not a duplicate, None if duplicate
      */
    def importTransaction(
        rawTransaction: RawTransaction, 
        sourceAccountId: Long
    ): UIO[Option[Transaction]]

    /** Create a domain event signaling an import completion
      *
      * @param sourceAccountId The source account ID
      * @param count The number of transactions imported
      * @return An ImportCompleted event
      */
    def createImportCompletedEvent(sourceAccountId: Long, count: Int): UIO[ImportCompleted]
end ImportService

/** Data transfer object for raw transaction data from external sources
  *
  * This DTO contains the minimal information needed to create a Transaction entity.
  * It represents the raw data from external systems before domain validation.
  */
case class RawTransaction(
    // Core identification 
    externalId: String, // External system's transaction ID
    
    // Transaction details
    date: java.time.LocalDate,
    amount: BigDecimal,
    currency: String,
    
    // Counterparty information
    counterAccount: Option[String],
    counterBankCode: Option[String],
    counterBankName: Option[String],
    
    // Additional transaction details
    variableSymbol: Option[String],
    constantSymbol: Option[String],
    specificSymbol: Option[String],
    userIdentification: Option[String],
    message: Option[String],
    transactionType: String,
    comment: Option[String]
)

/** Companion object for ImportService */
object ImportService:
    /** Access the ImportService from the ZIO environment */
    def importTransactions(
        sourceAccountId: Long, 
        rawTransactions: Seq[RawTransaction]
    ): URIO[ImportService, (Int, Seq[String])] =
        ZIO.serviceWithZIO[ImportService](_.importTransactions(sourceAccountId, rawTransactions))

    /** Check if a transaction already exists in the system */
    def checkForDuplicate(transactionId: TransactionId): URIO[ImportService, Boolean] =
        ZIO.serviceWithZIO[ImportService](_.checkForDuplicate(transactionId))

    /** Import a single transaction, handling duplicate detection */
    def importTransaction(
        rawTransaction: RawTransaction, 
        sourceAccountId: Long
    ): URIO[ImportService, Option[Transaction]] =
        ZIO.serviceWithZIO[ImportService](_.importTransaction(rawTransaction, sourceAccountId))

    /** Create a domain event signaling an import completion */
    def createImportCompletedEvent(
        sourceAccountId: Long, 
        count: Int
    ): URIO[ImportService, ImportCompleted] =
        ZIO.serviceWithZIO[ImportService](_.createImportCompletedEvent(sourceAccountId, count))
        
    /** Create a layer for the ImportService implementation */
    def layer: URLayer[Any, ImportService] =
        ??? // To be implemented by concrete implementations
end ImportService