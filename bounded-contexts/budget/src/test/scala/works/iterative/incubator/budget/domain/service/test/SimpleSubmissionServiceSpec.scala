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

/**
 * Test suite for SubmissionService domain logic.
 *
 * A simplified implementation testing core functionality.
 */
object SimpleSubmissionServiceSpec extends ZIOSpecDefault:
  def spec = suite("SubmissionServiceSpec")(
    test("should submit valid transactions") {
      for 
        env <- MockFactory.createMockEnvironment
        
        // Create a success-returning test YnabSubmitter
        ynabId = "ynab-result-success"
        successSubmitter = new YnabSubmitter {
          def submitTransaction(request: YnabSubmissionRequest): UIO[Either[String, YnabSubmissionResponse]] =
            ZIO.succeed(Right(YnabSubmissionResponse(ynabId, "ynab-account-1")))
        }
        
        // Create a test service with our success submitter
        testService = new SubmissionServiceImpl(
          processingStateRepository = env.processingStateRepo,
          transactionRepository = env.transactionRepo,
          ynabSubmitter = successSubmitter,
          eventPublisher = event => ZIO.succeed(())
        )
        
        // Create test transaction
        accountId = 1L
        tx = createTestTransaction(accountId, "tx-test", "Test Transaction")
        _ <- env.transactionRepo.save(tx.id, tx)
        
        // Create test processing state
        ps = createProcessingState(
          accountId, 
          "tx-test", 
          TransactionStatus.Categorized,
          categoryId = Some("Test Category"),
          payeeName = Some("Test Payee"),
          ynabAccountId = Some("ynab-account-1")
        )
        _ <- env.processingStateRepo.save(ps.transactionId, ps)
        
        // Submit the transaction
        result <- testService.submitTransaction(tx.id)
        psAfter <- env.processingStateRepo.load(tx.id)
      yield assertTrue(
        result.submitted,
        psAfter.isDefined,
        psAfter.exists(_.status == TransactionStatus.Submitted)
      )
    },
    
    test("should track submission statistics") {
      for
        env <- MockFactory.createMockEnvironment
        service <- makeSubmissionService(env)
        
        accountId = 1L
        
        // Create test transaction
        tx1 = createTestTransaction(accountId, "tx-submitted", "Submitted Transaction")
        tx2 = createTestTransaction(accountId, "tx-imported", "Imported Transaction")
        
        // Save transactions
        _ <- env.transactionRepo.save(tx1.id, tx1)
        _ <- env.transactionRepo.save(tx2.id, tx2)
        
        // Create different processing states
        psSubmitted = createProcessingState(
          accountId, 
          "tx-submitted", 
          TransactionStatus.Submitted,
          categoryId = Some("Test Category"),
          payeeName = Some("Test Payee"),
          ynabAccountId = Some("test-account-id"),
          ynabTransactionId = Some("ynab-tx-1")
        )
        
        psImported = createProcessingState(
          accountId, 
          "tx-imported", 
          TransactionStatus.Imported
        )
        
        // Save processing states
        _ <- env.processingStateRepo.save(psSubmitted.transactionId, psSubmitted)
        _ <- env.processingStateRepo.save(psImported.transactionId, psImported)
        
        // Get statistics
        stats <- service.getSubmissionStatistics(Some(accountId))
      yield assertTrue(
        stats.total == 2,
        stats.submitted == 1,
        stats.imported == 1
      )
    }
  )
  
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
  
  /** Creates the service under test with mock dependencies */
  private def makeSubmissionService(env: MockFactory.MockEnvironment): UIO[SubmissionService] =
      // Create a test YnabSubmitter that generates successful responses
      val ynabSubmitter = new YnabSubmitter {
          def submitTransaction(request: YnabSubmissionRequest): UIO[Either[String, YnabSubmissionResponse]] = {
              // Return a successful response by default
              ZIO.succeed(Right(YnabSubmissionResponse("test-ynab-id", "test-account-id")))
          }
      }
      
      ZIO.succeed(
          new SubmissionServiceImpl(
              processingStateRepository = env.processingStateRepo,
              transactionRepository = env.transactionRepo,
              ynabSubmitter = ynabSubmitter,
              eventPublisher = event => ZIO.succeed(()) // No-op event publisher for tests
          )
      )
end SimpleSubmissionServiceSpec