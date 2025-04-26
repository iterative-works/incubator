package works.iterative.incubator.budget.domain.service

import zio.*
import works.iterative.incubator.budget.domain.model.*

/** Service interface for transaction categorization functionality
  *
  * Handles the workflow of categorizing transactions, either via automatic rules, AI suggestions,
  * or manual user overrides. Manages the categorization status and emits appropriate domain events.
  *
  * Classification: Domain Service Interface
  */
trait CategorizationService:
    /** Apply automated categorization to a batch of transactions
      *
      * This workflow:
      *   1. Processes uncategorized transactions using categorization logic 2. Updates transaction
      *      processing states with suggested categories and confidence scores 3. Emits events for
      *      categorized transactions
      *
      * @param transactionIds
      *   The IDs of transactions to categorize
      * @return
      *   A CategorizedResult with the number of categorized transactions
      */
    def categorizeTransactions(
        transactionIds: Seq[TransactionId]
    ): UIO[CategorizationResult]

    /** Apply categorization to a single transaction
      *
      * @param transactionId
      *   The ID of the transaction to categorize
      * @return
      *   The categorization results for the transaction
      */
    def categorizeTransaction(
        transactionId: TransactionId
    ): UIO[Option[TransactionCategorization]]

    /** Manually update category for a single transaction
      *
      * @param transactionId
      *   The ID of the transaction to update
      * @param categoryId
      *   The new category ID
      * @param memo
      *   Optional memo to update
      * @param payeeName
      *   Optional payee name to update
      * @return
      *   An updated TransactionProcessingState
      */
    def updateCategory(
        transactionId: TransactionId,
        categoryId: String,
        memo: Option[String] = None,
        payeeName: Option[String] = None
    ): UIO[Option[TransactionProcessingState]]

    /** Bulk update categories for transactions matching criteria
      *
      * @param filter
      *   Criteria for matching transactions to update
      * @param categoryId
      *   The new category ID
      * @param memo
      *   Optional memo to update
      * @param payeeName
      *   Optional payee name to update
      * @return
      *   The number of transactions updated
      */
    def bulkUpdateCategory(
        filter: TransactionFilter,
        categoryId: String,
        memo: Option[String] = None,
        payeeName: Option[String] = None
    ): UIO[Int]

    /** Calculate average confidence score for a batch of categorizations
      *
      * @param categorizations
      *   List of transaction categorizations
      * @return
      *   An average confidence score
      */
    def calculateAverageConfidence(
        categorizations: Seq[TransactionCategorization]
    ): Option[ConfidenceScore]
end CategorizationService

/** Results of a categorization operation */
case class CategorizationResult(
    categorizedCount: Int,
    failedCount: Int,
    averageConfidence: Option[ConfidenceScore]
)

/** Contains category assignment for a transaction */
case class TransactionCategorization(
    transactionId: TransactionId,
    categoryId: Option[String],
    payeeName: Option[String],
    memo: Option[String],
    confidence: Option[ConfidenceScore]
)

/** Filter criteria for bulk category updates */
case class TransactionFilter(
    sourceAccountId: Option[Long] = None,
    descriptionContains: Option[String] = None,
    minAmount: Option[BigDecimal] = None,
    maxAmount: Option[BigDecimal] = None,
    dateRange: Option[(java.time.LocalDate, java.time.LocalDate)] = None,
    counterPartyContains: Option[String] = None,
    transactionType: Option[String] = None
)

/** Companion object for CategorizationService */
object CategorizationService:
    /** Access the CategorizationService from the ZIO environment */
    def categorizeTransactions(
        transactionIds: Seq[TransactionId]
    ): URIO[CategorizationService, CategorizationResult] =
        ZIO.serviceWithZIO[CategorizationService](_.categorizeTransactions(transactionIds))

    /** Apply categorization to a single transaction */
    def categorizeTransaction(
        transactionId: TransactionId
    ): URIO[CategorizationService, Option[TransactionCategorization]] =
        ZIO.serviceWithZIO[CategorizationService](_.categorizeTransaction(transactionId))

    /** Manually update category for a single transaction */
    def updateCategory(
        transactionId: TransactionId,
        categoryId: String,
        memo: Option[String] = None,
        payeeName: Option[String] = None
    ): URIO[CategorizationService, Option[TransactionProcessingState]] =
        ZIO.serviceWithZIO[CategorizationService](
            _.updateCategory(transactionId, categoryId, memo, payeeName)
        )

    /** Bulk update categories for transactions matching criteria */
    def bulkUpdateCategory(
        filter: TransactionFilter,
        categoryId: String,
        memo: Option[String] = None,
        payeeName: Option[String] = None
    ): URIO[CategorizationService, Int] =
        ZIO.serviceWithZIO[CategorizationService](
            _.bulkUpdateCategory(filter, categoryId, memo, payeeName)
        )

    /** Calculate average confidence score for a batch of categorizations */
    def calculateAverageConfidence(
        categorizations: Seq[TransactionCategorization]
    ): URIO[CategorizationService, Option[ConfidenceScore]] =
        ZIO.serviceWith[CategorizationService](_.calculateAverageConfidence(categorizations))

    /** Create a layer for the CategorizationService implementation */
    def layer: URLayer[Any, CategorizationService] =
        ??? // To be implemented by concrete implementations
end CategorizationService
