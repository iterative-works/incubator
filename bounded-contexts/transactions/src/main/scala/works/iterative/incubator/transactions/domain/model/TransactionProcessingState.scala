package works.iterative.incubator.transactions.domain.model

import java.time.Instant

/** Represents the mutable processing state of a transaction
  *
  * This entity tracks how a transaction is being processed through the system, including
  * categorization, AI suggestions, user overrides, and YNAB integration state. It references the
  * immutable Transaction but contains all state that changes during the transaction processing
  * lifecycle.
  *
  * Classification: Domain Entity
  */
case class TransactionProcessingState(
    // Reference to the transaction this state applies to
    transactionId: TransactionId, // Reference to the immutable transaction

    // Processing state
    status: TransactionStatus, // Imported, Categorized, Submitted

    // AI computed/processed fields for YNAB
    suggestedPayeeName: Option[String], // AI suggested payee name
    suggestedCategory: Option[String], // AI suggested category
    suggestedMemo: Option[String], // AI cleaned/processed memo

    // User overrides (if user wants to adjust AI suggestions)
    overridePayeeName: Option[String], // User override for payee
    overrideCategory: Option[String], // User override for category
    overrideMemo: Option[String], // User override for memo

    // YNAB integration fields
    ynabTransactionId: Option[String], // ID assigned by YNAB after submission
    ynabAccountId: Option[String], // YNAB account ID where transaction was submitted

    // Processing timestamps
    processedAt: Option[Instant], // When AI processed this transaction
    submittedAt: Option[Instant] // When this was submitted to YNAB
):
    /** Get the effective payee name (user override or suggestion) */
    def effectivePayeeName: Option[String] = overridePayeeName.orElse(suggestedPayeeName)

    /** Get the effective category (user override or suggestion) */
    def effectiveCategory: Option[String] = overrideCategory.orElse(suggestedCategory)

    /** Get the effective memo (user override or suggestion) */
    def effectiveMemo: Option[String] = overrideMemo.orElse(suggestedMemo)

    /** Is this transaction ready to be submitted to YNAB? */
    def isReadyForSubmission: Boolean =
        status == TransactionStatus.Categorized &&
            ynabAccountId.isDefined &&
            effectivePayeeName.isDefined

    /** Create a new version with AI categorization applied */
    def withAICategorization(
        payeeName: Option[String],
        category: Option[String],
        memo: Option[String]
    ): TransactionProcessingState =
        this.copy(
            status = TransactionStatus.Categorized,
            suggestedPayeeName = payeeName,
            suggestedCategory = category,
            suggestedMemo = memo,
            processedAt = Some(Instant.now)
        )

    /** Create a new version with user overrides applied */
    def withUserOverrides(
        payeeName: Option[String] = this.overridePayeeName,
        category: Option[String] = this.overrideCategory,
        memo: Option[String] = this.overrideMemo
    ): TransactionProcessingState =
        this.copy(
            overridePayeeName = payeeName,
            overrideCategory = category,
            overrideMemo = memo
        )

    /** Create a new version with YNAB submission information */
    def withYnabSubmission(
        ynabTransactionId: String,
        ynabAccountId: String
    ): TransactionProcessingState =
        this.copy(
            status = TransactionStatus.Submitted,
            ynabTransactionId = Some(ynabTransactionId),
            ynabAccountId = Some(ynabAccountId),
            submittedAt = Some(Instant.now)
        )
end TransactionProcessingState

object TransactionProcessingState:
    /** Create an initial processing state for a new transaction */
    def initial(transaction: Transaction): TransactionProcessingState =
        TransactionProcessingState(
            transactionId = transaction.id,
            status = TransactionStatus.Imported,
            suggestedPayeeName = None,
            suggestedCategory = None,
            suggestedMemo = None,
            overridePayeeName = None,
            overrideCategory = None,
            overrideMemo = None,
            ynabTransactionId = None,
            ynabAccountId = None,
            processedAt = None,
            submittedAt = None
        )
end TransactionProcessingState