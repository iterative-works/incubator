package works.iterative.incubator.budget.domain.service.impl

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.time.{LocalDate, Instant}
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.event.*
import works.iterative.incubator.budget.domain.service.{ImportService, RawTransaction}

object ImportServiceImplSpec extends ZIOSpecDefault:
    // Mock repositories
    class MockTransactionRepository extends TransactionRepository:
        private var transactions = Map.empty[TransactionId, Transaction]
        
        def save(key: TransactionId, value: Transaction): UIO[Unit] =
            ZIO.succeed { transactions = transactions + (key -> value) }
            
        def findById(id: TransactionId): UIO[Option[Transaction]] =
            ZIO.succeed(transactions.get(id))
            
        def find[Q](query: Q): UIO[Seq[Transaction]] = 
            ZIO.succeed(transactions.values.toSeq)
            
        // Test helper
        def getAll: UIO[Map[TransactionId, Transaction]] = ZIO.succeed(transactions)
    end MockTransactionRepository
    
    class MockProcessingStateRepository extends TransactionProcessingStateRepository:
        private var states = Map.empty[TransactionId, TransactionProcessingState]
        
        def find(query: TransactionProcessingStateQuery): UIO[Seq[TransactionProcessingState]] =
            ZIO.succeed {
                states.values.filter { state =>
                    query.sourceAccountId.forall(_ == state.transactionId.sourceAccountId) &&
                    query.status.forall(_ == state.status)
                }.toSeq
            }
            
        def save(key: TransactionId, value: TransactionProcessingState): UIO[Unit] =
            ZIO.succeed { states = states + (key -> value) }
            
        def load(id: TransactionId): UIO[Option[TransactionProcessingState]] =
            ZIO.succeed(states.get(id))
            
        def findReadyToSubmit(): UIO[Seq[TransactionProcessingState]] =
            ZIO.succeed(states.values.filter(_.isReadyForSubmission).toSeq)
            
        // Test helper
        def getAll: UIO[Map[TransactionId, TransactionProcessingState]] = ZIO.succeed(states)
    end MockProcessingStateRepository
    
    class MockSourceAccountRepository extends SourceAccountRepository:
        private var accounts = Map(
            1L -> SourceAccount(
                id = 1L,
                name = "Test Account",
                bankId = "test-bank",
                accountNumber = "123456789",
                iban = Some("CZ1234567890123456789012"),
                currency = "CZK",
                active = true
            )
        )
        
        def save(key: Long, value: SourceAccount): UIO[Unit] =
            ZIO.succeed { accounts = accounts + (key -> value) }
            
        def findById(id: Long): UIO[Option[SourceAccount]] =
            ZIO.succeed(accounts.get(id))
            
        def find[Q](query: Q): UIO[Seq[SourceAccount]] =
            ZIO.succeed(accounts.values.toSeq)
    end MockSourceAccountRepository
    
    // Event collector for testing
    class EventCollector:
        private var events = List.empty[DomainEvent]
        
        def publishEvent(event: DomainEvent): UIO[Unit] =
            ZIO.succeed { events = event :: events }
            
        def getEvents: UIO[List[DomainEvent]] = ZIO.succeed(events)
        
        def clear: UIO[Unit] = ZIO.succeed { events = List.empty }
    end EventCollector
    
    // Sample data for testing
    private val sampleRawTransactions = Seq(
        RawTransaction(
            externalId = "tx-1",
            date = LocalDate.of(2023, 1, 15),
            amount = BigDecimal("100.50"),
            currency = "CZK",
            counterAccount = Some("123456789"),
            counterBankCode = Some("0800"),
            counterBankName = Some("Česká spořitelna"),
            variableSymbol = Some("12345"),
            constantSymbol = None,
            specificSymbol = None,
            userIdentification = Some("John Doe"),
            message = Some("Payment for services"),
            transactionType = "PAYMENT",
            comment = None
        ),
        RawTransaction(
            externalId = "tx-2",
            date = LocalDate.of(2023, 1, 16),
            amount = BigDecimal("250.75"),
            currency = "CZK",
            counterAccount = Some("987654321"),
            counterBankCode = Some("0300"),
            counterBankName = Some("ČSOB"),
            variableSymbol = Some("54321"),
            constantSymbol = None,
            specificSymbol = None,
            userIdentification = Some("Jane Smith"),
            message = Some("Invoice payment"),
            transactionType = "PAYMENT",
            comment = None
        )
    )
    
    // Setup test environment
    def testEnvironment = for
        txRepo <- ZIO.succeed(new MockTransactionRepository)
        stateRepo <- ZIO.succeed(new MockProcessingStateRepository)
        accountRepo <- ZIO.succeed(new MockSourceAccountRepository)
        eventCollector <- ZIO.succeed(new EventCollector)
        service = ImportServiceImpl(txRepo, stateRepo, accountRepo, eventCollector.publishEvent)
    yield (service, txRepo, stateRepo, accountRepo, eventCollector)

    // Tests
    override def spec =
        suite("ImportService Implementation")(
            test("importTransactions should create transaction entities with Imported status") {
                for
                    env <- testEnvironment
                    (service, txRepo, stateRepo, _, eventCollector) = env
                    sourceAccountId = 1L
                    result <- service.importTransactions(sourceAccountId, sampleRawTransactions)
                    (importedCount, duplicateIds) = result
                    transactions <- txRepo.getAll
                    processingStates <- stateRepo.getAll
                yield
                    assertTrue(
                        importedCount == 2,
                        duplicateIds.isEmpty,
                        transactions.size == 2,
                        processingStates.size == 2,
                        processingStates.values.forall(_.status == TransactionStatus.Imported)
                    )
            },
            
            test("importTransactions should detect duplicate transactions") {
                for
                    env <- testEnvironment
                    (service, txRepo, _, _, eventCollector) = env
                    sourceAccountId = 1L
                    // First import
                    _ <- service.importTransactions(sourceAccountId, sampleRawTransactions)
                    txCount1 <- txRepo.getAll.map(_.size)
                    // Second import of the same transactions
                    result <- service.importTransactions(sourceAccountId, sampleRawTransactions)
                    (importedCount, duplicateIds) = result
                    txCount2 <- txRepo.getAll.map(_.size)
                    events <- eventCollector.getEvents
                    duplicateEvents = events.collect { case e: DuplicateTransactionDetected => e }
                yield
                    assertTrue(
                        txCount1 == 2,
                        importedCount == 0,
                        duplicateIds.length == 2,
                        txCount2 == 2, // No new transactions added
                        duplicateEvents.size == 2 // Two duplicate events published
                    )
            },
            
            test("ImportCompleted event should be published with correct count") {
                for
                    env <- testEnvironment
                    (service, _, _, _, eventCollector) = env
                    _ <- eventCollector.clear
                    sourceAccountId = 1L
                    _ <- service.importTransactions(sourceAccountId, sampleRawTransactions)
                    events <- eventCollector.getEvents
                    importCompletedEvents = events.collect { case e: ImportCompleted => e }
                yield
                    assertTrue(
                        importCompletedEvents.nonEmpty,
                        importCompletedEvents.head.count == 2,
                        importCompletedEvents.head.sourceAccountId == sourceAccountId
                    )
            },
            
            test("TransactionImported events should be published for each transaction") {
                for
                    env <- testEnvironment
                    (service, _, _, _, eventCollector) = env
                    _ <- eventCollector.clear
                    sourceAccountId = 1L
                    _ <- service.importTransactions(sourceAccountId, sampleRawTransactions)
                    events <- eventCollector.getEvents
                    importEvents = events.collect { case e: TransactionImported => e }
                yield
                    assertTrue(
                        importEvents.length == 2,
                        importEvents.forall(_.sourceAccountId == sourceAccountId)
                    )
            },
            
            test("importTransaction should fail gracefully with invalid source account") {
                for
                    env <- testEnvironment
                    (service, _, _, _, _) = env
                    invalidSourceId = 999L
                    result <- service.importTransactions(invalidSourceId, sampleRawTransactions).exit
                yield
                    assertTrue(result.isFailure)
            }
        ) @@ sequential
    end spec
end ImportServiceImplSpec