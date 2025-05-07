# Task: Implement Domain-Level Tests for Transaction Import

## Context

We have implemented the UI components for transaction import and defined the domain entities and services. Now we need to create comprehensive tests for the domain logic to ensure it behaves as expected according to our scenarios.

The tests should focus on the domain model behavior, particularly:
1. The `TransactionImportDomainService` implementation
2. The repository interfaces and their interaction
3. Error handling and validation logic

We'll use ZIO Test to create scenario-driven tests that map to our Gherkin scenarios and validate our domain logic.

## Reference Information

### Related Files
- `/features/BUDGET-001/VS001/tasks/12-domain-model.md` - Domain model implementation task
- `/features/BUDGET-001/VS001/tasks/13-transaction-import-service.md` - Service implementation task
- `/features/BUDGET-001/VS001/scenarios/transaction_import.feature` - Gherkin scenarios
- `/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/TransactionImportService.scala` - UI Service interface
- `/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/MockTransactionImportService.scala` - Mock UI implementation

### ZIO Test Patterns
- Use ZIOSpecDefault for test specs
- Organize tests into suites
- Use ZIO environments for dependency injection
- Create mock implementations of services and repositories
- Test both success and error scenarios
- Map tests to Gherkin scenarios with appropriate naming

## Implementation Requirements

### Package Structure
```
works.iterative.incubator.budget
  └── domain
      └── model
      │   └── (implemented transaction domain entities)
      └── service
      │   └── TransactionImportDomainService.scala (to test)
      └── repository
      │   └── (repository interfaces to mock)
      └── test
          └── TransactionImportDomainServiceSpec.scala (domain service tests)
          └── ImportBatchSpec.scala (entity behavior tests)
          └── TransactionSpec.scala (entity behavior tests)
          └── mock
              └── MockTransactionRepository.scala (test mocks)
              └── MockFioBankService.scala (test mocks)
              └── TestEnvironment.scala (unified test environment)
```

### Test Files to Create

1. **TransactionImportDomainServiceSpec.scala**
   - Tests for the `TransactionImportDomainService` implementation
   - Covers validation, import workflows, and error handling
   - Maps to scenarios in the feature file

2. **ImportBatchSpec.scala**
   - Tests for the `ImportBatch` entity behavior
   - Validates entity invariants and business rules

3. **TransactionSpec.scala**
   - Tests for the `Transaction` entity behavior
   - Validates entity invariants and business rules

4. **Mock implementations**
   - `MockTransactionRepository.scala` - In-memory repository implementation
   - `MockFioBankService.scala` - Mock implementation of the bank API service
   - `TestEnvironment.scala` - Unified test environment setup

### Test Scenarios to Cover

Based on the Gherkin scenarios, implement tests for:

1. **Date range validation**
   - Valid date ranges
   - Invalid date ranges (start after end)
   - Future dates
   - Range exceeding 90 days

2. **Transaction import process**
   - Successful import with multiple transactions
   - Import with no transactions found
   - Bank API unavailable/connection errors
   - Repository errors

3. **Transaction de-duplication**
   - Importing transactions that already exist
   - Partial duplicate detection

4. **Error handling**
   - Domain-specific errors are properly propagated
   - Error recovery strategies
   - Error message clarity

## Implementation Structure

### Test Environment Setup

Create a unified test environment with mocks:

```scala
object TestEnvironment:
  case class Env(
    transactionImportService: TransactionImportDomainService,
    transactionRepository: MockTransactionRepository,
    fioBankService: MockFioBankService,
    idGenerator: MockIdGenerator
  )

  def setup: UIO[Env] = 
    for
      transactionRepo <- MockTransactionRepository.make
      fioBankService <- MockFioBankService.make
      idGenerator <- MockIdGenerator.make
      service = TransactionImportDomainServiceLive(
        transactionRepo,
        fioBankService,
        idGenerator
      )
    yield Env(service, transactionRepo, fioBankService, idGenerator)

  val layer: ULayer[TransactionImportDomainService] =
    ZLayer.fromZIO(
      for
        transactionRepo <- MockTransactionRepository.make.toLayer
        fioBankService <- MockFioBankService.make.toLayer
        idGenerator <- MockIdGenerator.make.toLayer
      yield TransactionImportDomainServiceLive(
        transactionRepo,
        fioBankService, 
        idGenerator
      )
    )
```

### Domain Service Tests

```scala
object TransactionImportDomainServiceSpec extends ZIOSpecDefault:
  def spec = suite("TransactionImportDomainService")(
    // Date validation tests
    suite("validateDateRange")(
      test("should accept valid date range") {
        // Given a valid date range
        val startDate = LocalDate.of(2025, 4, 1)
        val endDate = LocalDate.of(2025, 4, 15)

        // When validating the date range
        for
          env <- TestEnvironment.setup
          result <- env.transactionImportService.validateDateRange(startDate, endDate).exit
        yield
          // Then it should succeed
          assertTrue(result.isSuccess)
      },

      test("should reject date range with start date after end date") {
        // Given an invalid date range with start after end
        val startDate = LocalDate.of(2025, 4, 15)
        val endDate = LocalDate.of(2025, 4, 1)

        // When validating the date range
        for
          env <- TestEnvironment.setup
          result <- env.transactionImportService.validateDateRange(startDate, endDate).exit
        yield
          // Then it should fail with specific error
          assert(result)(fails(hasField("message", _.message, containsString("after end date"))))
      }
      // More validation tests...
    ),

    // Import process tests
    suite("importTransactions")(
      test("should successfully import transactions for valid date range") {
        // Given a valid date range and bank data
        val startDate = LocalDate.of(2025, 4, 1)
        val endDate = LocalDate.of(2025, 4, 15)
        val transactions = TestData.createTransactions(10)

        // When importing transactions
        for
          env <- TestEnvironment.setup
          _ <- env.fioBankService.setTransactionsForDateRange(startDate, endDate, transactions)
          importBatch <- env.transactionImportService.importTransactions(startDate, endDate)
          savedTransactions <- env.transactionRepository.findAll
        yield
          // Then an import batch should be created and transactions saved
          assertTrue(
            importBatch.transactions.size == 10,
            importBatch.status == ImportBatchStatus.Completed,
            savedTransactions.size == 10
          )
      },
      
      // Test the no transactions scenario
      test("should handle scenario with no transactions found") {
        // Given a date range with no transactions
        val startDate = LocalDate.of(2025, 6, 1)
        val endDate = LocalDate.of(2025, 6, 2)

        // When importing transactions
        for
          env <- TestEnvironment.setup
          _ <- env.fioBankService.setTransactionsForDateRange(startDate, endDate, Seq.empty)
          importBatch <- env.transactionImportService.importTransactions(startDate, endDate)
        yield
          // Then an empty import batch should be created
          assertTrue(
            importBatch.transactions.isEmpty,
            importBatch.status == ImportBatchStatus.Completed,
            importBatch.isEmptyImport
          )
      }
      // More import process tests...
    ),

    // Error handling tests
    suite("error handling")(
      test("should handle bank API connection failures") {
        // Given a valid date range but unavailable bank API
        val startDate = LocalDate.of(2025, 4, 1)
        val endDate = LocalDate.of(2025, 4, 15)

        // When importing transactions
        for
          env <- TestEnvironment.setup
          _ <- env.fioBankService.simulateConnectionError
          result <- env.transactionImportService.importTransactions(startDate, endDate).exit
        yield
          // Then it should fail with a connection error
          assert(result)(fails(isSubtype[ImportError.ConnectionFailure](anything)))
      }
      // More error handling tests...
    ),
    
    // Deduplication tests
    suite("deduplication")(
      test("should identify and skip duplicate transactions") {
        // Given existing transactions and new imports with overlap
        val startDate = LocalDate.of(2025, 4, 1)
        val endDate = LocalDate.of(2025, 4, 15)
        val existingTransactions = TestData.createTransactions(10)
        val newTransactions = existingTransactions.take(5) ++ TestData.createTransactions(5)

        // When importing partially duplicate transactions
        for
          env <- TestEnvironment.setup
          _ <- env.transactionRepository.saveAll(existingTransactions)
          _ <- env.fioBankService.setTransactionsForDateRange(startDate, endDate, newTransactions)
          importBatch <- env.transactionImportService.importTransactions(startDate, endDate)
          allTransactions <- env.transactionRepository.findAll
        yield
          // Then only non-duplicate transactions should be added
          assertTrue(
            importBatch.transactions.size == 5, // Only the new ones
            importBatch.duplicatesSkipped == 5, // The duplicates were detected
            allTransactions.size == 15 // 10 original + 5 new
          )
      }
      // More deduplication tests...
    )
  )
```

### Entity Behavior Tests

```scala
object ImportBatchSpec extends ZIOSpecDefault:
  def spec = suite("ImportBatch")(
    test("should calculate correct statistics") {
      // Given transactions with various statuses
      val successfulTxs = TestData.createTransactions(5)
      val failedTxs = List.fill(3)(TestData.createFailedTransaction)
      
      // When creating an import batch
      val importBatch = ImportBatch(
        id = ImportBatchId("test-batch"),
        startDate = LocalDate.of(2025, 4, 1),
        endDate = LocalDate.of(2025, 4, 15),
        timestamp = Instant.now(),
        transactions = successfulTxs,
        failedTransactions = failedTxs,
        status = ImportBatchStatus.Completed
      )
      
      // Then statistics should be calculated correctly
      assertTrue(
        importBatch.successCount == 5,
        importBatch.failureCount == 3,
        importBatch.totalCount == 8,
        importBatch.isSuccess,
        !importBatch.isEmptyImport
      )
    }
    // More entity behavior tests...
  )
```

### Mock Implementations

```scala
case class MockTransactionRepository(
  data: Ref[Map[TransactionId, Transaction]],
  errors: Ref[Set[TransactionId]]
) extends TransactionRepository:
  // Implement repository methods with in-memory storage
  def findById(id: TransactionId): IO[RepositoryError, Option[Transaction]] =
    errors.get.flatMap { errorIds =>
      if errorIds.contains(id) then
        ZIO.fail(RepositoryError.DatabaseFailure("Simulated error"))
      else
        data.get.map(_.get(id))
    }

  def save(transaction: Transaction): IO[RepositoryError, Transaction] =
    data.update(_ + (transaction.id -> transaction))
      .as(transaction)
  
  // Add test helper methods
  def findAll: UIO[Seq[Transaction]] =
    data.get.map(_.values.toSeq)
    
  def saveAll(transactions: Seq[Transaction]): UIO[Unit] =
    data.update(_ ++ transactions.map(tx => tx.id -> tx))
    
  def reset: UIO[Unit] =
    data.set(Map.empty) *> errors.set(Set.empty)
    
  // More implementation...

object MockTransactionRepository:
  def make: UIO[MockTransactionRepository] =
    for
      dataRef <- Ref.make(Map.empty[TransactionId, Transaction])
      errorRef <- Ref.make(Set.empty[TransactionId])
    yield MockTransactionRepository(dataRef, errorRef)
```

```scala
case class MockFioBankService(
  dataByDateRange: Ref[Map[(LocalDate, LocalDate), Seq[Transaction]]],
  shouldFail: Ref[Boolean]
) extends FioBankService:
  // Implement bank service methods
  def fetchTransactions(startDate: LocalDate, endDate: LocalDate): IO[BankApiError, Seq[Transaction]] =
    shouldFail.get.flatMap { fail =>
      if fail then
        ZIO.fail(BankApiError.ConnectionFailure("Simulated connection error"))
      else
        dataByDateRange.get.map(_.getOrElse((startDate, endDate), Seq.empty))
    }
    
  // Add test helper methods
  def setTransactionsForDateRange(startDate: LocalDate, endDate: LocalDate, transactions: Seq[Transaction]): UIO[Unit] =
    dataByDateRange.update(_ + ((startDate, endDate) -> transactions))
    
  def simulateConnectionError: UIO[Unit] =
    shouldFail.set(true)
    
  def reset: UIO[Unit] =
    dataByDateRange.set(Map.empty) *> shouldFail.set(false)
    
  // More implementation...

object MockFioBankService:
  def make: UIO[MockFioBankService] =
    for
      dataRef <- Ref.make(Map.empty[(LocalDate, LocalDate), Seq[Transaction]])
      shouldFailRef <- Ref.make(false)
    yield MockFioBankService(dataRef, shouldFailRef)
```

## Test Data Helpers

```scala
object TestData:
  def createTransaction(id: String = UUID.randomUUID().toString): Transaction =
    Transaction(
      id = TransactionId(id),
      accountId = AccountId("test-account"),
      date = LocalDate.of(2025, 4, 1).plusDays(Random.nextInt(15)),
      amount = Money(BigDecimal(Random.nextDouble() * 1000).setScale(2, RoundingMode.HALF_EVEN)),
      description = s"Test Transaction $id",
      counterparty = Some(s"Test Counterparty $id"),
      reference = Some(s"REF$id"),
      importBatchId = ImportBatchId("test-batch"),
      status = TransactionStatus.Imported
    )
    
  def createTransactions(count: Int): Seq[Transaction] =
    (1 to count).map(i => createTransaction(s"tx-$i"))
    
  def createFailedTransaction: Transaction =
    createTransaction().copy(status = TransactionStatus.Failed)
```

## Constraints and Considerations

1. **Test isolation**: Each test should run independently with no shared state.
2. **Mapping to scenarios**: Tests should clearly map to the Gherkin scenarios.
3. **Thread safety**: Use ZIO Ref for concurrency-safe state management in mocks.
4. **Error handling**: Test both success cases and failure scenarios.
5. **Realistic behavior**: Mock implementations should behave consistently with real implementations.

## Completion Criteria

The task is considered complete when:

1. All test files are implemented and pass successfully
2. Tests provide good coverage of domain logic and scenarios
3. Mock implementations simulate realistic behavior
4. Documentation is clear and comprehensive
5. Code follows project style guidelines and best practices

## Additional Resources

- [ZIO Test Implementation Guide](/ai-context/architecture/guides/zio_test_implementation_guide.md)
- [ZIO Test Guide](/ai-context/workflows/zio-test-guide.md)
- [Mock Implementation Guide](/ai-context/architecture/guides/mock_implementation_guide.md)