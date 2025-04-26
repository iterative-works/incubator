package works.iterative.incubator.budget.domain.model

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
    isDuplicate: Boolean = false, // Whether this transaction is a duplicate

    // AI computed/processed fields for YNAB
    suggestedPayeeName: Option[String], // AI suggested payee name
    suggestedCategory: Option[String], // AI suggested category
    suggestedMemo: Option[String], // AI cleaned/processed memo

    // Confidence scores for AI suggestions
    categoryConfidence: Option[ConfidenceScore], // How confident is the AI about the category
    payeeConfidence: Option[ConfidenceScore], // How confident is the AI about the payee

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
            effectivePayeeName.isDefined &&
            effectiveCategory.isDefined &&
            !isDuplicate

    /** Check if this is a manually categorized transaction */
    def isManuallyCategoriazed: Boolean = overrideCategory.isDefined

    /** Check if the category suggestion is reliable based on confidence score */
    def hasCategoryWithReliableConfidence: Boolean =
        categoryConfidence.exists(_.exceeds(ConfidenceScore.ReliableThreshold))

    /** Create a new version with AI categorization applied */
    def withAICategorization(
        payeeName: Option[String],
        category: Option[String],
        memo: Option[String],
        categoryConfidence: Option[ConfidenceScore] = None,
        payeeConfidence: Option[ConfidenceScore] = None
    ): TransactionProcessingState =
        // Only transition to Categorized if we actually have a category
        val newStatus = if category.isDefined then TransactionStatus.Categorized else this.status

        this.copy(
            status = newStatus,
            suggestedPayeeName = payeeName,
            suggestedCategory = category,
            suggestedMemo = memo,
            categoryConfidence = categoryConfidence,
            payeeConfidence = payeeConfidence,
            processedAt = Some(Instant.now)
        )
    end withAICategorization

    /** Create a new version with user overrides applied */
    def withUserOverrides(
        payeeName: Option[String] = this.overridePayeeName,
        category: Option[String] = this.overrideCategory,
        memo: Option[String] = this.overrideMemo
    ): TransactionProcessingState =
        // If user provides a category, state becomes Categorized
        val newStatus =
            if category.isDefined && this.status == TransactionStatus.Imported then
                TransactionStatus.Categorized
            else
                this.status

        this.copy(
            status = newStatus,
            overridePayeeName = payeeName,
            overrideCategory = category,
            overrideMemo = memo
        )
    end withUserOverrides

    /** Create a new version with YNAB submission information */
    def withYnabSubmission(
        ynabTransactionId: String,
        ynabAccountId: String
    ): TransactionProcessingState =
        // Can only submit if the transaction is categorized
        if status != TransactionStatus.Categorized then
            throw new IllegalStateException(
                s"Cannot submit transaction with status $status, must be Categorized"
            )

        // Check that we have all required fields
        if effectiveCategory.isEmpty then
            throw new IllegalStateException("Cannot submit transaction without a category")

        if effectivePayeeName.isEmpty then
            throw new IllegalStateException("Cannot submit transaction without a payee name")

        this.copy(
            status = TransactionStatus.Submitted,
            ynabTransactionId = Some(ynabTransactionId),
            ynabAccountId = Some(ynabAccountId),
            submittedAt = Some(Instant.now)
        )
    end withYnabSubmission

    /** Mark this transaction as a duplicate */
    def markAsDuplicate: TransactionProcessingState =
        this.copy(isDuplicate = true)
end TransactionProcessingState

object TransactionProcessingState:
    /** Create an initial processing state for a new transaction */
    def initial(transaction: Transaction): TransactionProcessingState =
        TransactionProcessingState(
            transactionId = transaction.id,
            status = TransactionStatus.Imported,
            isDuplicate = false,
            suggestedPayeeName = None,
            suggestedCategory = None,
            suggestedMemo = None,
            categoryConfidence = None,
            payeeConfidence = None,
            overridePayeeName = None,
            overrideCategory = None,
            overrideMemo = None,
            ynabTransactionId = None,
            ynabAccountId = None,
            processedAt = None,
            submittedAt = None
        )
end TransactionProcessingState
