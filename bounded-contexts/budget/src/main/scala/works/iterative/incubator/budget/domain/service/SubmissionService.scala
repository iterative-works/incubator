package works.iterative.incubator.budget.domain.service

import zio.*
import works.iterative.incubator.budget.domain.model.*

/** Service interface for transaction submission functionality
  *
  * This service handles the workflow of submitting transactions to external budget systems
  * (currently YNAB), validating transactions before submission, and emitting appropriate domain
  * events.
  *
  * Classification: Domain Service Interface
  */
trait SubmissionService:
    /** Submit a batch of transactions to YNAB
      *
      * This workflow:
      *   1. Validates that transactions are ready for submission 2. Submits ready transactions to
      *      YNAB 3. Updates transaction processing states with YNAB IDs 4. Emits events for
      *      submitted transactions
      *
      * @param transactionIds
      *   The IDs of transactions to submit
      * @return
      *   A SubmissionResult with counts of successful and failed submissions
      */
    def submitTransactions(
        transactionIds: Seq[TransactionId]
    ): UIO[SubmissionResult]

    /** Submit a single transaction to YNAB
      *
      * @param transactionId
      *   The ID of the transaction to submit
      * @return
      *   The submission results for the transaction
      */
    def submitTransaction(
        transactionId: TransactionId
    ): UIO[TransactionSubmissionResult]

    /** Check if transactions meet requirements for submission
      *
      * @param transactionStates
      *   The processing states to validate
      * @return
      *   A ValidationResult with valid and invalid transactions
      */
    def validateForSubmission(
        transactionStates: Seq[TransactionProcessingState]
    ): UIO[ValidationResult]

    /** Get statistics about transaction submission status
      *
      * @param sourceAccountId
      *   Optional account ID to filter by
      * @return
      *   Statistics about transaction counts by status
      */
    def getSubmissionStatistics(
        sourceAccountId: Option[Long] = None
    ): UIO[SubmissionStatistics]
end SubmissionService

/** Results of a batch transaction submission operation */
case class SubmissionResult(
    submittedCount: Int,
    failedCount: Int,
    errors: Seq[SubmissionError]
)

/** Results of a single transaction submission */
case class TransactionSubmissionResult(
    transactionId: TransactionId,
    submitted: Boolean,
    ynabTransactionId: Option[String],
    error: Option[SubmissionError]
)

/** Validation results for transaction submission */
case class ValidationResult(
    validTransactions: Seq[TransactionProcessingState],
    invalidTransactions: Seq[(TransactionProcessingState, String)]
)

/** Error that occurred during transaction submission */
case class SubmissionError(
    transactionId: TransactionId,
    reason: String
)

/** Statistics about transaction submission status */
case class SubmissionStatistics(
    total: Int,
    imported: Int,
    categorized: Int,
    submitted: Int,
    duplicate: Int
)

/** Companion object for SubmissionService */
object SubmissionService:
    /** Access the SubmissionService from the ZIO environment */
    def submitTransactions(
        transactionIds: Seq[TransactionId]
    ): URIO[SubmissionService, SubmissionResult] =
        ZIO.serviceWithZIO[SubmissionService](_.submitTransactions(transactionIds))

    /** Submit a single transaction to YNAB */
    def submitTransaction(
        transactionId: TransactionId
    ): URIO[SubmissionService, TransactionSubmissionResult] =
        ZIO.serviceWithZIO[SubmissionService](_.submitTransaction(transactionId))

    /** Check if transactions meet requirements for submission */
    def validateForSubmission(
        transactionStates: Seq[TransactionProcessingState]
    ): URIO[SubmissionService, ValidationResult] =
        ZIO.serviceWithZIO[SubmissionService](_.validateForSubmission(transactionStates))

    /** Get statistics about transaction submission status */
    def getSubmissionStatistics(
        sourceAccountId: Option[Long] = None
    ): URIO[SubmissionService, SubmissionStatistics] =
        ZIO.serviceWithZIO[SubmissionService](_.getSubmissionStatistics(sourceAccountId))

    /** Create a layer for the SubmissionService implementation */
    def layer: URLayer[Any, SubmissionService] =
        ??? // To be implemented by concrete implementations
end SubmissionService
