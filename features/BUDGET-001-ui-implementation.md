---
status: draft
last_updated: 2025-04-23
version: "0.1"
tags:
  - workflow
  - bdd
  - ui
  - implementation
---

> [!info] Draft Document
> This document is an initial draft and may change significantly.

# UI Implementation Plan: BUDGET-001

## Feature Reference
- **Related Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature Specification**: [BUDGET-001](./BUDGET-001.md)
- **Scenario Analysis**: [BUDGET-001-scenario-analysis](./BUDGET-001-scenario-analysis.md)
- **Domain Model**: [BUDGET-001-domain-model](./BUDGET-001-domain-model.md)
- **Domain Testing**: [BUDGET-001-domain-testing](./BUDGET-001-domain-testing.md)
- **Gherkin Feature**: [BUDGET-001.feature](./BUDGET-001.feature)
- **Implementation Plan**: [BUDGET-001-implementation-plan](./BUDGET-001-implementation-plan.md)

## Overview

This document outlines the UI implementation plan for the Fio Bank to YNAB Integration feature, following our BDD-Driven UI-First approach. The UI will be developed and validated with mock implementations of domain services before connecting to real infrastructure.

## UI Implementation Approach

1. **UI Components by Scenario**:
   - Map each scenario to specific UI components
   - Develop components that support multiple scenarios
   - Ensure all scenario steps are represented in the UI

2. **Mock Integration**:
   - Use the domain mock implementations for UI development
   - Create UI-specific service wrappers around domain services
   - Implement local data stores for UI state management

3. **Component-Based Development**:
   - Develop UI components in isolation first
   - Compose components to create complete scenario flows
   - Test each component against specific scenario steps

## UI Component Mapping to Scenarios

### 1. Login & Authentication Components
- **Scenarios**: Security/Authentication scenario
- **Components**:
  - LoginForm
  - AuthenticationProvider
  - ProtectedRoute
  - NavigationGuard

### 2. Import Transaction Components
- **Scenarios**: Import transactions, Validate date range
- **Components**:
  - ImportForm
  - DateRangePicker
  - ImportStatusIndicator
  - ValidationMessage
  - ImportResultsView

### 3. Transaction List Components
- **Scenarios**: View transactions, Filter transactions
- **Components**:
  - TransactionTable
  - TransactionListItem
  - FilterPanel
  - SearchBox
  - PaginationControls
  - StatusIndicator

### 4. Categorization Components
- **Scenarios**: AI categorization, Manual modification, Bulk category modification
- **Components**:
  - CategorySelector
  - CategorizationStatusIndicator
  - BulkActionBar
  - CategoryConfidenceIndicator
  - CategorySuggestionPanel

### 5. Submission Components
- **Scenarios**: Submit to YNAB, Handle API failure, Prevent duplicates
- **Components**:
  - SubmissionForm
  - SubmissionStatusIndicator
  - SubmissionResultsPanel
  - ErrorNotification
  - DuplicateWarning
  - RetryButton

## Component Design Details

### UI Component: ImportForm

```scala
case class ImportFormProps(
    onImport: (LocalDate, LocalDate, String) => Unit,
    availableAccounts: Seq[SourceAccountView],
    importInProgress: Boolean,
    validationErrors: Option[Seq[String]]
)

def ImportForm(props: ImportFormProps) =
    div(cls := "import-form")(
        h2("Import Transactions"),
        div(cls := "form-group")(
            label("Source Account"),
            select(id := "account-select")(
                props.availableAccounts.map { account =>
                    option(value := account.id)(account.name)
                }
            )
        ),
        div(cls := "form-group")(
            label("Date Range"),
            div(cls := "date-range")(
                DatePicker(id := "start-date", label := "From"),
                DatePicker(id := "end-date", label := "To")
            )
        ),
        when(props.validationErrors.isDefined)(
            div(cls := "validation-errors")(
                props.validationErrors.get.map { error =>
                    div(cls := "error-message")(error)
                }
            )
        ),
        button(
            cls := "primary-button",
            disabled := props.importInProgress,
            onClick := { _ =>
                val startDate = getDatePickerValue("start-date")
                val endDate = getDatePickerValue("end-date")
                val accountId = getSelectValue("account-select")
                props.onImport(startDate, endDate, accountId)
            }
        )(
            if (props.importInProgress) "Importing..." else "Import Transactions"
        )
    )
```

### UI Component: TransactionTable

```scala
case class TransactionTableProps(
    transactions: Seq[TransactionView],
    onSelectTransaction: TransactionView => Unit,
    onUpdateCategory: (String, String) => Unit,
    selectedTransactions: Seq[String],
    onSelectMultiple: Seq[String] => Unit,
    availableCategories: Seq[CategoryView],
    isLoading: Boolean
)

def TransactionTable(props: TransactionTableProps) =
    div(cls := "transaction-table-container")(
        when(props.isLoading)(
            div(cls := "loading-indicator")("Loading...")
        ),
        when(props.transactions.isEmpty && !props.isLoading)(
            div(cls := "empty-state")("No transactions found")
        ),
        when(props.transactions.nonEmpty)(
            table(cls := "transaction-table")(
                thead(
                    tr(
                        th(cls := "checkbox-column")(
                            input(
                                `type` := "checkbox",
                                onChange := { e =>
                                    if (e.target.checked)
                                        props.onSelectMultiple(props.transactions.map(_.id))
                                    else
                                        props.onSelectMultiple(Seq.empty)
                                }
                            )
                        ),
                        th("Date"),
                        th("Description"),
                        th("Payee"),
                        th("Amount"),
                        th("Category"),
                        th("Status"),
                        th("Actions")
                    )
                ),
                tbody(
                    props.transactions.map { transaction =>
                        tr(
                            cls := List(
                                "transaction-row" -> true,
                                "selected" -> props.selectedTransactions.contains(transaction.id)
                            ),
                            onClick := { _ => props.onSelectTransaction(transaction) }
                        )(
                            td(cls := "checkbox-column")(
                                input(
                                    `type` := "checkbox",
                                    checked := props.selectedTransactions.contains(transaction.id),
                                    onChange := { e =>
                                        val newSelected =
                                            if (e.target.checked)
                                                props.selectedTransactions :+ transaction.id
                                            else
                                                props.selectedTransactions.filterNot(_ == transaction.id)
                                        props.onSelectMultiple(newSelected)
                                    },
                                    onClick := { e => e.stopPropagation() }
                                )
                            ),
                            td(transaction.date),
                            td(transaction.description),
                            td(transaction.payee),
                            td(cls := "amount")(transaction.amount),
                            td(cls := "category-cell")(
                                CategorySelector(
                                    selectedCategoryId = transaction.categoryId,
                                    categories = props.availableCategories,
                                    onChange = { categoryId =>
                                        props.onUpdateCategory(transaction.id, categoryId)
                                    },
                                    disabled = transaction.status == "Submitted"
                                )
                            ),
                            td(StatusPill(transaction.status)),
                            td(cls := "actions-cell")(
                                button(
                                    cls := "detail-button",
                                    onClick := { e =>
                                        e.stopPropagation()
                                        props.onSelectTransaction(transaction)
                                    }
                                )("Details")
                            )
                        )
                    }
                )
            )
        )
    )
```

### UI Component: BulkActionBar

```scala
case class BulkActionBarProps(
    selectedCount: Int,
    availableCategories: Seq[CategoryView],
    onBulkCategorize: String => Unit,
    onBulkSubmit: () => Unit,
    canSubmit: Boolean
)

def BulkActionBar(props: BulkActionBarProps) =
    div(cls := "bulk-action-bar")(
        div(cls := "selected-count")(
            s"${props.selectedCount} transactions selected"
        ),
        div(cls := "actions")(
            div(cls := "bulk-categorize")(
                span("Set category:"),
                select(
                    disabled := props.selectedCount == 0,
                    onChange := { e => props.onBulkCategorize(e.target.value) }
                )(
                    option(value := "", disabled := true, selected := true)("Select category..."),
                    props.availableCategories.map { category =>
                        option(value := category.id)(category.name)
                    }
                )
            ),
            button(
                cls := "submit-button",
                disabled := !props.canSubmit || props.selectedCount == 0,
                onClick := { _ => props.onBulkSubmit() }
            )("Submit to YNAB")
        )
    )
```

## Page Designs

### Import & Transaction List Page

This page combines import functionality with transaction listing and management. It supports the following scenarios:

- Successfully import transactions from Fio Bank
- Filter transactions by status
- Validate transaction date range
- Manual modification of transaction category
- Bulk category modification

```scala
def TransactionManagementPage() =
    val (transactions, setTransactions) = useState[Seq[TransactionView]](Seq.empty)
    val (selectedTransactions, setSelectedTransactions) = useState[Seq[String]](Seq.empty)
    val (importInProgress, setImportInProgress) = useState(false)
    val (importErrors, setImportErrors) = useState[Option[Seq[String]]](None)
    val (filter, setFilter) = useState(TransactionFiltersView())
    val (categories, setCategories) = useState[Seq[CategoryView]](Seq.empty)
    val (accounts, setAccounts) = useState[Seq[SourceAccountView]](Seq.empty)

    // Mock service calls
    useEffect(() => {
        // Load categories
        MockCategorizationService.getAvailableCategories()
            .then(setCategories)

        // Load accounts
        MockAccountService.getSourceAccounts()
            .then(setAccounts)

        // Load initial transactions
        refreshTransactions(filter)
    }, [])

    def refreshTransactions(filters: TransactionFiltersView) = {
        MockTransactionService.getTransactions(filters, 1, 50)
            .then(result => setTransactions(result.items))
    }

    def handleImport(startDate: LocalDate, endDate: LocalDate, accountId: String) = {
        setImportInProgress(true)
        setImportErrors(None)

        MockImportService.startImport(accountId, startDate, endDate)
            .then(result => {
                setImportInProgress(false)
                refreshTransactions(filter)
            })
            .catch(error => {
                setImportInProgress(false)
                setImportErrors(Some(Seq(error.message)))
            })
    }

    def handleUpdateCategory(transactionId: String, categoryId: String) = {
        MockTransactionService.updateCategory(transactionId, categoryId)
            .then(_ => {
                refreshTransactions(filter)
            })
    }

    def handleBulkCategorize(categoryId: String) = {
        MockTransactionService.updateCategoryBulk(selectedTransactions, categoryId)
            .then(_ => {
                refreshTransactions(filter)
                setSelectedTransactions(Seq.empty)
            })
    }

    def handleSubmit() = {
        MockSubmissionService.submitToYnab(selectedTransactions, "default-budget", "default-account")
            .then(result => {
                showNotification(s"Submitted ${result.successCount} transactions successfully.")
                if (result.failureCount > 0) {
                    showError(s"${result.failureCount} transactions failed to submit.")
                }
                refreshTransactions(filter)
                setSelectedTransactions(Seq.empty)
            })
    }

    div(cls := "transaction-management-page")(
        h1("Transaction Management"),

        div(cls := "page-content")(
            div(cls := "left-panel")(
                ImportForm(
                    onImport = handleImport,
                    availableAccounts = accounts,
                    importInProgress = importInProgress,
                    validationErrors = importErrors
                ),

                FilterPanel(
                    filter = filter,
                    onFilterChange = newFilter => {
                        setFilter(newFilter)
                        refreshTransactions(newFilter)
                    }
                )
            ),

            div(cls := "main-panel")(
                BulkActionBar(
                    selectedCount = selectedTransactions.length,
                    availableCategories = categories,
                    onBulkCategorize = handleBulkCategorize,
                    onBulkSubmit = handleSubmit,
                    canSubmit = selectedTransactions.nonEmpty
                ),

                TransactionTable(
                    transactions = transactions,
                    onSelectTransaction = tx => showTransactionDetail(tx.id),
                    onUpdateCategory = handleUpdateCategory,
                    selectedTransactions = selectedTransactions,
                    onSelectMultiple = setSelectedTransactions,
                    availableCategories = categories,
                    isLoading = false
                ),

                PaginationControls()
            )
        )
    )
```

### Submission & Results Page

This page handles submission to YNAB and displays results. It supports the following scenarios:

- Submit transactions to YNAB
- Handle YNAB API connection failure
- Prevent duplicate submission of transactions

```scala
def SubmissionPage() =
    val (submissionResult, setSubmissionResult) = useState[Option[SubmissionResultView]](None)
    val (isSubmitting, setIsSubmitting) = useState(false)
    val (error, setError) = useState[Option[String]](None)
    val (selectedTransactions, setSelectedTransactions) = useState[Seq[TransactionView]](Seq.empty)

    // Load transactions that are ready for submission (has categories)
    useEffect(() => {
        MockTransactionService.getTransactions(
            TransactionFiltersView(status = Some("Categorized")),
            1,
            50
        ).then(result => {
            setSelectedTransactions(result.items)
        })
    }, [])

    def handleSubmit(budgetId: String, accountId: String) = {
        setIsSubmitting(true)
        setError(None)

        MockSubmissionService.submitToYnab(
            selectedTransactions.map(_.id),
            budgetId,
            accountId
        ).then(result => {
            setIsSubmitting(false)
            setSubmissionResult(Some(result))
        }).catch(err => {
            setIsSubmitting(false)
            setError(Some(err.message))
        })
    }

    def handleRetry() = {
        // Clear previous result and try again
        setSubmissionResult(None)
        handleSubmit("default-budget", "default-account")
    }

    div(cls := "submission-page")(
        h1("Submit Transactions to YNAB"),

        div(cls := "submission-form-container")(
            when(error.isDefined)(
                ErrorNotification(
                    message = error.get,
                    onRetry = Some(handleRetry)
                )
            ),

            when(submissionResult.isEmpty)(
                div(cls := "submission-form")(
                    h2("Ready to Submit"),
                    p(s"${selectedTransactions.length} transactions selected for submission"),

                    div(cls := "form-group")(
                        label("Target Budget"),
                        YnabBudgetSelector()
                    ),

                    div(cls := "form-group")(
                        label("Target Account"),
                        YnabAccountSelector()
                    ),

                    button(
                        cls := "submit-button",
                        disabled := isSubmitting || selectedTransactions.isEmpty,
                        onClick := { _ =>
                            handleSubmit("default-budget", "default-account")
                        }
                    )(
                        if (isSubmitting) "Submitting..." else "Submit to YNAB"
                    )
                )
            ),

            when(submissionResult.isDefined)(
                SubmissionResultsPanel(
                    result = submissionResult.get,
                    onDone = () => navigateTo("/transactions"),
                    onRetry = handleRetry
                )
            )
        )
    )
```

## UI Component Relationships

```plaintext
Navigation
├── Header
│   └── NavigationMenu
└── Content
    ├── TransactionManagementPage
    │   ├── ImportForm
    │   │   └── DateRangePicker
    │   ├── FilterPanel
    │   ├── BulkActionBar
    │   └── TransactionTable
    │       ├── TransactionListItem
    │       ├── CategorySelector
    │       └── StatusPill
    ├── TransactionDetailPage
    │   ├── TransactionDetails
    │   └── CategoryEditor
    └── SubmissionPage
        ├── SubmissionForm
        │   ├── YnabBudgetSelector
        │   └── YnabAccountSelector
        └── SubmissionResultsPanel
            └── ErrorNotification
```

## Page Navigation Flow

```plaintext
Login Page → Dashboard
              ↓
Dashboard → Transaction Management Page → Transaction Detail Page
              ↓                           ↓
Dashboard → Submission Page          → Transaction Management Page
              ↓
Dashboard
```

## Mock Services for UI Development

```scala
object MockImportService:
    def startImport(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Future[ImportBatchStatusView] = Future {
        // Validation
        if (startDate.isAfter(endDate))
            throw new Exception("End date must be after start date")

        if (ChronoUnit.DAYS.between(startDate, endDate) > 30)
            throw new Exception("Date range cannot exceed 30 days")

        // Return mock import result
        ImportBatchStatusView(
            id = "batch-" + System.currentTimeMillis(),
            dateRange = s"${startDate} to ${endDate}",
            status = "Completed",
            transactionCount = 10,
            sourceAccount = "Test Account",
            createdAt = LocalDateTime.now().toString,
            completedAt = Some(LocalDateTime.now().plusSeconds(3).toString)
        )
    }

object MockTransactionService:
    private var transactions = generateMockTransactions(50)

    def getTransactions(
        filters: TransactionFiltersView,
        page: Int,
        pageSize: Int
    ): Future[PageView[TransactionView]] = Future {
        var filtered = transactions

        // Apply filters
        if (filters.status.isDefined)
            filtered = filtered.filter(_.status == filters.status.get)

        if (filters.searchTerm.isDefined)
            filtered = filtered.filter(tx =>
                tx.description.contains(filters.searchTerm.get) ||
                tx.payee.contains(filters.searchTerm.get)
            )

        // Create page
        val start = (page - 1) * pageSize
        val end = Math.min(start + pageSize, filtered.length)
        val items = if (start < filtered.length) filtered.slice(start, end) else Seq.empty

        PageView(
            items = items,
            totalItems = filtered.length,
            totalPages = Math.ceil(filtered.length.toDouble / pageSize).toInt,
            currentPage = page
        )
    }

    def updateCategory(
        transactionId: String,
        categoryId: String
    ): Future[TransactionView] = Future {
        val txIndex = transactions.indexWhere(_.id == transactionId)
        if (txIndex < 0) throw new Exception("Transaction not found")

        // Get category name
        val categoryName = MockCategorizationService.getCategoryName(categoryId)

        // Update transaction
        val updatedTx = transactions(txIndex).copy(
            categoryId = Some(categoryId),
            categoryName = Some(categoryName),
            status = "Categorized"
        )

        transactions = transactions.updated(txIndex, updatedTx)
        updatedTx
    }

    def updateCategoryBulk(
        transactionIds: Seq[String],
        categoryId: String
    ): Future[BulkUpdateResultView] = Future {
        val categoryName = MockCategorizationService.getCategoryName(categoryId)

        var successCount = 0
        var failureCount = 0

        transactions = transactions.map { tx =>
            if (transactionIds.contains(tx.id)) {
                if (tx.status == "Submitted") {
                    failureCount += 1
                    tx // Cannot update submitted transaction
                } else {
                    successCount += 1
                    tx.copy(
                        categoryId = Some(categoryId),
                        categoryName = Some(categoryName),
                        status = "Categorized"
                    )
                }
            } else tx
        }

        BulkUpdateResultView(
            successCount = successCount,
            failureCount = failureCount,
            errors = if (failureCount > 0) Some(Seq("Some transactions could not be updated")) else None
        )
    }

object MockCategorizationService:
    private val categories = Seq(
        CategoryView("grocery", "Groceries", "ynab-grocery", None, None),
        CategoryView("dining", "Dining Out", "ynab-dining", None, None),
        CategoryView("transport", "Transportation", "ynab-transport", None, None),
        CategoryView("fuel", "Fuel", "ynab-fuel", Some("transport"), Some("Transportation")),
        CategoryView("entertainment", "Entertainment", "ynab-entertainment", None, None)
    )

    def getAvailableCategories(): Future[Seq[CategoryView]] = Future {
        categories
    }

    def getCategoryName(categoryId: String): String = {
        categories.find(_.id == categoryId).map(_.name).getOrElse("Unknown")
    }

object MockSubmissionService:
    def submitToYnab(
        transactionIds: Seq[String],
        budgetId: String,
        accountId: String
    ): Future[SubmissionResultView] = Future {
        // Simulate some processing time
        Thread.sleep(1500)

        // Simulate API error (10% chance)
        if (Random.nextDouble() < 0.1)
            throw new Exception("YNAB API connection error")

        // Get transactions from MockTransactionService
        val transactions = MockTransactionService.transactions
            .filter(tx => transactionIds.contains(tx.id))

        // Count transactions by status
        val alreadySubmitted = transactions.count(_.status == "Submitted")
        val successCount = transactions.length - alreadySubmitted

        // Update transaction status to submitted
        MockTransactionService.transactions = MockTransactionService.transactions.map { tx =>
            if (transactionIds.contains(tx.id) && tx.status != "Submitted") {
                tx.copy(status = "Submitted")
            } else tx
        }

        SubmissionResultView(
            submissionId = "submission-" + System.currentTimeMillis(),
            transactionCount = transactions.length,
            successCount = successCount,
            failureCount = alreadySubmitted,
            status = "Completed",
            errors = if (alreadySubmitted > 0)
                Some(Seq(s"$alreadySubmitted transactions were already submitted"))
                else None
        )
    }
```

## UI Testing Strategy

1. **Component Unit Tests**:
   - Test individual UI components with mock data
   - Verify component rendering and behavior
   - Test user interactions and state updates

2. **Scenario-Based UI Tests**:
   - Test complete UI flows for each scenario
   - Verify all scenario steps can be executed via the UI
   - Test UI response to different data conditions

3. **Mock Service Tests**:
   - Test interactions between UI and mock services
   - Verify UI properly handles service responses
   - Test error handling and edge cases

## Implementation Sequence

1. **Base UI Component Library**:
   - Create reusable UI components (buttons, inputs, tables)
   - Implement styling and layout foundation
   - Build mock service infrastructure

2. **Core Scenario Components**:
   - Implement components for most critical scenarios first
   - Focus on Transaction Table and Import Form components
   - Develop basic navigation and page structure

3. **Advanced Interaction Components**:
   - Implement category selection and bulk actions
   - Add filtering and searching capabilities
   - Develop submission workflow and results display

4. **Error Handling & Edge Cases**:
   - Add validation messages and error displays
   - Implement duplicate detection UI
   - Add loading states and progress indicators

## Scenario Implementation Prioritization

| Priority | Scenario | UI Components |
|----------|----------|---------------|
| 1 | Successfully import transactions | ImportForm, TransactionTable |
| 2 | Manual modification of transaction category | CategorySelector, TransactionTable |
| 3 | Filter transactions by status | FilterPanel, TransactionTable |
| 4 | Submit transactions to YNAB | BulkActionBar, SubmissionPage |
| 5 | AI categorization of imported transactions | CategoryConfidenceIndicator, TransactionTable |
| 6 | Bulk category modification | BulkActionBar, TransactionTable |
| 7 | Handle YNAB API connection failure | ErrorNotification, SubmissionResultsPanel |
| 8 | Prevent duplicate submission | DuplicateWarning, SubmissionResultsPanel |
| 9 | Validate transaction date range | ImportForm, ValidationMessage |
| (Deferred) | Unauthorized access attempt | (Deferred to future iteration) |

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2025-04-23 | Initial draft | Dev Team |
