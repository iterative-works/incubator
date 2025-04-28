package works.iterative.incubator.budget.domain.service.test

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.domain.service.impl.*
import works.iterative.incubator.budget.domain.event.*
import works.iterative.incubator.budget.domain.mock.MockFactory
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.port.TransactionSubmissionPort
import works.iterative.incubator.budget.infrastructure.repository.inmemory.*

/** Test suite for SubmissionService domain logic.
  *
  * Tests verify the behavior of the transaction submission workflow, including:
  * - Validation of transactions before submission
  * - Submission to external systems
  * - Error handling and duplicate prevention
  * - Statistics calculation
  */
object SubmissionServiceSpec extends ZIOSpecDefault:
    def spec = 
        suite("SubmissionServiceSpec")(
            // Group tests by functional areas
            validationTests,
            submissionTests,
            duplicatePreventionTests,
            statisticsTests
        )
    
    // Tests for transaction validation before submission
    val validationTests = suite("Submission Validation")(
        test("should validate transactions ready for submission") {
            for
                env <- MockFactory.createForScenario("transaction-submission")
                service <- makeSubmissionService(env)
                
                // Create test transactions in various states
                accountId = 1L
                // Valid transaction ready for submission
                readyState = createProcessingState(
                    accountId, 
                    "tx-valid", 
                    TransactionStatus.Categorized,
                    categoryId = Some("Food:Groceries"),
                    payeeName = Some("Grocery Store"),
                    ynabAccountId = Some("ynab-account-1")
                )
                
                // Missing category - not valid for submission
                missingCategoryState = createProcessingState(
                    accountId,
                    "tx-no-category",
                    TransactionStatus.Categorized,
                    categoryId = None,
                    payeeName = Some("Unknown Store"),
                    ynabAccountId = Some("ynab-account-1")
                )
                
                // Missing YNAB account - not valid for submission
                noAccountState = createProcessingState(
                    accountId,
                    "tx-no-account",
                    TransactionStatus.Categorized,
                    categoryId = Some("Transportation"),
                    payeeName = Some("Fuel Station"),
                    ynabAccountId = None
                )
                
                // Already submitted - not valid for submission
                submittedState = createProcessingState(
                    accountId,
                    "tx-submitted",
                    TransactionStatus.Submitted,
                    categoryId = Some("Entertainment"),
                    payeeName = Some("Cinema"),
                    ynabAccountId = Some("ynab-account-1"),
                    ynabTransactionId = Some("ynab-tx-1")
                )
                
                // When we validate the transactions
                validationResult = service.validateForSubmission(
                    Seq(readyState, missingCategoryState, noAccountState, submittedState)
                )
            yield
                assertTrue(
                    validationResult.validTransactions.size == 1,
                    validationResult.validTransactions.contains(readyState),
                    validationResult.invalidTransactions.size == 3,
                    validationResult.invalidTransactions.exists(_._1 == missingCategoryState),
                    validationResult.invalidTransactions.exists(_._1 == noAccountState),
                    validationResult.invalidTransactions.exists(_._1 == submittedState)
                )
        }
    )
    
    // Tests for submitting transactions
    val submissionTests = suite("Transaction Submission")(
        test("should submit valid transactions to external system") {
            for
                env <- MockFactory.createForScenario("transaction-submission")
                service <- makeSubmissionService(env)
                
                // Create and save test transactions
                accountId = 1L
                tx1 = createTestTransaction(accountId, "tx-submit-1", "Grocery Shopping")
                tx2 = createTestTransaction(accountId, "tx-submit-2", "Dining Out")
                
                // Save transactions
                _ <- env.transactionRepo.save(tx1.id, tx1)
                _ <- env.transactionRepo.save(tx2.id, tx2)
                
                // Create and save properly categorized processing states
                ps1 = createProcessingState(
                    accountId,
                    "tx-submit-1",
                    TransactionStatus.Categorized,
                    categoryId = Some("Food:Groceries"),
                    payeeName = Some("Grocery Store"),
                    ynabAccountId = Some("ynab-account-1")
                )
                ps2 = createProcessingState(
                    accountId,
                    "tx-submit-2",
                    TransactionStatus.Categorized,
                    categoryId = Some("Food:Dining Out"),
                    payeeName = Some("Restaurant"),
                    ynabAccountId = Some("ynab-account-1")
                )
                
                _ <- env.processingStateRepo.save(ps1.transactionId, ps1)
                _ <- env.processingStateRepo.save(ps2.transactionId, ps2)
                
                // Configure the mock submission port to accept these transactions
                _ <- env.submissionPort.setSubmissionResponse(tx1.id, 
                    SubmissionResponse(true, Some("ynab-result-1"), None))
                _ <- env.submissionPort.setSubmissionResponse(tx2.id,
                    SubmissionResponse(true, Some("ynab-result-2"), None))
                
                // When we submit the transactions
                result <- service.submitTransactions(Seq(tx1.id, tx2.id))
                
                // Then the submission should succeed
                ps1After <- env.processingStateRepo.findById(tx1.id)
                ps2After <- env.processingStateRepo.findById(tx2.id)
            yield
                assertTrue(
                    // Check result
                    result.submittedCount == 2,
                    result.failedCount == 0,
                    result.errors.isEmpty,
                    
                    // Check updated processing states
                    ps1After.isDefined,
                    ps1After.exists(_.status == TransactionStatus.Submitted),
                    ps1After.exists(_.ynabTransactionId.contains("ynab-result-1")),
                    
                    ps2After.isDefined,
                    ps2After.exists(_.status == TransactionStatus.Submitted),
                    ps2After.exists(_.ynabTransactionId.contains("ynab-result-2"))
                )
        },
        
        test("should handle failed submissions") {
            for
                env <- MockFactory.createForScenario("transaction-submission")
                service <- makeSubmissionService(env)
                
                // Create and save test transaction
                accountId = 1L
                tx = createTestTransaction(accountId, "tx-fail", "Failed Transaction")
                
                // Save transaction
                _ <- env.transactionRepo.save(tx.id, tx)
                
                // Create and save processing state
                ps = createProcessingState(
                    accountId,
                    "tx-fail",
                    TransactionStatus.Categorized,
                    categoryId = Some("Misc"),
                    payeeName = Some("Some Store"),
                    ynabAccountId = Some("ynab-account-1")
                )
                
                _ <- env.processingStateRepo.save(ps.transactionId, ps)
                
                // Configure the mock submission port to reject this transaction
                errorMsg = "API Error: Budget not found"
                _ <- env.submissionPort.setSubmissionResponse(tx.id, 
                    SubmissionResponse(false, None, Some(errorMsg)))
                
                // When we attempt to submit the transaction
                result <- service.submitTransactions(Seq(tx.id))
                
                // Then the submission should fail properly
                psAfter <- env.processingStateRepo.findById(tx.id)
            yield
                assertTrue(
                    // Check result
                    result.submittedCount == 0,
                    result.failedCount == 1,
                    result.errors.size == 1,
                    result.errors.head.transactionId == tx.id,
                    result.errors.head.reason.contains(errorMsg),
                    
                    // Processing state should not be updated to Submitted
                    psAfter.isDefined,
                    psAfter.exists(_.status == TransactionStatus.Categorized),
                    psAfter.exists(_.ynabTransactionId.isEmpty)
                )
        },
        
        test("should submit a single transaction") {
            for
                env <- MockFactory.createForScenario("transaction-submission")
                service <- makeSubmissionService(env)
                
                // Create and save test transaction
                accountId = 1L
                tx = createTestTransaction(accountId, "tx-single", "Single Transaction")
                
                // Save transaction
                _ <- env.transactionRepo.save(tx.id, tx)
                
                // Create and save processing state
                ps = createProcessingState(
                    accountId,
                    "tx-single",
                    TransactionStatus.Categorized,
                    categoryId = Some("Entertainment"),
                    payeeName = Some("Movie Theater"),
                    ynabAccountId = Some("ynab-account-1")
                )
                
                _ <- env.processingStateRepo.save(ps.transactionId, ps)
                
                // Configure the mock submission port
                ynabId = "ynab-result-single"
                _ <- env.submissionPort.setSubmissionResponse(tx.id, 
                    SubmissionResponse(true, Some(ynabId), None))
                
                // When we submit a single transaction
                result <- service.submitTransaction(tx.id)
                
                // Then the submission should succeed
                psAfter <- env.processingStateRepo.findById(tx.id)
            yield
                assertTrue(
                    // Check result
                    result.submitted,
                    result.ynabTransactionId.contains(ynabId),
                    result.error.isEmpty,
                    
                    // Check updated processing state
                    psAfter.isDefined,
                    psAfter.exists(_.status == TransactionStatus.Submitted),
                    psAfter.exists(_.ynabTransactionId.contains(ynabId))
                )
        }
    )
    
    // Tests for duplicate prevention
    val duplicatePreventionTests = suite("Duplicate Prevention")(
        test("should prevent resubmission of already submitted transactions") {
            for
                env <- MockFactory.createForScenario("transaction-submission")
                service <- makeSubmissionService(env)
                
                // Create and save a transaction that has already been submitted
                accountId = 1L
                tx = createTestTransaction(accountId, "tx-already-submitted", "Previously Submitted")
                
                // Save transaction
                _ <- env.transactionRepo.save(tx.id, tx)
                
                // Create and save processing state with Submitted status
                ps = createProcessingState(
                    accountId,
                    "tx-already-submitted",
                    TransactionStatus.Submitted, // Already submitted
                    categoryId = Some("Bills"),
                    payeeName = Some("Electricity Company"),
                    ynabAccountId = Some("ynab-account-1"),
                    ynabTransactionId = Some("existing-ynab-id")
                )
                
                _ <- env.processingStateRepo.save(ps.transactionId, ps)
                
                // When we try to submit it again
                result <- service.submitTransaction(tx.id)
                
                // Then submission should be rejected due to duplicate
                psAfter <- env.processingStateRepo.findById(tx.id)
            yield
                assertTrue(
                    // Check result
                    !result.submitted,
                    result.ynabTransactionId.isEmpty,
                    result.error.isDefined,
                    result.error.exists(_.reason.contains("already submitted")),
                    
                    // Processing state should remain unchanged
                    psAfter.isDefined,
                    psAfter.exists(_.status == TransactionStatus.Submitted),
                    psAfter.exists(_.ynabTransactionId.contains("existing-ynab-id"))
                )
        }
    )
    
    // Tests for statistics
    val statisticsTests = suite("Submission Statistics")(
        test("should calculate correct statistics") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeSubmissionService(env)
                
                // Create test transactions with various statuses
                accountId = 1L
                
                // Create 5 imported transactions
                importedTxs = (1 to 5).map(i => 
                    createTestTransaction(accountId, s"tx-imported-$i", s"Imported Transaction $i")
                )
                
                // Create 3 categorized transactions
                categorizedTxs = (1 to 3).map(i => 
                    createTestTransaction(accountId, s"tx-categorized-$i", s"Categorized Transaction $i")
                )
                
                // Create 2 submitted transactions
                submittedTxs = (1 to 2).map(i => 
                    createTestTransaction(accountId, s"tx-submitted-$i", s"Submitted Transaction $i")
                )
                
                // Create 1 duplicate transaction
                duplicateTxs = Seq(
                    createTestTransaction(accountId, "tx-duplicate", "Duplicate Transaction")
                )
                
                // Save all transactions
                _ <- ZIO.foreach(importedTxs ++ categorizedTxs ++ submittedTxs ++ duplicateTxs)(tx =>
                    env.transactionRepo.save(tx.id, tx)
                )
                
                // Create and save processing states
                _ <- ZIO.foreach(importedTxs)(tx =>
                    env.processingStateRepo.save(tx.id, createProcessingState(
                        accountId, tx.id.externalId, TransactionStatus.Imported
                    ))
                )
                
                _ <- ZIO.foreach(categorizedTxs)(tx =>
                    env.processingStateRepo.save(tx.id, createProcessingState(
                        accountId, tx.id.externalId, TransactionStatus.Categorized,
                        categoryId = Some("Test Category"),
                        payeeName = Some("Test Payee")
                    ))
                )
                
                _ <- ZIO.foreach(submittedTxs)(tx =>
                    env.processingStateRepo.save(tx.id, createProcessingState(
                        accountId, tx.id.externalId, TransactionStatus.Submitted,
                        categoryId = Some("Test Category"),
                        payeeName = Some("Test Payee"),
                        ynabAccountId = Some("test-account"),
                        ynabTransactionId = Some(s"ynab-${tx.id.externalId}")
                    ))
                )
                
                _ <- ZIO.foreach(duplicateTxs)(tx =>
                    env.processingStateRepo.save(tx.id, createProcessingState(
                        accountId, tx.id.externalId, TransactionStatus.Imported,
                        isDuplicate = true
                    ))
                )
                
                // When we get submission statistics
                stats <- service.getSubmissionStatistics(Some(accountId))
                
            yield
                assertTrue(
                    // Check statistics match expected counts
                    stats.total == 11, // 5 + 3 + 2 + 1
                    stats.imported == 5,
                    stats.categorized == 3,
                    stats.submitted == 2,
                    stats.duplicate == 1
                )
        }
    )
    
    // Helpers
    
    /** Creates a test transaction with specified properties */
    private def createTestTransaction(
        sourceAccountId: Long,
        id: String,
        message: String,
        amount: BigDecimal = -50.0
    ): Transaction =
        Transaction(
            id = TransactionId(sourceAccountId, id),
            date = java.time.LocalDate.now(),
            amount = amount,
            currency = "CZK",
            counterAccount = None,
            counterBankCode = None,
            counterBankName = None,
            variableSymbol = None,
            constantSymbol = None,
            specificSymbol = None,
            userIdentification = None,
            message = Some(message),
            transactionType = "PAYMENT",
            comment = None,
            importedAt = java.time.Instant.now()
        )
    
    /** Creates a transaction processing state with specified properties */
    private def createProcessingState(
        sourceAccountId: Long,
        txId: String,
        status: TransactionStatus,
        categoryId: Option[String] = None,
        payeeName: Option[String] = None,
        memo: Option[String] = None,
        ynabAccountId: Option[String] = None,
        ynabTransactionId: Option[String] = None,
        isDuplicate: Boolean = false
    ): TransactionProcessingState =
        TransactionProcessingState(
            transactionId = TransactionId(sourceAccountId, txId),
            status = status,
            isDuplicate = isDuplicate,
            
            // Suggestions
            suggestedPayeeName = payeeName.map(_ + " (suggested)"),
            suggestedCategory = categoryId.map(_ + " (suggested)"),
            suggestedMemo = memo.map(_ + " (suggested)"),
            
            // Confidence
            categoryConfidence = Some(ConfidenceScore(0.8)),
            payeeConfidence = Some(ConfidenceScore(0.7)),
            
            // Overrides - for valid submission these should take precedence
            overridePayeeName = payeeName,
            overrideCategory = categoryId,
            overrideMemo = memo,
            
            // YNAB information
            ynabTransactionId = ynabTransactionId,
            ynabAccountId = ynabAccountId,
            
            // Timestamps
            processedAt = Some(java.time.Instant.now().minusMillis(10000)),
            submittedAt = if (status == TransactionStatus.Submitted) 
                Some(java.time.Instant.now()) else None
        )
    
    /** Response from mock submission port */
    case class SubmissionResponse(
        success: Boolean,
        ynabId: Option[String],
        error: Option[String]
    )
    
    /** Creates the service under test with mock dependencies */
    private def makeSubmissionService(env: MockFactory.MockEnvironment): UIO[SubmissionService] =
        ZIO.succeed(
            new SubmissionServiceImpl(
                transactionRepository = env.transactionRepo,
                processingStateRepository = env.processingStateRepo,
                submissionPort = env.submissionPort,
                eventPublisher = event => ZIO.succeed(()) // No-op event publisher for tests
            )
        )
end SubmissionServiceSpec