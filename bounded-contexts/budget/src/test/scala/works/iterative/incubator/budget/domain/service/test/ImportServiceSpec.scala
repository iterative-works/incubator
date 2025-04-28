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
import works.iterative.incubator.budget.domain.port.TransactionProvider
import works.iterative.incubator.budget.infrastructure.repository.inmemory.*

/** Test suite for ImportService domain logic.
  *
  * Tests verify the behavior of the transaction import workflow, including:
  * - Transaction import from source accounts
  * - Duplicate detection
  * - Event publication for imported transactions
  * - Error handling
  */
object ImportServiceSpec extends ZIOSpecDefault:
    def spec = 
        suite("ImportServiceSpec")(
            // Group tests by functional areas
            importTransactionsTests,
            duplicateDetectionTests,
            eventPublicationTests,
            errorHandlingTests
        )
    
    // Tests for importing transactions
    val importTransactionsTests = suite("Import Transactions")(
        test("should successfully import valid transactions") {
            // Given source account and transactions exist
            for
                env <- MockFactory.createForScenario("transaction-import")
                service <- makeImportService(env)
                accountId = 1L
                rawTransactions = Seq(
                    RawTransaction(
                        externalId = "tx-new-1",
                        date = java.time.LocalDate.now(),
                        amount = 150.0,
                        currency = "CZK",
                        counterAccount = None,
                        counterBankCode = None,
                        counterBankName = None,
                        variableSymbol = None,
                        constantSymbol = None,
                        specificSymbol = None,
                        userIdentification = None,
                        message = Some("New Transaction"),
                        transactionType = "PAYMENT",
                        comment = None
                    ),
                    RawTransaction(
                        externalId = "tx-new-2",
                        date = java.time.LocalDate.now(),
                        amount = -75.0,
                        currency = "CZK",
                        counterAccount = None,
                        counterBankCode = None,
                        counterBankName = None,
                        variableSymbol = None,
                        constantSymbol = None,
                        specificSymbol = None,
                        userIdentification = None,
                        message = Some("Another Transaction"),
                        transactionType = "PAYMENT",
                        comment = None
                    )
                )
                // When we import the transactions
                result <- service.importTransactions(accountId, rawTransactions)
                (importCount, duplicates) = result
                // Then we should have all transactions imported and no duplicates
                allTransactions <- env.transactionRepo.find(works.iterative.incubator.budget.domain.query.TransactionQuery())
                allProcessingStates <- env.processingStateRepo.find(works.iterative.incubator.budget.domain.query.TransactionProcessingStateQuery())
            yield
                assertTrue(
                    importCount == 2,
                    duplicates.isEmpty,
                    allTransactions.size == 2,
                    allProcessingStates.size == 2,
                    allProcessingStates.forall(_.status == TransactionStatus.Imported),
                    allTransactions.exists(_.message.contains("New Transaction")),
                    allTransactions.exists(_.message.contains("Another Transaction"))
                )
        },
        
        test("should create processing state for each imported transaction") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeImportService(env)
                accountId = 1L
                rawTransaction = RawTransaction(
                    externalId = "tx-state-1",
                    date = java.time.LocalDate.now(),
                    amount = 100.0,
                    currency = "CZK",
                    counterAccount = None,
                    counterBankCode = None,
                    counterBankName = None,
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = None,
                    message = Some("Test Transaction"),
                    transactionType = "PAYMENT",
                    comment = None
                )
                // When we import a transaction
                resultOpt <- service.importTransaction(rawTransaction, accountId)
                // Then a processing state should be created
                transactionId = TransactionId(accountId, "tx-state-1")
                processingStateOpt <- env.processingStateRepo.load(transactionId)
            yield
                assertTrue(
                    resultOpt.isDefined,
                    processingStateOpt.isDefined,
                    processingStateOpt.exists(_.status == TransactionStatus.Imported),
                    processingStateOpt.exists(ps => 
                        ps.transactionId == transactionId && 
                        !ps.isDuplicate && 
                        ps.suggestedCategory.isEmpty
                    )
                )
        },
        
        test("should import data with all transaction details") {
            // Verify all transaction fields are properly mapped from raw transaction
            for
                env <- MockFactory.createMockEnvironment
                service <- makeImportService(env)
                accountId = 1L
                // Given a raw transaction with complete details
                rawTransaction = RawTransaction(
                    externalId = "tx-full-1",
                    date = java.time.LocalDate.of(2024, 5, 15),
                    amount = 1299.99,
                    currency = "CZK",
                    counterAccount = Some("123456789"),
                    counterBankCode = Some("0800"),
                    counterBankName = Some("Test Bank"),
                    variableSymbol = Some("12345"),
                    constantSymbol = Some("0308"),
                    specificSymbol = Some("54321"),
                    userIdentification = Some("Payment for invoice #12345"),
                    message = Some("Invoice payment"),
                    transactionType = "TRANSFER",
                    comment = Some("Monthly service")
                )
                // When we import the transaction
                resultOpt <- service.importTransaction(rawTransaction, accountId)
                // Then all fields should be mapped correctly
                transactionId = TransactionId(accountId, "tx-full-1")
                savedTransactionOpt <- env.transactionRepo.load(transactionId)
            yield
                assertTrue(
                    resultOpt.isDefined,
                    savedTransactionOpt.isDefined,
                    savedTransactionOpt.exists(t =>
                        t.id == transactionId &&
                        t.date == java.time.LocalDate.of(2024, 5, 15) &&
                        t.amount == BigDecimal(1299.99) &&
                        t.currency == "CZK" &&
                        t.counterAccount == Some("123456789") &&
                        t.counterBankCode == Some("0800") &&
                        t.counterBankName == Some("Test Bank") &&
                        t.variableSymbol == Some("12345") &&
                        t.constantSymbol == Some("0308") &&
                        t.specificSymbol == Some("54321") &&
                        t.userIdentification == Some("Payment for invoice #12345") &&
                        t.message == Some("Invoice payment") &&
                        t.transactionType == "TRANSFER" &&
                        t.comment == Some("Monthly service")
                    )
                )
        }
    )
    
    // Tests for duplicate detection
    val duplicateDetectionTests = suite("Duplicate Detection")(
        test("should detect duplicate transactions") {
            for
                env <- MockFactory.createForScenario("duplicate-detection")
                service <- makeImportService(env)
                accountId = 2L
                rawTransaction = RawTransaction(
                    externalId = "tx-dup-1", // This ID already exists in the test repository
                    date = java.time.LocalDate.now(),
                    amount = 100.0,
                    currency = "CZK",
                    counterAccount = None,
                    counterBankCode = None,
                    counterBankName = None,
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = None,
                    message = Some("Duplicate Transaction"),
                    transactionType = "PAYMENT",
                    comment = None
                )
                // When we try to import a duplicate transaction
                isDuplicate <- service.checkForDuplicate(TransactionId(accountId, "tx-dup-1"))
                importResult <- service.importTransaction(rawTransaction, accountId)
            yield
                assertTrue(
                    isDuplicate,  // Should detect duplicate
                    importResult.isEmpty // Should not import duplicate transaction
                )
        },
        
        test("should report duplicate IDs when batch importing") {
            for
                env <- MockFactory.createForScenario("duplicate-detection")
                service <- makeImportService(env)
                accountId = 2L
                rawTransactions = Seq(
                    RawTransaction(
                        externalId = "tx-dup-1", // This ID already exists
                        date = java.time.LocalDate.now(),
                        amount = 100.0,
                        currency = "CZK",
                        counterAccount = None,
                        counterBankCode = None,
                        counterBankName = None,
                        variableSymbol = None,
                        constantSymbol = None,
                        specificSymbol = None,
                        userIdentification = None,
                        message = Some("Duplicate Transaction"),
                        transactionType = "PAYMENT",
                        comment = None
                    ),
                    RawTransaction(
                        externalId = "tx-new-1", // This is new
                        date = java.time.LocalDate.now(),
                        amount = 25.0,
                        currency = "CZK",
                        counterAccount = None,
                        counterBankCode = None,
                        counterBankName = None,
                        variableSymbol = None,
                        constantSymbol = None,
                        specificSymbol = None,
                        userIdentification = None,
                        message = Some("New Transaction"),
                        transactionType = "PAYMENT",
                        comment = None
                    )
                )
                // When we batch import with a mix of new and duplicate transactions
                result <- service.importTransactions(accountId, rawTransactions)
                (importCount, duplicates) = result
            yield
                assertTrue(
                    importCount == 1,  // Only one transaction should be imported
                    duplicates.size == 1,  // One duplicate detected
                    duplicates.contains("tx-dup-1")  // The duplicate ID is reported
                )
        }
    )
    
    // Tests for event publication
    val eventPublicationTests = suite("Event Publication")(
        test("should create and publish ImportCompleted event") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeImportService(env)
                accountId = 3L
                count = 5
                // When we create an import completed event
                event <- service.createImportCompletedEvent(accountId, count)
            yield
                assertTrue(
                    event.sourceAccountId == accountId,
                    event.count == count,
                    event.occurredAt != null
                )
        }
    )
    
    // Tests for error handling
    val errorHandlingTests = suite("Error Handling")(
        test("should handle empty transaction batch gracefully") {
            for
                env <- MockFactory.createMockEnvironment
                service <- makeImportService(env)
                // Create a valid source account
                accountId = 4L
                sourceAccount = SourceAccount(
                    id = accountId,
                    accountId = "test-account-4",
                    bankId = "TEST",
                    name = "Test Bank Account",
                    currency = "CZK"
                )
                _ <- env.sourceAccountRepo.save(accountId, sourceAccount)
                // When we import an empty batch
                result <- service.importTransactions(accountId, Seq.empty)
                (importCount, duplicates) = result
            yield
                assertTrue(
                    importCount == 0,
                    duplicates.isEmpty
                )
        }
    )
    
    // Helper to create the service under test with mock dependencies
    private def makeImportService(env: MockFactory.MockEnvironment): UIO[ImportService] = 
        // Create an implementation of ImportService with the mock repositories
        ZIO.succeed(
            new ImportServiceImpl(
                transactionRepository = env.transactionRepo,
                processingStateRepository = env.processingStateRepo,
                sourceAccountRepository = env.sourceAccountRepo,
                eventPublisher = event => ZIO.succeed(()) // No-op event publisher for tests
            )
        )
end ImportServiceSpec