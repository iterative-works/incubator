package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.{Instant, LocalDate}
import scala.util.Try

/** Tests for the TransactionProcessingState domain entity */
object TransactionProcessingStateSpec extends ZIOSpecDefault:

    // Test setup - create a sample transaction
    private val transactionId = TransactionId(1L, "tx123")
    private val now = Instant.now()
    private val transaction = Transaction(
        id = transactionId,
        date = LocalDate.now(),
        amount = BigDecimal("100.00"),
        currency = "CZK",
        counterAccount = None,
        counterBankCode = None,
        counterBankName = None,
        variableSymbol = None,
        constantSymbol = None,
        specificSymbol = None,
        userIdentification = None,
        message = None,
        transactionType = "PAYMENT",
        comment = None,
        importedAt = now
    )

    def spec = suite("TransactionProcessingState")(
        test("should be initialized with Imported status") {
            val state = TransactionProcessingState.initial(transaction)
            assert(state.status)(equalTo(TransactionStatus.Imported)) &&
            assert(state.transactionId)(equalTo(transactionId)) &&
            assert(state.isDuplicate)(isFalse)
        },
        test("should transition to Categorized when AI categorization adds a category") {
            val state = TransactionProcessingState.initial(transaction)
            val categoryConfidence = ConfidenceScore(0.85)

            val categorizedState = state.withAICategorization(
                payeeName = Some("Shop"),
                category = Some("Groceries"),
                memo = Some("Weekly shopping"),
                categoryConfidence = Some(categoryConfidence),
                payeeConfidence = Some(ConfidenceScore(0.7))
            )

            assert(categorizedState.status)(equalTo(TransactionStatus.Categorized)) &&
            assert(categorizedState.suggestedCategory)(equalTo(Some("Groceries"))) &&
            assert(categorizedState.categoryConfidence)(equalTo(Some(categoryConfidence))) &&
            assert(categorizedState.processedAt.isDefined)(isTrue)
        },
        test("should remain in Imported status when AI categorization does not add a category") {
            val state = TransactionProcessingState.initial(transaction)

            val processed = state.withAICategorization(
                payeeName = Some("Unknown Shop"),
                category = None,
                memo = Some("Cannot determine category"),
                categoryConfidence = None
            )

            assert(processed.status)(equalTo(TransactionStatus.Imported)) &&
            assert(processed.suggestedCategory)(isNone) &&
            assert(processed.processedAt.isDefined)(isTrue)
        },
        test("should transition to Categorized when user provides a category override") {
            val state = TransactionProcessingState.initial(transaction)

            val userCategorizedState = state.withUserOverrides(
                category = Some("Entertainment")
            )

            assert(userCategorizedState.status)(equalTo(TransactionStatus.Categorized)) &&
            assert(userCategorizedState.overrideCategory)(equalTo(Some("Entertainment")))
        },
        test("should keep existing status when user provides overrides but no category") {
            val state = TransactionProcessingState.initial(transaction)

            val userOverriddenState = state.withUserOverrides(
                payeeName = Some("Custom Shop"),
                memo = Some("Custom memo")
            )

            assert(userOverriddenState.status)(equalTo(TransactionStatus.Imported)) &&
            assert(userOverriddenState.overridePayeeName)(equalTo(Some("Custom Shop"))) &&
            assert(userOverriddenState.overrideCategory)(isNone)
        },
        test("should transition to Submitted when submission is successful") {
            // Start with a categorized transaction
            val state = TransactionProcessingState.initial(transaction)
                .withAICategorization(
                    payeeName = Some("Shop"),
                    category = Some("Groceries"),
                    memo = Some("Weekly shopping")
                )

            val submittedState = state.withYnabSubmission(
                ynabTransactionId = "ynab-123",
                ynabAccountId = "ynab-account-456"
            )

            assert(submittedState.status)(equalTo(TransactionStatus.Submitted)) &&
            assert(submittedState.ynabTransactionId)(equalTo(Some("ynab-123"))) &&
            assert(submittedState.ynabAccountId)(equalTo(Some("ynab-account-456"))) &&
            assert(submittedState.submittedAt.isDefined)(isTrue)
        },
        test("should throw exception when trying to submit a transaction that is not categorized") {
            val state = TransactionProcessingState.initial(transaction)

            val result = Try(
                state.withYnabSubmission(
                    ynabTransactionId = "ynab-123",
                    ynabAccountId = "ynab-account-456"
                )
            )

            assert(result.isFailure)(isTrue) &&
            assert(result.failed.get.getMessage.contains(
                "Cannot submit transaction with status Imported"
            ))(isTrue)
        },
        test("should throw exception when trying to submit a transaction without a category") {
            // Create a state with Categorized status but no category
            val state = TransactionProcessingState.initial(transaction).copy(
                status = TransactionStatus.Categorized,
                suggestedPayeeName = Some("Shop"),
                suggestedCategory = None
            )

            val result = Try(
                state.withYnabSubmission(
                    ynabTransactionId = "ynab-123",
                    ynabAccountId = "ynab-account-456"
                )
            )

            assert(result.isFailure)(isTrue) &&
            assert(result.failed.get.getMessage.contains(
                "Cannot submit transaction without a category"
            ))(isTrue)
        },
        test("should throw exception when trying to submit a transaction without a payee name") {
            // Create a state with Categorized status but no payee name
            val state = TransactionProcessingState.initial(transaction).copy(
                status = TransactionStatus.Categorized,
                suggestedPayeeName = None,
                suggestedCategory = Some("Groceries")
            )

            val result = Try(
                state.withYnabSubmission(
                    ynabTransactionId = "ynab-123",
                    ynabAccountId = "ynab-account-456"
                )
            )

            assert(result.isFailure)(isTrue) &&
            assert(result.failed.get.getMessage.contains(
                "Cannot submit transaction without a payee name"
            ))(isTrue)
        },
        test("should correctly determine if it has a category with reliable confidence") {
            val state1 = TransactionProcessingState.initial(transaction).copy(
                categoryConfidence = Some(ConfidenceScore(0.8))
            )

            val state2 = TransactionProcessingState.initial(transaction).copy(
                categoryConfidence = Some(ConfidenceScore(0.6))
            )

            val state3 = TransactionProcessingState.initial(transaction).copy(
                categoryConfidence = None
            )

            assert(state1.hasCategoryWithReliableConfidence)(isTrue) &&
            assert(state2.hasCategoryWithReliableConfidence)(isFalse) &&
            assert(state3.hasCategoryWithReliableConfidence)(isFalse)
        },
        test("should be marked as a duplicate") {
            val state = TransactionProcessingState.initial(transaction)
            val duplicateState = state.markAsDuplicate

            assert(duplicateState.isDuplicate)(isTrue)
        },
        test("should not be ready for submission when it's a duplicate") {
            val state = TransactionProcessingState.initial(transaction)
                .withAICategorization(
                    payeeName = Some("Shop"),
                    category = Some("Groceries"),
                    memo = None
                )
                .copy(ynabAccountId = Some("ynab-account-456"))
                .markAsDuplicate

            assert(state.isReadyForSubmission)(isFalse)
        },
        test("should correctly return effective values") {
            // State with only suggestions
            val state1 = TransactionProcessingState.initial(transaction).copy(
                suggestedPayeeName = Some("AI Shop"),
                suggestedCategory = Some("AI Category"),
                suggestedMemo = Some("AI Memo")
            )

            // State with overrides
            val state2 = state1.withUserOverrides(
                payeeName = Some("User Shop"),
                category = Some("User Category"),
                memo = Some("User Memo")
            )

            // State with partial overrides
            val state3 = state1.withUserOverrides(
                category = Some("User Category Only")
            )

            assert(state1.effectivePayeeName)(equalTo(Some("AI Shop"))) &&
            assert(state1.effectiveCategory)(equalTo(Some("AI Category"))) &&
            assert(state1.effectiveMemo)(equalTo(Some("AI Memo"))) &&
            assert(state2.effectivePayeeName)(equalTo(Some("User Shop"))) &&
            assert(state2.effectiveCategory)(equalTo(Some("User Category"))) &&
            assert(state2.effectiveMemo)(equalTo(Some("User Memo"))) &&
            assert(state3.effectivePayeeName)(equalTo(Some("AI Shop"))) &&
            assert(state3.effectiveCategory)(equalTo(Some("User Category Only"))) &&
            assert(state3.effectiveMemo)(equalTo(Some("AI Memo")))
        }
    )

end TransactionProcessingStateSpec
