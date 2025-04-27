package works.iterative.incubator.budget.domain.port

import zio.*

import works.iterative.incubator.budget.domain.model.*

/** Port interface for submitting transactions to external budget systems.
  *
  * This port abstracts the interaction with external budgeting systems (currently YNAB) for
  * submitting transactions. It focuses on the domain's view of submission, not the technical
  * implementation details of specific APIs.
  *
  * Implementations of this interface are responsible for:
  *   - Establishing connections to budget systems
  *   - Converting domain transactions to the format required by the external system
  *   - Handling submission, including batching when appropriate
  *   - Managing API rate limits and retries
  *   - Providing external system IDs for submitted transactions
  */
trait TransactionSubmissionPort:
    /** Validates if the transaction is ready for submission.
      *
      * @param transaction
      *   The transaction to validate
      * @param processingState
      *   The current processing state of the transaction
      * @return
      *   A ZIO effect containing validation results
      */
    def validateForSubmission(
        transaction: Transaction,
        processingState: TransactionProcessingState
    ): ZIO[Any, Nothing, ValidationResult]

    /** Submits a single transaction to the external budget system.
      *
      * @param transaction
      *   The transaction to submit
      * @param processingState
      *   The current processing state of the transaction
      * @return
      *   A ZIO effect containing the submission result or a submission error
      */
    def submitTransaction(
        transaction: Transaction,
        processingState: TransactionProcessingState
    ): ZIO[Any, SubmissionError, TransactionSubmissionResult]

    /** Submits multiple transactions to the external budget system.
      *
      * @param transactions
      *   The transactions to submit with their processing states
      * @return
      *   A ZIO effect containing the submission results or a submission error
      */
    def submitTransactions(
        transactions: List[(Transaction, TransactionProcessingState)]
    ): ZIO[Any, SubmissionError, List[TransactionSubmissionResult]]

    /** Tests if a connection to the external budget system can be established.
      *
      * @return
      *   A ZIO effect indicating success or a submission error
      */
    def testConnection(): ZIO[Any, SubmissionError, Unit]
end TransactionSubmissionPort

/** Result of a validation check before submission.
  */
sealed trait ValidationResult

object ValidationResult:
    /** Transaction passes validation and is ready for submission */
    case object Valid extends ValidationResult

    /** Transaction is missing required data for submission */
    final case class Invalid(reasons: List[String]) extends ValidationResult
end ValidationResult

/** Result of a transaction submission operation.
  *
  * @param transactionId
  *   The internal transaction ID
  * @param externalId
  *   The ID assigned by the external system
  * @param status
  *   The status of the submission
  */
case class TransactionSubmissionResult(
    transactionId: TransactionId,
    externalId: Option[String],
    status: TransactionSubmissionStatus
)

/** Status of a transaction submission operation.
  */
enum TransactionSubmissionStatus:
    /** Transaction was successfully submitted */
    case Submitted

    /** Transaction was already submitted previously */
    case AlreadySubmitted

    /** Transaction submission failed */
    case Failed(reason: String)
end TransactionSubmissionStatus

/** Represents errors that can occur during submission operations.
  */
sealed trait SubmissionError

object SubmissionError:
    /** Authentication with the external system failed */
    final case class AuthenticationFailed(message: String) extends SubmissionError

    /** Connection to the external system could not be established */
    final case class ConnectionFailed(message: String) extends SubmissionError

    /** API rate limit was exceeded */
    case object RateLimitExceeded extends SubmissionError

    /** Error occurred while submitting transactions */
    final case class SubmissionFailed(message: String) extends SubmissionError

    /** Validation failed for one or more transactions */
    final case class ValidationFailed(invalidTransactions: List[TransactionId])
        extends SubmissionError

    /** External system returned an unexpected or unknown error */
    final case class UnexpectedError(cause: String) extends SubmissionError
end SubmissionError
