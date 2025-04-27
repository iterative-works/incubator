package works.iterative.incubator.budget.domain.port

import zio.*

import works.iterative.incubator.budget.domain.model.*

/** Port interface for categorizing financial transactions.
  *
  * This port abstracts the interaction with services that perform automatic categorization of
  * transactions, whether through AI/ML algorithms, rule-based systems, or other means. It focuses
  * on the domain's view of categorization, not the technical implementation.
  *
  * Implementations of this interface are responsible for:
  *   - Analyzing transaction data to determine appropriate categories
  *   - Providing confidence scores for categorization decisions
  *   - Supporting both individual and bulk categorization operations
  *   - Handling categorization failures gracefully
  */
trait CategorizationProvider:
    /** Categorizes a single transaction.
      *
      * @param transaction
      *   The transaction to categorize
      * @return
      *   A ZIO effect containing the categorization result or a provider error
      */
    def categorizeTransaction(
        transaction: Transaction
    ): ZIO[Any, CategorizationError, TransactionCategorization]

    /** Categorizes multiple transactions in bulk.
      *
      * @param transactions
      *   The list of transactions to categorize
      * @return
      *   A ZIO effect containing the categorization results or a provider error
      */
    def categorizeTransactions(
        transactions: List[Transaction]
    ): ZIO[Any, CategorizationError, List[TransactionCategorization]]

    /** Updates the categorization model with feedback about a categorization. This allows the
      * categorization system to learn from corrections.
      *
      * @param transaction
      *   The transaction that was categorized
      * @param assignedCategory
      *   The category that was assigned
      * @param correctCategory
      *   The correct category provided by user feedback
      * @return
      *   A ZIO effect indicating success or a provider error
      */
    def learnFromFeedback(
        transaction: Transaction,
        assignedCategory: Category,
        correctCategory: Category
    ): ZIO[Any, CategorizationError, Unit]
end CategorizationProvider

/** Result of a transaction categorization operation.
  *
  * @param transactionId
  *   The ID of the categorized transaction
  * @param category
  *   The assigned category
  * @param confidenceScore
  *   The confidence score for the categorization
  * @param alternativeCategories
  *   Optional alternative categories with their confidence scores
  */
case class TransactionCategorization(
    transactionId: TransactionId,
    category: Category,
    confidenceScore: ConfidenceScore,
    alternativeCategories: List[(Category, ConfidenceScore)] = List.empty
)

/** Represents errors that can occur during categorization operations.
  */
sealed trait CategorizationError

object CategorizationError:
    /** Categorization service is unavailable */
    final case class ServiceUnavailable(message: String) extends CategorizationError

    /** Transaction data is insufficient for categorization */
    final case class InsufficientData(transactionId: TransactionId) extends CategorizationError

    /** Error in the categorization algorithm */
    final case class AlgorithmError(message: String) extends CategorizationError

    /** Request exceeded rate or quota limits */
    case object RateLimitExceeded extends CategorizationError

    /** Provider returned an unexpected or unknown error */
    final case class UnexpectedError(cause: String) extends CategorizationError
end CategorizationError
