---
status: draft
last_updated: 2023-04-24
version: "0.1"
tags:
  - workflow
  - bdd
  - scenario-analysis
---

# Scenario Analysis: Fio Bank to YNAB Integration

## Feature Reference
- **Related Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature Specification**: [BUDGET-001.md](./BUDGET-001.md)
- **Gherkin Feature File**: [BUDGET-001.feature](./BUDGET-001.feature)
- **Analysis Date**: 2023-04-24
- **Participants**: AI Assistant, Human Partner

## Scenario Overview

| Scenario ID | Scenario Name | Priority | Value Driver | Complexity (1-5) | Dependencies |
|-------------|---------------|----------|--------------|------------------|----------------|
| Domain-1 | Transaction import workflow creates proper domain records | High | Data Integrity | 2 | None |
| Domain-2 | Transaction categorization applies rules correctly | High | Automation | 3 | Domain-1 |
| Domain-3 | Manual category override updates transaction correctly | Medium | User Control | 2 | Domain-2 |
| Domain-4 | Bulk category update processes multiple transactions | Medium | Efficiency | 3 | Domain-2, Domain-3 |
| Domain-5 | Transaction submission workflow marks records as submitted | High | Data Sync | 2 | Domain-2 |
| Domain-6 | Duplicate transaction detection prevents reprocessing | High | Data Integrity | 3 | Domain-1 |
| Domain-7 | Transactions require categories before submission | Medium | Data Quality | 1 | Domain-2, Domain-5 |
| Domain-8 | Transaction status statistics are calculated correctly | Low | Reporting | 1 | Domain-1, Domain-2, Domain-5 |
| Domain-9 | Transaction import and categorization meets performance requirements | Medium | Performance | 4 | Domain-1, Domain-2 |
| UI-1 | Dashboard displays transaction summary statistics | Medium | Visibility | 2 | Domain-8 |
| UI-2 | Dashboard statistics update dynamically | Low | UX | 2 | UI-1 |
| UI-3 | User can initiate a new transaction import with date range | High | User Control | 3 | Domain-1 |
| UI-4 | Transaction list provides sorting and filtering | Medium | Usability | 2 | Domain-1 |
| UI-5 | User can edit transaction category via dropdown | High | User Control | 2 | Domain-3, UI-4 |
| UI-6 | Bulk selection and submission of transactions | High | Efficiency | 3 | Domain-5, UI-4 |
| UI-7 | Error messages are displayed for validation failures | Medium | Error Handling | 2 | UI-6 |
| UI-8 | User interface actions meet response time requirements | Medium | Performance | 3 | All UI scenarios |
| Integration-1 | System connects successfully to Fio Bank API | High | Connectivity | 3 | None |
| Integration-2 | Fio Bank transactions are retrieved and transformed correctly | High | Data Quality | 4 | Integration-1 |
| Integration-3 | AI service categorizes transactions with required accuracy | High | Automation | 5 | Domain-2 |
| Integration-4 | System handles Fio Bank API failures gracefully | Medium | Resilience | 3 | Integration-1 |
| Integration-5 | System connects successfully to YNAB API | High | Connectivity | 3 | None |
| Integration-6 | Transactions are submitted correctly to YNAB | High | Data Sync | 4 | Integration-5, Domain-5 |
| Integration-7 | System handles YNAB API rate limiting | Medium | Resilience | 3 | Integration-6 |
| Integration-8 | Import process meets throughput requirements | Medium | Performance | 4 | Integration-2 |
| E2E-1 | Complete transaction import-categorize-submit workflow | High | Core Value | 5 | Multiple |
| E2E-2 | User modifies categories and submits transactions | High | Core Value | 4 | Multiple |
| E2E-3 | System prevents duplicate transaction submission | Medium | Data Integrity | 3 | Domain-6, E2E-1 |
| E2E-4 | Complete workflow meets all performance criteria | Medium | Performance | 4 | Domain-9, UI-8, Integration-8 |
| E2E-5 | System handles network interruption during workflow | Medium | Resilience | 4 | Integration-4, Integration-7 |

## Detailed Scenario Analysis

### Scenario: Transaction import workflow creates proper domain records

```gherkin
@domain
Scenario: Transaction import workflow creates proper domain records
  When the import workflow receives 5 transaction records from a provider
  Then 5 transaction domain entities should be created
  And each transaction should have "Imported" status
  And an "ImportCompleted" domain event should be published with count 5
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Entity | Transaction | Core entity representing a financial transaction with properties like ID, description, date, amount, status | When/Then steps |
| Value Object | TransactionStatus | Enumeration of possible transaction states (Imported, Categorized, Submitted) | And each transaction should have "Imported" status |
| Value Object | TransactionRecord | External system representation of transaction data | When the import workflow receives 5 transaction records |
| Domain Service | ImportService | Service responsible for handling import operations | When the import workflow receives 5 transaction records |
| Domain Event | ImportCompletedEvent | Event published when import is completed successfully | And an "ImportCompleted" domain event should be published |
| Repository | TransactionRepository | Interface for persisting and retrieving transaction entities | Then 5 transaction domain entities should be created |
| Port | TransactionProvider | Abstract interface to get transactions from an external source | When the import workflow receives 5 transaction records |

#### UI Component Requirements

Not applicable for this domain-level scenario.

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| Transaction Provider | Read | Provider sends transaction records to the system | When the import workflow receives 5 transaction records from a provider |

#### Implementation Risks and Considerations

- **Risk 1**: Transaction volume could be very large, impacting performance
  - Mitigation: Implement batch processing and pagination
- **Risk 2**: Transaction data format might vary or contain unexpected values
  - Mitigation: Create robust data validation and error handling

#### Test Considerations

- **Test Data Requirements**: Sample transaction records in the format expected from providers
- **Mock Requirements**: Mock transaction provider for testing the import workflow
- **Edge Cases**: Empty import, malformed data, duplicate external IDs
- **Performance Considerations**: Test with large transaction sets (100+ transactions)

### Scenario: Transaction categorization applies rules correctly

```gherkin
@domain
Scenario: Transaction categorization applies rules correctly
  Given 3 uncategorized transactions exist in the system
  When the categorization service processes the transactions
  Then each transaction should have a category assigned
  And each categorization should have a confidence score
  And transactions should have "Categorized" status
  And a "TransactionsCategorized" domain event should be published
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Entity | Transaction | Core entity with newly added category property | Given 3 uncategorized transactions exist |
| Entity | Category | Represents a transaction category with name and external ID | Then each transaction should have a category assigned |
| Value Object | ConfidenceScore | Represents the confidence level of categorization | And each categorization should have a confidence score |
| Domain Service | CategorizationService | Service responsible for transaction categorization | When the categorization service processes the transactions |
| Domain Event | TransactionsCategorizedEvent | Event published when categorization completes | And a "TransactionsCategorized" domain event should be published |
| Repository | TransactionRepository | Interface for accessing transactions | Given 3 uncategorized transactions exist |
| Repository | CategoryRepository | Interface for accessing categories | Then each transaction should have a category assigned |
| Port | CategorizationProvider | Abstract interface to categorization capability | When the categorization service processes the transactions |

#### UI Component Requirements

Not applicable for this domain-level scenario.

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| Categorization Provider | Service Call | System sends transaction descriptions, provider returns category suggestions with confidence scores | When the categorization service processes the transactions |

#### Implementation Risks and Considerations

- **Risk 1**: AI categorization might not meet accuracy requirements
  - Mitigation: Implement feedback mechanism to improve categorization over time
- **Risk 2**: External categorization service could be slow or unavailable
  - Mitigation: Implement fallback rules and timeout handling

#### Test Considerations

- **Test Data Requirements**: Transactions with various descriptions covering common scenarios
- **Mock Requirements**: Mock categorization provider with predictable responses
- **Edge Cases**: Ambiguous transactions, unknown merchant names, non-standard descriptions
- **Performance Considerations**: Test categorization with batches of different sizes

### Scenario: Manual category override updates transaction correctly

```gherkin
@domain
Scenario: Manual category override updates transaction correctly
  Given a transaction with ID "tx-123" exists with category "Groceries"
  When a user updates the category to "Dining Out" for transaction "tx-123"
  Then the transaction "tx-123" should have category "Dining Out"
  And a "CategoryUpdated" domain event should be published
  And the event should contain transaction ID "tx-123"
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Entity | Transaction | Transaction entity with updatable category | Given a transaction with ID exists |
| Entity | Category | Category entity referenced by transaction | Given transaction exists with category / When updated |
| Domain Service | CategoryUpdateService | Service for handling category updates | When a user updates the category |
| Domain Event | CategoryUpdatedEvent | Event published when category is updated | And a "CategoryUpdated" domain event should be published |
| Repository | TransactionRepository | Interface to access and update transactions | Given/When/Then steps involving transactions |

#### UI Component Requirements

| UI Component | Responsibility | Scenario Step |
|--------------|----------------|---------------|
| CategoryDropdown | Presents available categories for selection | When a user updates the category |
| TransactionEditor | Allows editing transaction properties | When a user updates the category |

#### External System Interactions

Not directly applicable for this domain-level scenario.

#### Implementation Risks and Considerations

- **Risk 1**: Category updates might conflict if multiple users edit simultaneously
  - Mitigation: Implement optimistic concurrency control
- **Risk 2**: Performance impact if events trigger expensive processing
  - Mitigation: Use asynchronous event processing where appropriate

#### Test Considerations

- **Test Data Requirements**: Transaction with assigned category
- **Mock Requirements**: Mock repository for testing category updates
- **Edge Cases**: Updating to same category, updating non-existent category
- **Performance Considerations**: Test event publication performance

### Scenario: Transaction submission workflow marks records as submitted

```gherkin
@domain
Scenario: Transaction submission workflow marks records as submitted
  Given 3 categorized transactions exist
  When the submission workflow processes these transactions
  Then each transaction should have "Submitted" status
  And a "TransactionsSubmitted" domain event should be published with count 3
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Entity | Transaction | Transaction with submittable status | Given 3 categorized transactions exist |
| Value Object | TransactionStatus | Status enum including "Submitted" state | Then each transaction should have "Submitted" status |
| Domain Service | SubmissionService | Service handling submission workflows | When the submission workflow processes these transactions |
| Domain Event | TransactionsSubmittedEvent | Event published on successful submission | And a "TransactionsSubmitted" domain event should be published |
| Repository | TransactionRepository | Repository for accessing transactions | Given/When/Then steps involving transactions |
| Port | TransactionSubmissionPort | Interface for submitting transactions to external systems | When the submission workflow processes these transactions |

#### UI Component Requirements

Not applicable for this domain-level scenario.

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| YNAB API | Write | System submits transaction data to YNAB | When the submission workflow processes these transactions |

#### Implementation Risks and Considerations

- **Risk 1**: External system could reject some transactions
  - Mitigation: Implement partial success handling and detailed error reporting
- **Risk 2**: Network failures during submission
  - Mitigation: Transaction-based approach ensuring either all or none are marked submitted

#### Test Considerations

- **Test Data Requirements**: Categorized transactions ready for submission
- **Mock Requirements**: Mock submission port to simulate external system
- **Edge Cases**: Failed submissions, partial successes
- **Performance Considerations**: Test with varying batch sizes

### Scenario: Dashboard displays transaction summary statistics

```gherkin
@ui
Scenario: Dashboard displays transaction summary statistics
  When the user navigates to the dashboard
  Then the dashboard should display the following summary statistics:
    | statistic               | value |
    | Total Transactions      | 25    |
    | Categorized             | 15    |
    | Submitted               | 5     |
  And the dashboard should contain an "Import" button
  And the dashboard should display a transaction table with status indicators
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Value Object | TransactionStatistics | Contains counts of transactions by status | Then the dashboard should display summary statistics |
| Domain Service | StatisticsService | Calculates transaction statistics | When the user navigates to the dashboard |
| Repository | TransactionRepository | Provides transaction data for statistics | When the user navigates to the dashboard |

#### UI Component Requirements

| UI Component | Responsibility | Scenario Step |
|--------------|----------------|---------------|
| DashboardView | Main dashboard container | When the user navigates to the dashboard |
| StatisticsPanel | Displays transaction statistics | Then the dashboard should display summary statistics |
| ImportButton | Button to initiate new import | And the dashboard should contain an "Import" button |
| TransactionTable | Table showing transaction list | And the dashboard should display a transaction table |
| StatusIndicator | Visual indicator of transaction status | And the dashboard should display a transaction table with status indicators |

#### External System Interactions

No direct external system interactions in this scenario.

#### Implementation Risks and Considerations

- **Risk 1**: Slow statistics calculation with large transaction volumes
  - Mitigation: Cache statistics or use database aggregation queries
- **Risk 2**: UI performance with large transaction tables
  - Mitigation: Implement pagination and lazy loading

#### Test Considerations

- **Test Data Requirements**: Transactions in various states (imported, categorized, submitted)
- **Mock Requirements**: Mock statistics service for UI testing
- **Edge Cases**: No transactions, all transactions in same state
- **Performance Considerations**: Test rendering with large datasets

### Scenario: User can initiate a new transaction import with date range

```gherkin
@ui
Scenario: User can initiate a new transaction import with date range
  Given the user is on the dashboard screen
  When the user clicks the "Import" button
  Then an import dialog should appear
  And the dialog should contain "Start Date" and "End Date" fields
  And the dialog should contain an "Import" button
  
  When the user selects start date "2025-04-01" and end date "2025-04-15"
  And the user clicks the import dialog "Import" button
  Then the system should delegate to the domain import service with these dates
  And the user should see a loading indicator
  And the user should be notified when import is complete
  And the transaction table should update with new transactions
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Value Object | DateRange | Represents start and end dates for import | When the user selects date range |
| Domain Service | ImportService | Handles import requests with date range | Then the system should delegate to the domain import service |
| Domain Event | ImportCompletedEvent | Event published when import completes | And the user should be notified when import is complete |

#### UI Component Requirements

| UI Component | Responsibility | Scenario Step |
|--------------|----------------|---------------|
| ImportButton | Triggers import dialog | When the user clicks the "Import" button |
| ImportDialog | Dialog for import configuration | Then an import dialog should appear |
| DatePicker | Date selection control | And the dialog should contain date fields |
| LoadingIndicator | Shows import in progress | And the user should see a loading indicator |
| NotificationComponent | Shows import completion | And the user should be notified when import is complete |
| TransactionTable | Displays imported transactions | And the transaction table should update with new transactions |

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| Fio Bank API | Read | System requests transactions for date range | Then the system should delegate to the domain import service |

#### Implementation Risks and Considerations

- **Risk 1**: Long-running imports could cause UI unresponsiveness
  - Mitigation: Implement asynchronous processing with progress updates
- **Risk 2**: Date range too broad could cause performance issues
  - Mitigation: Implement date range validation and limits

#### Test Considerations

- **Test Data Requirements**: Valid date ranges with known transaction counts
- **Mock Requirements**: Mock import service with controlled response times
- **Edge Cases**: Invalid date ranges, empty results, very large results
- **Performance Considerations**: Test with various date range sizes

### Scenario: System connects successfully to Fio Bank API

```gherkin
@integration
Scenario: System connects successfully to Fio Bank API
  When the Fio Bank provider attempts to establish a connection
  Then the connection should succeed
  And the provider should authenticate successfully
  And the system should receive a valid session token
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Port | FioBankPort | Abstract interface for Fio Bank API | When the Fio Bank provider attempts to establish a connection |
| Value Object | ConnectionCredentials | Encapsulates authentication details | When the Fio Bank provider attempts to establish a connection |
| Value Object | SessionToken | Represents valid authentication token | And the system should receive a valid session token |

#### UI Component Requirements

Not applicable for this integration-level scenario.

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| Fio Bank API | Authentication | System sends credentials, receives token | When the provider attempts to establish a connection |

#### Implementation Risks and Considerations

- **Risk 1**: Authentication failures due to invalid or expired credentials
  - Mitigation: Implement credential refresh mechanism and clear error messages
- **Risk 2**: API changes could break connection
  - Mitigation: Version checking and graceful degradation

#### Test Considerations

- **Test Data Requirements**: Valid API credentials for testing
- **Mock Requirements**: Mock Fio Bank API for testing authentication flows
- **Edge Cases**: Expired credentials, network timeouts, invalid responses
- **Performance Considerations**: Test authentication response time

### Scenario: AI service categorizes transactions with required accuracy

```gherkin
@integration
Scenario: AI service categorizes transactions with required accuracy
  Given 100 transactions with descriptions and known correct categories are available
  When the categorization provider processes these transactions
  Then at least 80 transactions should be assigned their correct category
  And each categorization should include a confidence score
  And the system should meet the 80% accuracy requirement specified in NFR1
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Port | AICategorizationPort | Abstract interface to AI categorization service | When the categorization provider processes these transactions |
| Value Object | CategorizationResult | Contains category and confidence score | Then at least 80 transactions should be assigned their correct category |
| Value Object | ConfidenceScore | Numeric representation of categorization confidence | And each categorization should include a confidence score |

#### UI Component Requirements

Not applicable for this integration-level scenario.

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| OpenAI API | Service Call | System sends transaction descriptions, receives categories with confidence scores | When the categorization provider processes these transactions |

#### Implementation Risks and Considerations

- **Risk 1**: AI service might not meet accuracy requirements
  - Mitigation: Implement training feedback loop, use multiple models for validation
- **Risk 2**: API costs could escalate with high volumes
  - Mitigation: Implement caching of common categorizations, batch processing

#### Test Considerations

- **Test Data Requirements**: Diverse set of transaction descriptions with known correct categories
- **Mock Requirements**: Mock AI service with controlled accuracy for testing
- **Edge Cases**: Ambiguous descriptions, new merchants, foreign language descriptions
- **Performance Considerations**: Test with batch processing of various sizes

### Scenario: Complete transaction import-categorize-submit workflow

```gherkin
@e2e
Scenario: Complete transaction import-categorize-submit workflow
  When the user imports transactions from Fio Bank for period "2025-04-01" to "2025-04-15"
  Then the system should retrieve transactions from Fio Bank
  And save them to the database
  And display them in the transaction table
  
  When the user requests automated categorization
  Then the system should categorize the transactions using the AI service
  And update the transaction display with categories
  
  When the user reviews and approves the categories
  And selects all transactions
  And clicks "Submit to YNAB"
  Then the system should submit the transactions to YNAB
  And display a success confirmation
  And update transaction status to "Submitted"
```

#### Domain Model Requirements

| Component Type | Component Name | Description | Scenario Step |
|----------------|---------------|-------------|---------------|
| Domain Service | ImportService | Handles transaction import | When the user imports transactions |
| Domain Service | CategorizationService | Handles transaction categorization | When the user requests automated categorization |
| Domain Service | SubmissionService | Handles transaction submission | When the user clicks "Submit to YNAB" |
| Repository | TransactionRepository | Stores and retrieves transactions | And save them to the database |

#### UI Component Requirements

| UI Component | Responsibility | Scenario Step |
|--------------|----------------|---------------|
| ImportDialog | Configure and initiate import | When the user imports transactions |
| TransactionTable | Display transaction data | And display them in the transaction table |
| CategorizeButton | Initiate categorization | When the user requests automated categorization |
| CategoryDisplay | Show category assignments | And update the transaction display with categories |
| TransactionSelector | Select transactions for submission | And selects all transactions |
| SubmitButton | Trigger submission process | And clicks "Submit to YNAB" |
| ConfirmationDialog | Show submission results | And display a success confirmation |

#### External System Interactions

| External System | Interaction Type | Data Exchange | Scenario Step |
|-----------------|------------------|---------------|-----------------|
| Fio Bank API | Read | System retrieves transaction data | Then the system should retrieve transactions from Fio Bank |
| OpenAI API | Service Call | System sends descriptions, receives categories | Then the system should categorize the transactions using the AI service |
| YNAB API | Write | System submits categorized transactions | Then the system should submit the transactions to YNAB |

#### Implementation Risks and Considerations

- **Risk 1**: Failure at any stage could leave workflow in inconsistent state
  - Mitigation: Implement proper transaction boundaries and rollback capability
- **Risk 2**: End-to-end performance might not meet user expectations
  - Mitigation: Implement progress indicators, background processing where appropriate

#### Test Considerations

- **Test Data Requirements**: Test accounts for Fio Bank and YNAB with controlled data
- **Mock Requirements**: Mock all external systems for testing full workflow
- **Edge Cases**: Failures at different stages of the workflow
- **Performance Considerations**: Test complete workflow with realistic data volumes

## Shared Components Across Scenarios

### Shared Domain Components

| Component | Scenarios | Shared Responsibility |
|-----------|-----------|------------------------|
| Transaction Entity | All | Core data model representing financial transactions |
| Category Entity | Domain-2, Domain-3, UI-5, E2E-1, E2E-2 | Represents categorization information |
| TransactionRepository | All Domain, Integration-6 | Persistent storage of transactions |
| ImportService | Domain-1, UI-3, Integration-2, E2E-1 | Coordinating transaction import |
| CategorizationService | Domain-2, Integration-3, E2E-1 | Managing transaction categorization |
| SubmissionService | Domain-5, Integration-6, E2E-1, E2E-2 | Handling submission to external systems |
| TransactionStatus | Domain-1, Domain-2, Domain-5, UI-1, E2E-1 | Tracking state of transactions |

### Shared UI Components

| UI Component | Scenarios | Shared Responsibility |
|--------------|-----------|------------------------|
| TransactionTable | UI-1, UI-3, UI-4, UI-5, UI-6, E2E-1, E2E-2 | Displaying transaction data |
| ImportDialog | UI-3, E2E-1 | Configuring transaction imports |
| CategoryDropdown | UI-5, E2E-2 | Selecting transaction categories |
| DashboardView | UI-1, UI-2, UI-3 | Main application dashboard |
| NotificationComponent | UI-3, UI-5, UI-6, UI-7, E2E-1, E2E-3 | User feedback and alerts |

## Implementation Sequence

### Phase 1: Core Domain Model Implementation

1. **Domain Entities and Value Objects**:
   - Transaction, Category
   - TransactionStatus, ConfidenceScore, DateRange
   - ImportCompletedEvent, TransactionsCategorizedEvent, CategoryUpdatedEvent, TransactionsSubmittedEvent

2. **Domain Services**:
   - ImportService
   - CategorizationService
   - CategoryUpdateService
   - SubmissionService
   - StatisticsService

3. **Repository Interfaces**:
   - TransactionRepository
   - CategoryRepository

4. **Ports/Adapters Interfaces**:
   - TransactionProvider
   - CategorizationProvider
   - TransactionSubmissionPort

### Phase 2: Mock Implementations and Domain Testing

1. **Mock Repositories**:
   - InMemoryTransactionRepository
   - InMemoryCategoryRepository

2. **Mock External Systems**:
   - MockTransactionProvider
   - MockCategorizationProvider
   - MockTransactionSubmissionPort

3. **Domain-Level Tests**:
   - Test all @domain scenarios
   - Validate core business rules
   - Test event publishing

### Phase 3: UI Implementation

1. **View Models**:
   - DashboardViewModel
   - TransactionTableViewModel
   - ImportViewModel
   - CategoryViewModel

2. **UI Components**:
   - DashboardView
   - TransactionTable
   - ImportDialog
   - CategoryDropdown
   - StatisticsPanel

3. **UI-Level Tests**:
   - Test all @ui scenarios
   - Validate component interactions
   - Test user workflows

### Phase 4: Infrastructure Implementation

1. **Real Repositories**:
   - PostgreSQLTransactionRepository
   - PostgreSQLCategoryRepository

2. **External System Adapters**:
   - FioBankAdapter
   - OpenAIAdapter
   - YNABAdapter

3. **E2E Tests**:
   - Test all @e2e scenarios
   - Validate complete workflows
   - Performance testing

## Technical Decisions

| Decision | Rationale | Scenarios Affected |
|----------|-----------|-------------------|
| Use ZIO for effect management | Provides clean separation between pure domain logic and side effects, aligning with functional core architecture | All scenarios |
| Implement PostgreSQL repositories with Doobie | Type-safe database access with good ZIO integration | All persistence-related scenarios |
| Use OpenAI API for categorization | Highest accuracy potential compared to alternatives | All categorization scenarios |
| Implement HTMX + Scalatags for UI | Simplifies UI updates without heavy JavaScript requirements | All UI scenarios |
| Batch process transactions | Improves performance for large transaction sets | Import and categorization scenarios |
| Implement optimistic concurrency | Prevents data corruption during concurrent edits | Category update scenarios |
| Use webhook-based notifications | Allows asynchronous completion of long-running operations | Import and categorization scenarios |
| Implement caching for categorization results | Reduces API costs and improves performance | All categorization scenarios |
| Use transaction-based approach for submission | Ensures consistent state in case of failures | All submission scenarios |

## Review Notes

### Domain Model Review
- **Reviewer**: To be completed after initial review
- **Date**: TBD
- **Comments**: TBD
- **Changes Requested**: TBD

### Technical Approach Review
- **Reviewer**: To be completed after initial review
- **Date**: TBD
- **Comments**: TBD
- **Changes Requested**: TBD

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2023-04-24 | Initial draft | AI Assistant & Human Partner |