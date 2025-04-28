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
import works.iterative.incubator.budget.domain.port.CategorizationProvider
import works.iterative.incubator.budget.infrastructure.repository.inmemory.*

/** Test suite for CategorizationService domain logic.
  *
  * Tests verify the behavior of the transaction categorization workflow, including:
  * - Automated categorization of transactions
  * - Manual category updates
  * - Bulk category operations
  * - Confidence score calculation
  */
object CategorizationServiceSpec extends ZIOSpecDefault:
    def spec = 
        suite("CategorizationServiceSpec")(
            // Group tests by functional areas
            automatedCategorizationTests,
            manualOverrideTests,
            bulkOperationsTests,
            confidenceScoreTests
        )
    
    // Tests for automated categorization
    val automatedCategorizationTests = suite("Automated Categorization")(
        test("should categorize transactions based on content") {
            for
                env <- MockFactory.createForScenario("transaction-categorization")
                service <- makeCategorizationService(env)
                // Create test transactions
                accountId = 1L
                groceryTx = createTestTransaction(accountId, "tx-grocery", "Grocery Store Purchase")
                restaurantTx = createTestTransaction(accountId, "tx-restaurant", "Restaurant Payment")
                unknownTx = createTestTransaction(accountId, "tx-unknown", "Unknown Service")
                
                // Save transactions and initial processing states
                _ <- env.transactionRepo.save(groceryTx.id, groceryTx)
                _ <- env.transactionRepo.save(restaurantTx.id, restaurantTx)
                _ <- env.transactionRepo.save(unknownTx.id, unknownTx)
                _ <- env.processingStateRepo.save(groceryTx.id, TransactionProcessingState.initial(groceryTx))
                _ <- env.processingStateRepo.save(restaurantTx.id, TransactionProcessingState.initial(restaurantTx))
                _ <- env.processingStateRepo.save(unknownTx.id, TransactionProcessingState.initial(unknownTx))
                
                // When we categorize the transactions
                result <- service.categorizeTransactions(
                    Seq(groceryTx.id, restaurantTx.id, unknownTx.id)
                )
                
                // Then transactions should be categorized
                groceryState <- env.processingStateRepo.load(groceryTx.id)
                restaurantState <- env.processingStateRepo.load(restaurantTx.id)
                unknownState <- env.processingStateRepo.load(unknownTx.id)
            yield
                assertTrue(
                    result.categorizedCount == 3, // All transactions should be categorized
                    result.failedCount == 0,
                    // Grocery transaction should be categorized as "Groceries"
                    groceryState.exists(_.suggestedCategory.contains("Groceries")),
                    groceryState.exists(_.status == TransactionStatus.Categorized),
                    // Restaurant transaction should be categorized as "Dining Out"
                    restaurantState.exists(_.suggestedCategory.contains("Dining Out")),
                    restaurantState.exists(_.status == TransactionStatus.Categorized),
                    // Unknown transaction should get the default category
                    unknownState.exists(_.suggestedCategory.contains("Uncategorized")),
                    unknownState.exists(_.status == TransactionStatus.Categorized)
                )
        },
        
        test("should categorize a single transaction") {
            for
                env <- MockFactory.createForScenario("transaction-categorization")
                service <- makeCategorizationService(env)
                // Create test transaction
                accountId = 1L
                coffeeTx = createTestTransaction(accountId, "tx-coffee", "Coffee Shop")
                
                // Save transaction and processing state
                _ <- env.transactionRepo.save(coffeeTx.id, coffeeTx)
                _ <- env.processingStateRepo.save(coffeeTx.id, TransactionProcessingState.initial(coffeeTx))
                
                // When we categorize a single transaction
                resultOpt <- service.categorizeTransaction(coffeeTx.id)
                
                // Then the categorization should be correct
                coffeeState <- env.processingStateRepo.load(coffeeTx.id)
            yield
                assertTrue(
                    resultOpt.isDefined,
                    resultOpt.exists(_.categoryId.contains("Coffee Shops")),
                    resultOpt.exists(_.confidence.isDefined),
                    coffeeState.isDefined,
                    coffeeState.exists(_.suggestedCategory.contains("Coffee Shops")),
                    coffeeState.exists(_.status == TransactionStatus.Categorized)
                )
        },
        
        test("should handle missing transactions") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeCategorizationService(env)
                // When we categorize a non-existent transaction
                nonExistentId = TransactionId(999L, "non-existent")
                resultOpt <- service.categorizeTransaction(nonExistentId)
            yield
                assertTrue(
                    resultOpt.isEmpty
                )
        }
    )
    
    // Tests for manual category updates
    val manualOverrideTests = suite("Manual Category Updates")(
        test("should apply manual category override") {
            for
                env <- MockFactory.createForScenario("transaction-categorization")
                service <- makeCategorizationService(env)
                // Create test transaction with auto-categorization
                accountId = 1L
                tx = createTestTransaction(accountId, "tx-override", "Auto-categorized transaction")
                
                // Save transaction with initial auto-categorization
                _ <- env.transactionRepo.save(tx.id, tx)
                initialState = TransactionProcessingState.initial(tx)
                    .withAICategorization(
                        payeeName = Some("Auto Payee"),
                        category = Some("Initial Category"),
                        memo = Some("Auto memo"),
                        categoryConfidence = Some(ConfidenceScore(0.7))
                    )
                _ <- env.processingStateRepo.save(tx.id, initialState)
                
                // When we apply a manual override
                newCategoryId = "Manual Category"
                newMemo = Some("Manual memo")
                newPayee = Some("Manual payee")
                updatedStateOpt <- service.updateCategory(tx.id, newCategoryId, newMemo, newPayee)
                
                // Then the override values should be used
                retrievedStateOpt <- env.processingStateRepo.load(tx.id)
            yield
                assertTrue(
                    updatedStateOpt.isDefined,
                    updatedStateOpt.exists(_.overrideCategory.contains(newCategoryId)),
                    updatedStateOpt.exists(_.overrideMemo == newMemo),
                    updatedStateOpt.exists(_.overridePayeeName == newPayee),
                    // Original suggested values should be preserved
                    updatedStateOpt.exists(_.suggestedCategory.contains("Initial Category")),
                    updatedStateOpt.exists(_.suggestedPayeeName.contains("Auto Payee")),
                    // The effective values should be the override values
                    updatedStateOpt.exists(_.effectiveCategory.contains(newCategoryId)),
                    updatedStateOpt.exists(_.effectivePayeeName == newPayee),
                    // Changes should be persisted
                    retrievedStateOpt.isDefined,
                    retrievedStateOpt.exists(_.overrideCategory.contains(newCategoryId))
                )
        },
        
        test("should transition Imported to Categorized state when manual category applied") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeCategorizationService(env)
                // Create a test transaction with initial ImportedState
                accountId = 1L
                tx = createTestTransaction(accountId, "tx-transition", "Test Transaction")
                
                // Save transaction with initial state
                _ <- env.transactionRepo.save(tx.id, tx)
                initialState = TransactionProcessingState.initial(tx) // Status is Imported
                _ <- env.processingStateRepo.save(tx.id, initialState)
                
                // When we manually categorize the transaction
                updatedStateOpt <- service.updateCategory(tx.id, "New Category")
                
                // Then the status should transition to Categorized
                retrievedStateOpt <- env.processingStateRepo.load(tx.id)
            yield
                assertTrue(
                    initialState.status == TransactionStatus.Imported,
                    updatedStateOpt.isDefined,
                    updatedStateOpt.exists(_.status == TransactionStatus.Categorized),
                    retrievedStateOpt.exists(_.status == TransactionStatus.Categorized)
                )
        }
    )
    
    // Tests for bulk operations
    val bulkOperationsTests = suite("Bulk Category Operations")(
        test("should update categories in bulk by filter criteria") {
            for
                env <- MockFactory.createForScenario("transaction-categorization")
                service <- makeCategorizationService(env)
                // Create and save multiple transactions
                accountId = 1L
                txs = Seq(
                    createTestTransaction(accountId, "tx-bulk-1", "Coffee Purchase", amount = -5.0),
                    createTestTransaction(accountId, "tx-bulk-2", "Coffee Shop", amount = -10.0),
                    createTestTransaction(accountId, "tx-bulk-3", "Restaurant Visit", amount = -25.0),
                    createTestTransaction(accountId, "tx-bulk-4", "Coffee Break", amount = -3.5)
                )
                
                // Save all transactions and their initial states
                _ <- ZIO.foreach(txs)(tx => 
                    env.transactionRepo.save(tx.id, tx) *>
                    env.processingStateRepo.save(tx.id, TransactionProcessingState.initial(tx))
                )
                
                // Define filter to match coffee-related transactions
                filter = TransactionFilter(
                    sourceAccountId = Some(accountId),
                    descriptionContains = Some("Coffee")
                )
                
                // When we apply bulk updates for coffee-related transactions
                newCategoryId = "Coffee Budget"
                updatedCount <- service.bulkUpdateCategory(filter, newCategoryId)
                
                // Then all matching transactions should be updated
                states <- ZIO.foreach(txs)(tx => env.processingStateRepo.load(tx.id))
                // Get transactions with "Coffee" in the message
                coffeeStates = states.flatten.filter(state => 
                    txs.exists(tx => 
                        tx.id == state.transactionId && tx.message.exists(_.contains("Coffee"))
                    )
                )
            yield
                assertTrue(
                    updatedCount == 3, // Three coffee-related transactions
                    coffeeStates.size == 3,
                    coffeeStates.forall(_.overrideCategory.contains("Coffee Budget")),
                    coffeeStates.forall(_.status == TransactionStatus.Categorized)
                )
        },
        
        test("should filter by amount range in bulk update") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeCategorizationService(env)
                // Create and save transactions with different amounts
                accountId = 1L
                txs = Seq(
                    createTestTransaction(accountId, "tx-amt-1", "Small Payment", amount = -5.0),
                    createTestTransaction(accountId, "tx-amt-2", "Medium Payment", amount = -25.0),
                    createTestTransaction(accountId, "tx-amt-3", "Large Payment", amount = -100.0)
                )
                
                // Save all transactions and their initial states
                _ <- ZIO.foreach(txs)(tx => 
                    env.transactionRepo.save(tx.id, tx) *>
                    env.processingStateRepo.save(tx.id, TransactionProcessingState.initial(tx))
                )
                
                // Define filter to match medium-sized payments
                filter = TransactionFilter(
                    minAmount = Some(BigDecimal(-50)),
                    maxAmount = Some(BigDecimal(-10))
                )
                
                // When we apply bulk updates for medium-sized payments
                newCategoryId = "Medium Expenses"
                updatedCount <- service.bulkUpdateCategory(filter, newCategoryId)
                
                // Then only the medium payment should be updated
                mediumStateOpt <- env.processingStateRepo.load(txs(1).id)
                otherStates <- ZIO.foreach(Seq(txs(0).id, txs(2).id))(id => 
                    env.processingStateRepo.load(id)
                )
            yield
                assertTrue(
                    updatedCount == 1, // Only one transaction matches
                    mediumStateOpt.exists(_.overrideCategory.contains("Medium Expenses")),
                    mediumStateOpt.exists(_.status == TransactionStatus.Categorized),
                    otherStates.flatten.forall(_.overrideCategory.isEmpty)
                )
        }
    )
    
    // Tests for confidence scores
    val confidenceScoreTests = suite("Confidence Scores")(
        test("should calculate average confidence score") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeCategorizationService(env)
                // Create test categorizations with different confidence scores
                categorizations = Seq(
                    TransactionCategorization(
                        transactionId = TransactionId(1L, "tx-1"),
                        categoryId = Some("Category 1"),
                        payeeName = Some("Payee 1"),
                        memo = None,
                        confidence = Some(ConfidenceScore(0.8))
                    ),
                    TransactionCategorization(
                        transactionId = TransactionId(1L, "tx-2"),
                        categoryId = Some("Category 2"),
                        payeeName = Some("Payee 2"),
                        memo = None,
                        confidence = Some(ConfidenceScore(0.6))
                    ),
                    TransactionCategorization(
                        transactionId = TransactionId(1L, "tx-3"),
                        categoryId = Some("Category 3"),
                        payeeName = Some("Payee 3"),
                        memo = None,
                        confidence = Some(ConfidenceScore(0.7))
                    )
                )
                
                // When we calculate the average confidence
                avgConfidence <- ZIO.fromOption(service.calculateAverageConfidence(categorizations))
                    .orElseFail("No average confidence calculated")
            yield
                // Then the average should be correct (0.8 + 0.6 + 0.7) / 3 = 0.7
                assertTrue(
                    avgConfidence.value == 0.7
                )
        },
        
        test("should handle empty confidence scores") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeCategorizationService(env)
                // Create test categorizations with missing confidence scores
                categorizations = Seq(
                    TransactionCategorization(
                        transactionId = TransactionId(1L, "tx-1"),
                        categoryId = Some("Category 1"),
                        payeeName = Some("Payee 1"),
                        memo = None,
                        confidence = None
                    ),
                    TransactionCategorization(
                        transactionId = TransactionId(1L, "tx-2"),
                        categoryId = Some("Category 2"),
                        payeeName = Some("Payee 2"),
                        memo = None,
                        confidence = None
                    )
                )
                
                // When we calculate the average confidence
                avgConfidenceOpt = service.calculateAverageConfidence(categorizations)
            yield
                // Then the result should be None as no valid scores exist
                assertTrue(
                    avgConfidenceOpt.isEmpty
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
    
    /** Creates the service under test with mock dependencies */
    private def makeCategorizationService(env: MockFactory.MockEnvironment): UIO[CategorizationService] =
        // Create a categorization strategy that uses the mock categorization provider
        val categorizationStrategy = new CategorizationStrategy {
            def categorize(transaction: Transaction): UIO[TransactionCategorization] =
                // Call the mock provider's categorizeTransaction method and handle errors
                env.categorizationProvider.categorizeTransaction(transaction)
                  .fold(
                    _ => works.iterative.incubator.budget.domain.service.TransactionCategorization(
                        transactionId = transaction.id,
                        categoryId = None,
                        payeeName = None,
                        memo = None,
                        confidence = None
                    ),
                    categorization => works.iterative.incubator.budget.domain.service.TransactionCategorization(
                        transactionId = transaction.id,
                        categoryId = Some(categorization.category.id),
                        payeeName = transaction.userIdentification,
                        memo = transaction.message,
                        confidence = Some(categorization.confidenceScore)
                    )
                  )
        }
        
        ZIO.succeed(
            new CategorizationServiceImpl(
                transactionRepository = env.transactionRepo,
                processingStateRepository = env.processingStateRepo,
                categoryRepository = env.categoryRepo,
                categorizationStrategy = categorizationStrategy,
                eventPublisher = event => ZIO.succeed(()) // No-op event publisher for tests
            )
        )
end CategorizationServiceSpec