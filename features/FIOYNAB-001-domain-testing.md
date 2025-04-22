---
status: draft
last_updated: 2025-04-23
version: "0.1"
tags:
  - workflow
  - bdd
  - testing
  - domain-testing
---

> [!info] Draft Document
> This document is an initial draft and may change significantly.

# Domain-Level Testing Plan: FIOYNAB-001

## Feature Reference
- **Related Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature Specification**: [FIOYNAB-001](./FIOYNAB-001.md)
- **Scenario Analysis**: [FIOYNAB-001-scenario-analysis](./FIOYNAB-001-scenario-analysis.md)
- **Domain Model**: [FIOYNAB-001-domain-model](./FIOYNAB-001-domain-model.md)
- **Gherkin Feature**: [FIOYNAB-001.feature](./FIOYNAB-001.feature)
- **Implementation Plan**: [FIOYNAB-001-implementation-plan](./FIOYNAB-001-implementation-plan.md)

## Overview

This document describes the approach for domain-level testing of all scenarios in the Fio Bank to YNAB Integration feature. Following our BDD-Driven UI-First approach, we will implement and test the domain layer with mock implementations before developing the UI.

## Domain Testing Approach

1. **Mock Implementation Strategy**:
   - Create in-memory repositories for all data storage
   - Implement mock external system ports (Fio Bank, YNAB, AI)
   - Use test data fixtures that cover all scenario conditions

2. **Scenario-Based Testing**:
   - Implement each Gherkin scenario as a domain-level test
   - Map Given/When/Then to test setup/execution/assertions
   - Test services in isolation with mocked dependencies

3. **Test Organization**:
   - Group tests by domain service
   - Use descriptive test names that reflect scenario steps
   - Maintain traceability between tests and scenarios

## Mock Implementations

### Repository Mocks

```scala
class InMemoryTransactionRepository extends TransactionRepository:
    private val storage = Ref.make(Map.empty[TransactionId, Transaction]).runSync
    
    override def findById(id: TransactionId): UIO[Option[Transaction]] =
        storage.get.map(_.get(id))
    
    override def findByIds(ids: Seq[TransactionId]): UIO[Seq[Transaction]] =
        storage.get.map(s => ids.flatMap(id => s.get(id)))
    
    override def findByFilters(
        filters: TransactionFilters,
        page: Int,
        pageSize: Int
    ): UIO[Page[Transaction]] =
        for
            allTransactions <- storage.get.map(_.values.toSeq)
            filtered = applyFilters(allTransactions, filters)
            paginated = paginate(filtered, page, pageSize)
        yield Page(paginated, filtered.size, Math.ceil(filtered.size / pageSize.toDouble).toInt, page)
    
    // ... implement other methods similarly
```

### External System Port Mocks

```scala
class MockFioBankPort extends FioBankPort:
    private val transactions = Map(
        "account1" -> Map(
            (LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 15)) -> 
                generateTestTransactions(10, "2025-04-01", "2025-04-15")
        )
    )
    
    override def authenticate(): IO[ApiError, Unit] =
        IO.succeed(())
    
    override def getTransactions(
        accountId: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): IO[ApiError, Seq[FioTransaction]] =
        transactions.get(accountId)
            .flatMap(_.get((fromDate, toDate)))
            .map(IO.succeed)
            .getOrElse(IO.fail(ApiError("No transactions found")))
```

```scala
class MockAiCategorizationPort extends AiCategorizationPort:
    private val categorySuggestions = Map(
        "Supermarket Purchase" -> Category("grocery", "Groceries", "ynab-grocery", None),
        "Restaurant Payment" -> Category("dining", "Dining Out", "ynab-dining", None),
        "Gas Station" -> Category("fuel", "Fuel", "ynab-fuel", None)
    )
    
    override def suggestCategory(
        transactionDetails: Map[String, String],
        availableCategories: Seq[Category]
    ): IO[ApiError, CategorySuggestion] =
        transactionDetails.get("description")
            .flatMap(description => 
                categorySuggestions.find { case (key, _) => 
                    description.contains(key)
                }.map { case (_, category) =>
                    CategorySuggestion(category, 0.85, s"Matched by description: ${description}")
                }
            )
            .map(IO.succeed)
            .getOrElse(IO.succeed(CategorySuggestion(
                availableCategories.head,
                0.3,
                "Low confidence fallback"
            )))
```

## Test Fixtures

### Transaction Test Data

```scala
def createTestTransaction(id: String, amount: BigDecimal, description: String, status: TransactionStatus): Transaction =
    Transaction(
        id = id,
        fioId = Some(s"fio-$id"),
        date = java.time.LocalDate.parse("2025-04-05"),
        amount = Money(amount, Currency.getInstance("CZK")),
        description = description,
        originalPayee = PayeeName(s"Original Payee for $description"),
        cleanedPayee = None,
        counterAccountNumber = Some("123456789/0100"),
        counterBankCode = Some("0100"),
        variableSymbol = Some("12345"),
        constantSymbol = None,
        specificSymbol = None,
        category = None,
        status = status,
        importBatchId = "batch-test-1",
        ynabId = None,
        fingerprint = TransactionFingerprint(s"fingerprint-$id"),
        submittedAt = None,
        createdAt = java.time.Instant.now(),
        updatedAt = None
    )

def createTestCategories(): Seq[Category] = Seq(
    Category("grocery", "Groceries", "ynab-grocery", None),
    Category("dining", "Dining Out", "ynab-dining", None),
    Category("transport", "Transportation", "ynab-transport", None),
    Category("fuel", "Fuel", "ynab-fuel", Some("transport")),
    Category("entertainment", "Entertainment", "ynab-entertainment", None)
)
```

## Scenario-Based Tests

### Scenario 1: Successfully import transactions from Fio Bank

```scala
test("Import service should successfully import transactions from Fio Bank") {
    // Given
    val fioBankPort = new MockFioBankPort()
    val transactionRepo = new InMemoryTransactionRepository()
    val importBatchRepo = new InMemoryImportBatchRepository()
    val importService = new ImportServiceImpl(
        fioBankPort = fioBankPort,
        transactionRepo = transactionRepo,
        importBatchRepo = importBatchRepo
    )
    
    val sourceAccount = SourceAccount("account1", "Test Account", "fio")
    val startDate = LocalDate.parse("2025-04-01")
    val endDate = LocalDate.parse("2025-04-15")
    
    // When
    val result = importService.importTransactions(
        sourceAccount,
        startDate,
        endDate
    ).runSync
    
    // Then
    assert(result.isRight)
    val importBatch = result.toOption.get
    assertEquals(importBatch.status, ImportBatchStatus.Completed)
    assertEquals(importBatch.transactionCount, 10)
    
    val savedTransactions = transactionRepo.findByImportBatch(importBatch.id).runSync
    assertEquals(savedTransactions.length, 10)
    assertEquals(savedTransactions.map(_.status).distinct, List(TransactionStatus.Imported))
}
```

### Scenario 2: AI categorization of imported transactions

```scala
test("Categorization service should assign categories to imported transactions") {
    // Given
    val aiPort = new MockAiCategorizationPort()
    val transactionRepo = new InMemoryTransactionRepository()
    val categoryRepo = new InMemoryCategoryRepository()
    
    // Seed with test data
    val testCategories = createTestCategories()
    testCategories.foreach(categoryRepo.save(_).runSync)
    
    val testTransactions = Seq(
        createTestTransaction("tx1", 100, "Supermarket Purchase", TransactionStatus.Imported),
        createTestTransaction("tx2", 250, "Restaurant Payment", TransactionStatus.Imported),
        createTestTransaction("tx3", 500, "Gas Station", TransactionStatus.Imported)
    )
    testTransactions.foreach(transactionRepo.save(_).runSync)
    
    val categorizationService = new CategorizationServiceImpl(
        aiPort = aiPort,
        transactionRepo = transactionRepo,
        categoryRepo = categoryRepo
    )
    
    // When
    val result = categorizationService.categorizeTransactions(
        testTransactions.map(_.id)
    ).runSync
    
    // Then
    assert(result.isRight)
    val suggestions = result.toOption.get
    assertEquals(suggestions.length, 3)
    
    // Verify all transactions have categories assigned
    val updatedTransactions = testTransactions.map { tx =>
        transactionRepo.findById(tx.id).runSync.get
    }
    
    // All should have categories
    assert(updatedTransactions.forall(_.category.isDefined))
    
    // All should be marked as categorized
    assertEquals(
        updatedTransactions.map(_.status).distinct,
        List(TransactionStatus.Categorized)
    )
    
    // Check specific categories
    val tx1 = updatedTransactions.find(_.id == "tx1").get
    assertEquals(tx1.category.get.name, "Groceries")
    
    val tx2 = updatedTransactions.find(_.id == "tx2").get
    assertEquals(tx2.category.get.name, "Dining Out")
    
    val tx3 = updatedTransactions.find(_.id == "tx3").get
    assertEquals(tx3.category.get.name, "Fuel")
}
```

### Scenario 3: Manual modification of transaction category

```scala
test("Transaction manager should allow manual category modification") {
    // Given
    val transactionRepo = new InMemoryTransactionRepository()
    val categoryRepo = new InMemoryCategoryRepository()
    val auditRepo = new InMemoryAuditRepository()
    
    // Seed test data
    val testCategories = createTestCategories()
    testCategories.foreach(categoryRepo.save(_).runSync)
    
    val transaction = createTestTransaction(
        "tx1",
        100,
        "Supermarket Purchase",
        TransactionStatus.Categorized
    ).copy(
        category = Some(testCategories.find(_.name == "Groceries").get)
    )
    transactionRepo.save(transaction).runSync
    
    val transactionManager = new TransactionManagerServiceImpl(
        transactionRepo = transactionRepo,
        categoryRepo = categoryRepo,
        auditRepo = auditRepo
    )
    
    // Get the "Dining Out" category
    val diningCategory = testCategories.find(_.name == "Dining Out").get
    
    // When
    val result = transactionManager.updateTransactionCategory(
        transaction.id,
        diningCategory.id
    ).runSync
    
    // Then
    assert(result.isRight)
    val updatedTx = result.toOption.get
    
    // Verify category was changed
    assertEquals(updatedTx.category.get.id, diningCategory.id)
    assertEquals(updatedTx.category.get.name, "Dining Out")
    
    // Verify audit log was created
    val auditEntries = auditRepo.findByTransactionId(transaction.id).runSync
    assertEquals(auditEntries.length, 1)
    assertEquals(auditEntries.head.fieldName, "category")
    assertEquals(auditEntries.head.oldValue, "Groceries")
    assertEquals(auditEntries.head.newValue, "Dining Out")
}
```

### Scenario 5: Submit transactions to YNAB

```scala
test("Submission service should submit transactions to YNAB") {
    // Given
    val ynabPort = new MockYnabPort()
    val transactionRepo = new InMemoryTransactionRepository()
    
    // Create test transactions with categories
    val testCategories = createTestCategories()
    val testTransactions = Seq(
        createTestTransaction("tx1", 100, "Supermarket Purchase", TransactionStatus.Categorized)
            .copy(category = Some(testCategories.find(_.name == "Groceries").get)),
        createTestTransaction("tx2", 250, "Restaurant Payment", TransactionStatus.Categorized)
            .copy(category = Some(testCategories.find(_.name == "Dining Out").get)),
        createTestTransaction("tx3", 500, "Gas Station", TransactionStatus.Categorized)
            .copy(category = Some(testCategories.find(_.name == "Fuel").get))
    )
    testTransactions.foreach(transactionRepo.save(_).runSync)
    
    val submissionService = new SubmissionServiceImpl(
        ynabPort = ynabPort,
        transactionRepo = transactionRepo
    )
    
    // When
    val result = submissionService.submitTransactionsToYnab(
        testTransactions.map(_.id),
        "test-budget",
        "test-account"
    ).runSync
    
    // Then
    assert(result.isRight)
    val submissionResult = result.toOption.get
    
    assertEquals(submissionResult.successCount, 3)
    assertEquals(submissionResult.failureCount, 0)
    
    // Verify all transactions were marked as submitted
    val updatedTransactions = testTransactions.map { tx =>
        transactionRepo.findById(tx.id).runSync.get
    }
    
    assertEquals(
        updatedTransactions.map(_.status).distinct,
        List(TransactionStatus.Submitted)
    )
    
    // Verify YNAB IDs were assigned
    assert(updatedTransactions.forall(_.ynabId.isDefined))
    assert(updatedTransactions.forall(_.submittedAt.isDefined))
}
```

### Scenario 7: Prevent duplicate submission of transactions

```scala
test("Submission service should prevent duplicate submission") {
    // Given
    val ynabPort = new MockYnabPort()
    val transactionRepo = new InMemoryTransactionRepository()
    
    // Create test transactions that were already submitted
    val testCategories = createTestCategories()
    val testTransactions = Seq(
        createTestTransaction("tx1", 100, "Supermarket Purchase", TransactionStatus.Submitted)
            .copy(
                category = Some(testCategories.find(_.name == "Groceries").get),
                ynabId = Some("ynab-tx1"),
                submittedAt = Some(java.time.Instant.now())
            )
    )
    testTransactions.foreach(transactionRepo.save(_).runSync)
    
    val submissionService = new SubmissionServiceImpl(
        ynabPort = ynabPort,
        transactionRepo = transactionRepo
    )
    
    // When
    val isDuplicate = submissionService.isDuplicate(testTransactions.head).runSync
    
    // Then
    assert(isDuplicate)
    
    // When trying to submit again
    val result = submissionService.submitTransactionsToYnab(
        testTransactions.map(_.id),
        "test-budget",
        "test-account"
    ).runSync
    
    // Then - result should have those marked as duplicates
    assert(result.isRight)
    val submissionResult = result.toOption.get
    
    assertEquals(submissionResult.successCount, 0)
    assertEquals(submissionResult.failureCount, 1)
    assertEquals(submissionResult.results.head.errorMessage.get, "Transaction already submitted")
}
```

## Complete Test Coverage Mapping

The following table maps each Gherkin scenario step to a corresponding test in the domain layer:

| Scenario | Scenario Step | Domain Test |
|----------|---------------|-------------|
| 1. Successfully import transactions | Given I am logged in as an administrator | Authentication handled at UI level |
| | And the system is connected to Fio Bank and YNAB APIs | MockFioBankPort.authenticate() |
| | When I initiate an import for the date range | importService.importTransactions() |
| | Then the system should connect to Fio Bank API | MockFioBankPort.getTransactions() verification |
| | And retrieve all transactions for the specified date range | Verify correct parameters passed to fioBankPort |
| | And store them in the database | transactionRepo verification after import |
| | And I should see "10" transactions | Count verification from ImportBatch |
| | And all transactions should have "Imported" status | Transaction status verification |
| 2. AI categorization | Given "10" transactions have been imported | Test fixture setup |
| | When the AI categorization process completes | categorizationService.categorizeTransactions() |
| | Then each transaction should have an assigned YNAB category | Verify all transactions have categories |
| | And the transaction status should update to "Categorized" | Verify status is updated |
| | And the categorization accuracy should be at least "80%" | Verify suggestion confidence levels |

*[Table continues for all scenarios]*

## Testing Environment Setup

### Test Configuration

```scala
// ZIO Test environment setup
val testLayer = ZLayer.make[
    TransactionManagerService & 
    ImportService & 
    CategorizationService & 
    SubmissionService & 
    ValidationService
](
    // Mock repositories
    InMemoryTransactionRepository.layer,
    InMemoryImportBatchRepository.layer,
    InMemoryCategoryRepository.layer,
    InMemoryPayeeCleanupRuleRepository.layer,
    InMemoryAuditRepository.layer,
    
    // Mock external ports
    MockFioBankPort.layer,
    MockYnabPort.layer,
    MockAiCategorizationPort.layer,
    MockAiPayeeCleanupPort.layer,
    
    // Service implementations
    TransactionManagerServiceImpl.layer,
    ImportServiceImpl.layer,
    CategorizationServiceImpl.layer,
    SubmissionServiceImpl.layer,
    ValidationServiceImpl.layer
)
```

## Test Execution Plan

1. **Domain Entity Tests**:
   - Test all domain entities and value objects
   - Verify business rules and invariants
   - Test validation methods

2. **Domain Service Tests**:
   - Implement tests for each service interface
   - Cover all methods and edge cases
   - Test error handling and validation

3. **Scenario-Based Integration Tests**:
   - Test complete domain service interactions
   - Verify full scenario flows
   - Test boundary conditions and error scenarios

## Implementation Strategy

1. Start with basic domain entity and value object tests
2. Implement repository mock implementations
3. Develop external port mocks
4. Implement domain service tests for each scenario
5. Finally, implement complete scenario-based integration tests

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2025-04-23 | Initial draft | Dev Team |