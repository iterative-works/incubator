---
status: draft
last_updated: 2025-04-23
version: "0.1"
tags:
  - workflow
  - bdd
  - domain-model
  - functional-core
---

> [!info] Draft Document
> This document is an initial draft and may change significantly.

# Domain Model Design: FIOYNAB-001

## Feature Reference
- **Related Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature Specification**: [FIOYNAB-001](./FIOYNAB-001.md)
- **Scenario Analysis**: [FIOYNAB-001-scenario-analysis](./FIOYNAB-001-scenario-analysis.md)
- **Gherkin Feature**: [FIOYNAB-001.feature](./FIOYNAB-001.feature)

## Domain Model Overview

This document defines the complete domain model for the Fio Bank to YNAB Integration feature, following our Functional Core/Imperative Shell architectural pattern. The domain model is designed to support all identified scenarios from the Gherkin feature file.

## Architectural Approach

Following our BDD-Driven development approach with Functional Core architecture:

1. **Pure Domain Model**: All domain logic is implemented as pure functions without side effects
2. **Clear Interface Boundaries**: Domain services expose interfaces for external components
3. **Repository Abstractions**: All data access is through repository interfaces
4. **Port Definitions**: External system interactions defined as ports
5. **Immutable Entities**: All domain entities are immutable

## Domain Entities and Value Objects

### Transaction

```scala
case class Transaction(
    id: TransactionId,
    fioId: Option[String],
    date: java.time.LocalDate,
    amount: Money,
    description: String,
    originalPayee: PayeeName,
    cleanedPayee: Option[PayeeName],
    counterAccountNumber: Option[String],
    counterBankCode: Option[String],
    variableSymbol: Option[String],
    constantSymbol: Option[String],
    specificSymbol: Option[String],
    category: Option[Category],
    status: TransactionStatus,
    importBatchId: ImportBatchId,
    ynabId: Option[String],
    fingerprint: TransactionFingerprint,
    submittedAt: Option[java.time.Instant],
    createdAt: java.time.Instant,
    updatedAt: Option[java.time.Instant]
)
```

### ImportBatch

```scala
case class ImportBatch(
    id: ImportBatchId,
    dateRangeStart: java.time.LocalDate,
    dateRangeEnd: java.time.LocalDate,
    status: ImportBatchStatus,
    transactionCount: Int,
    sourceAccount: SourceAccount,
    createdAt: java.time.Instant,
    completedAt: Option[java.time.Instant]
)
```

### Category

```scala
case class Category(
    id: CategoryId,
    name: String,
    ynabId: String,
    parentId: Option[CategoryId]
)
```

### Value Objects

```scala
case class Money(amount: BigDecimal, currency: Currency)

opaque type TransactionId = String
opaque type ImportBatchId = String
opaque type CategoryId = String

case class PayeeName(value: String)

case class TransactionFingerprint(value: String)

enum TransactionStatus:
    case Imported, Categorized, Submitted, Failed

enum ImportBatchStatus:
    case InProgress, Completed, Failed

case class ValidationError(code: String, message: String)

case class CategorySuggestion(
    category: Category,
    confidence: Double,
    reasoning: String
)

case class SubmissionResult(
    transactionId: TransactionId,
    ynabId: Option[String],
    success: Boolean,
    errorMessage: Option[String]
)

case class BulkSubmissionResult(
    results: Seq[SubmissionResult],
    successCount: Int,
    failureCount: Int
)

case class PayeeCleanupResult(
    originalPayee: PayeeName,
    cleanedPayee: PayeeName,
    confidence: Double,
    rule: Option[PayeeCleanupRule]
)
```

## Domain Service Interfaces

### Import Service

```scala
trait ImportService:
    def importTransactions(
        sourceAccount: SourceAccount,
        dateRangeStart: java.time.LocalDate,
        dateRangeEnd: java.time.LocalDate
    ): IO[ValidationError, ImportBatch]
    
    def getImportStatus(importBatchId: ImportBatchId): UIO[Option[ImportBatch]]
```

### Transaction Manager Service

```scala
trait TransactionManagerService:
    def getTransactions(
        filters: TransactionFilters,
        page: Int,
        pageSize: Int
    ): UIO[Page[Transaction]]
    
    def getTransactionById(id: TransactionId): UIO[Option[Transaction]]
    
    def updateTransactionCategory(
        id: TransactionId,
        categoryId: CategoryId
    ): IO[ValidationError, Transaction]
    
    def updateTransactionsCategory(
        ids: Seq[TransactionId],
        categoryId: CategoryId
    ): IO[ValidationError, Seq[Transaction]]
```

### Categorization Service

```scala
trait CategorizationService:
    def categorizeTransactions(
        transactionIds: Seq[TransactionId]
    ): IO[ValidationError, Seq[CategorySuggestion]]
    
    def getSuggestedCategory(
        transaction: Transaction
    ): IO[ValidationError, CategorySuggestion]
    
    def getCategoriesForBudget(budgetId: String): UIO[Seq[Category]]
```

### Payee Cleanup Service

```scala
trait PayeeCleanupService:
    def cleanupPayee(
        originalPayee: PayeeName,
        transactionContext: TransactionContext
    ): UIO[PayeeCleanupResult]
    
    def getPendingRules(): UIO[Seq[PayeeCleanupRule]]
    
    def getApprovedRules(): UIO[Seq[PayeeCleanupRule]]
    
    def approveRule(
        ruleId: String,
        modifications: Option[Map[String, String]] = None
    ): IO[ValidationError, PayeeCleanupRule]
    
    def rejectRule(
        ruleId: String,
        reason: Option[String] = None
    ): IO[ValidationError, Unit]
    
    def provideFeedback(
        ruleId: String,
        wasSuccessful: Boolean
    ): UIO[Unit]
```

### Submission Service

```scala
trait SubmissionService:
    def submitTransactionsToYnab(
        transactionIds: Seq[TransactionId],
        budgetId: String,
        accountId: String
    ): IO[ValidationError, BulkSubmissionResult]
    
    def isDuplicate(transaction: Transaction): UIO[Boolean]
```

### Validation Service

```scala
trait ValidationService:
    def validateDateRange(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): Either[ValidationError, Unit]
    
    def validateTransaction(transaction: Transaction): Either[ValidationError, Unit]
```

## Repository Interfaces

### Transaction Repository

```scala
trait TransactionRepository:
    def findById(id: TransactionId): UIO[Option[Transaction]]
    
    def findByIds(ids: Seq[TransactionId]): UIO[Seq[Transaction]]
    
    def findByFilters(
        filters: TransactionFilters,
        page: Int,
        pageSize: Int
    ): UIO[Page[Transaction]]
    
    def save(transaction: Transaction): UIO[Transaction]
    
    def saveAll(transactions: Seq[Transaction]): UIO[Seq[Transaction]]
    
    def updateStatus(
        id: TransactionId,
        status: TransactionStatus
    ): UIO[Option[Transaction]]
    
    def updateCategory(
        id: TransactionId,
        categoryId: CategoryId
    ): UIO[Option[Transaction]]
    
    def updateCategories(
        ids: Seq[TransactionId],
        categoryId: CategoryId
    ): UIO[Seq[Transaction]]
    
    def markAsSubmitted(
        ids: Seq[TransactionId],
        ynabIds: Map[TransactionId, String]
    ): UIO[Seq[Transaction]]
    
    def findSubmitted(fingerprints: Seq[TransactionFingerprint]): UIO[Seq[Transaction]]
```

### ImportBatch Repository

```scala
trait ImportBatchRepository:
    def save(importBatch: ImportBatch): UIO[ImportBatch]
    
    def findById(id: ImportBatchId): UIO[Option[ImportBatch]]
    
    def findByDateRange(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): UIO[Seq[ImportBatch]]
    
    def updateStatus(
        id: ImportBatchId,
        status: ImportBatchStatus
    ): UIO[Option[ImportBatch]]
```

### Category Repository

```scala
trait CategoryRepository:
    def findById(id: CategoryId): UIO[Option[Category]]
    
    def findAll(): UIO[Seq[Category]]
    
    def findByBudgetId(budgetId: String): UIO[Seq[Category]]
    
    def save(category: Category): UIO[Category]
    
    def saveAll(categories: Seq[Category]): UIO[Seq[Category]]
```

### PayeeCleanupRule Repository

```scala
trait PayeeCleanupRuleRepository:
    def findById(id: String): UIO[Option[PayeeCleanupRule]]
    
    def findByStatus(status: RuleStatus): UIO[Seq[PayeeCleanupRule]]
    
    def save(rule: PayeeCleanupRule): UIO[PayeeCleanupRule]
    
    def updateStatus(id: String, status: RuleStatus): UIO[Option[PayeeCleanupRule]]
    
    def updateFeedback(
        id: String,
        wasSuccessful: Boolean
    ): UIO[Option[PayeeCleanupRule]]
```

## Port Definitions (External System Interfaces)

### Fio Bank API Port

```scala
trait FioBankPort:
    def authenticate(): IO[ApiError, Unit]
    
    def getTransactions(
        accountId: String,
        fromDate: java.time.LocalDate,
        toDate: java.time.LocalDate
    ): IO[ApiError, Seq[FioTransaction]]
```

### YNAB API Port

```scala
trait YnabPort:
    def getBudgets(): IO[ApiError, Seq[YnabBudget]]
    
    def getAccounts(budgetId: String): IO[ApiError, Seq[YnabAccount]]
    
    def getCategories(budgetId: String): IO[ApiError, Seq[YnabCategory]]
    
    def createTransactions(
        budgetId: String,
        accountId: String,
        transactions: Seq[YnabTransaction]
    ): IO[ApiError, YnabTransactionResponse]
```

### AI Categorization Port

```scala
trait AiCategorizationPort:
    def suggestCategory(
        transactionDetails: Map[String, String],
        availableCategories: Seq[Category]
    ): IO[ApiError, CategorySuggestion]
```

### AI Payee Cleanup Port

```scala
trait AiPayeeCleanupPort:
    def cleanupPayee(
        payee: PayeeName,
        context: Map[String, String]
    ): IO[ApiError, PayeeCleanupResult]
    
    def generateRule(
        originalPayee: PayeeName,
        cleanedPayee: PayeeName,
        examples: Seq[(PayeeName, PayeeName)]
    ): IO[ApiError, PayeeCleanupRule]
```

## Application Service Interfaces

### TransactionImport Application Service

```scala
trait TransactionImportApplicationService:
    def startImport(
        sourceAccountId: String,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): IO[ApiError, ImportBatch]
    
    def getImportStatus(importBatchId: String): UIO[Option[ImportBatchStatusView]]
```

### TransactionManagement Application Service

```scala
trait TransactionManagementApplicationService:
    def getTransactions(
        filters: TransactionFiltersView,
        page: Int,
        pageSize: Int
    ): UIO[PageView[TransactionView]]
    
    def getTransactionDetails(id: String): UIO[Option[TransactionDetailView]]
    
    def updateCategory(
        id: String,
        categoryId: String
    ): IO[ApiError, TransactionView]
    
    def updateCategoryBulk(
        ids: Seq[String],
        categoryId: String
    ): IO[ApiError, BulkUpdateResultView]
```

### Categorization Application Service

```scala
trait CategorizationApplicationService:
    def startAutoCategorization(
        transactionIds: Seq[String]
    ): IO[ApiError, AutoCategorizationStatusView]
    
    def getAutoCategorizationStatus(jobId: String): UIO[Option[AutoCategorizationStatusView]]
    
    def getCategorySuggestion(transactionId: String): IO[ApiError, CategorySuggestionView]
    
    def getAvailableCategories(): UIO[Seq[CategoryView]]
```

### Submission Application Service

```scala
trait SubmissionApplicationService:
    def submitToYnab(
        transactionIds: Seq[String],
        targetBudgetId: String,
        targetAccountId: String
    ): IO[ApiError, SubmissionResultView]
    
    def getSubmissionStatus(submissionId: String): UIO[Option[SubmissionStatusView]]
```

## View Models

```scala
case class TransactionView(
    id: String,
    date: String,
    amount: String,
    description: String,
    payee: String,
    categoryId: Option[String],
    categoryName: Option[String],
    status: String
)

case class TransactionDetailView(
    id: String,
    date: String,
    amount: String,
    description: String,
    originalPayee: String,
    cleanedPayee: Option[String],
    counterAccountNumber: Option[String],
    counterBankCode: Option[String],
    variableSymbol: Option[String],
    constantSymbol: Option[String],
    specificSymbol: Option[String],
    categoryId: Option[String],
    categoryName: Option[String],
    status: String,
    importBatchId: String,
    ynabId: Option[String],
    submittedAt: Option[String],
    createdAt: String,
    updatedAt: Option[String]
)

case class PageView[T](
    items: Seq[T],
    totalItems: Int,
    totalPages: Int,
    currentPage: Int
)

case class ImportBatchStatusView(
    id: String,
    dateRange: String,
    status: String,
    transactionCount: Int,
    sourceAccount: String,
    createdAt: String,
    completedAt: Option[String]
)

case class CategoryView(
    id: String,
    name: String,
    ynabId: String,
    parentId: Option[String],
    parentName: Option[String]
)

case class CategorySuggestionView(
    categoryId: String,
    categoryName: String,
    confidence: Double,
    reasoning: String
)

case class AutoCategorizationStatusView(
    jobId: String,
    transactionCount: Int,
    completedCount: Int,
    inProgressCount: Int,
    failedCount: Int,
    status: String
)

case class SubmissionResultView(
    submissionId: String,
    transactionCount: Int,
    successCount: Int,
    failureCount: Int,
    status: String,
    errors: Option[Seq[String]]
)

case class SubmissionStatusView(
    submissionId: String,
    status: String,
    transactionCount: Int,
    successCount: Int,
    failureCount: Int,
    startedAt: String,
    completedAt: Option[String]
)

case class BulkUpdateResultView(
    successCount: Int,
    failureCount: Int,
    errors: Option[Seq[String]]
)

case class TransactionFiltersView(
    startDate: Option[String],
    endDate: Option[String],
    status: Option[String],
    minAmount: Option[String],
    maxAmount: Option[String],
    searchTerm: Option[String],
    categoryId: Option[String],
    importBatchId: Option[String]
)
```

## Scenario Implementation Mapping

This section maps the domain model components to the specific scenarios they support.

### Scenario 1: Successfully import transactions from Fio Bank
- **Key Components**: ImportService, FioBankPort, TransactionRepository, ImportBatchRepository
- **View Models**: ImportBatchStatusView, TransactionView

### Scenario 2: AI categorization of imported transactions
- **Key Components**: CategorizationService, AiCategorizationPort, TransactionRepository, CategoryRepository
- **View Models**: CategorySuggestionView, AutoCategorizationStatusView, TransactionView

### Scenario 3: Manual modification of transaction category
- **Key Components**: TransactionManagerService, TransactionRepository, CategoryRepository
- **View Models**: TransactionView, CategoryView

### Scenario 4: Bulk category modification
- **Key Components**: TransactionManagerService, TransactionRepository
- **View Models**: BulkUpdateResultView, TransactionView

### Scenario 5: Submit transactions to YNAB
- **Key Components**: SubmissionService, YnabPort, TransactionRepository
- **View Models**: SubmissionResultView, SubmissionStatusView

### Scenario 6: Handle YNAB API connection failure
- **Key Components**: SubmissionService, YnabPort, error handling in application services
- **View Models**: SubmissionResultView with error details

### Scenario 7: Prevent duplicate submission of transactions
- **Key Components**: SubmissionService (isDuplicate method), TransactionRepository
- **View Models**: SubmissionResultView with duplicate warnings

### Scenario 8: Filter transactions by status
- **Key Components**: TransactionManagerService, TransactionRepository
- **View Models**: TransactionFiltersView, PageView<TransactionView>

### Scenario 9: Validate transaction date range
- **Key Components**: ValidationService, ImportService
- **View Models**: ImportBatchStatusView with validation errors

### Scenario 10: Unauthorized access attempt (Deferred to Future Iteration)
- **Note**: Authentication implementation has been deferred to a future iteration. The MVS will use a deployment-specific security solution.

## Next Steps

1. Implement domain model interfaces and concrete classes
2. Create mock implementations for testing
3. Develop domain-level tests for scenarios
4. Refine view models for UI implementation

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2025-04-23 | Initial draft | Dev Team |