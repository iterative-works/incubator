---
status: draft
last_updated: 2023-04-25
version: "0.1"
tags:
  - workflow
  - implementation-plan
---

# Feature Implementation Plan: Fio Bank to YNAB Integration

## Feature Overview
- **Description**: A web-based tool that automates the import, categorization, and submission of financial transactions from Fio Bank to the YNAB (You Need A Budget) application using AI for transaction categorization.
- **Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature File**: [BUDGET-001.md](../features/BUDGET-001.md)
- **Business Value**: Significantly reduces manual effort in financial data management, improves categorization accuracy through AI assistance, and enables more timely financial reporting and budget management.

## Domain Analysis

### Domain Concepts
| Concept | Description | Type | Existing/New |
|---------|-------------|------|--------------|
| Transaction | Core entity representing a financial transaction with properties like ID, description, date, amount, status | Entity | New |
| Category | Represents a transaction category with name and external ID | Entity | New |
| TransactionStatus | Enumeration of possible transaction states (Imported, Categorized, Submitted) | Value Object | New |
| ConfidenceScore | Numeric representation of categorization confidence | Value Object | New |
| DateRange | Represents start and end dates for transaction import | Value Object | New |
| TransactionRecord | External system representation of transaction data | Value Object | New |
| ImportCompletedEvent | Event published when import is completed successfully | Domain Event | New |
| TransactionsCategorizedEvent | Event published when categorization completes | Domain Event | New |
| CategoryUpdatedEvent | Event published when category is updated | Domain Event | New |
| TransactionsSubmittedEvent | Event published on successful submission | Domain Event | New |
| TransactionStatistics | Contains counts of transactions by status | Value Object | New |

### Domain Invariants
- Each transaction must have exactly one category before submission to YNAB
- Transactions must not be submitted to YNAB more than once
- All imported transactions must maintain an audit trail of status changes
- Category confidence score must be between 0.0 and 1.0
- Transaction status transitions must follow the defined workflow (Imported → Categorized → Submitted)

### Domain Events
- **ImportCompletedEvent**: Triggered when a batch of transactions is successfully imported, contains count of imported transactions
- **TransactionsCategorizedEvent**: Triggered when transactions are successfully categorized, contains list of transaction IDs and average confidence score
- **CategoryUpdatedEvent**: Triggered when a transaction category is manually updated, contains transaction ID and old/new category information
- **TransactionsSubmittedEvent**: Triggered when transactions are successfully submitted to YNAB, contains count and list of transaction IDs

## Implementation Steps

### Step 1: Core Domain Entities and Value Objects
- **Component Type**: Entity, Value Object
- **Component Name**: `Transaction`, `Category`, `TransactionStatus`, `ConfidenceScore`
- **Package Location**: `works.iterative.incubator.ynab.domain.model`
- **Purpose**: Define the core domain model representing financial transactions and their properties
- **Key Behaviors**:
  - Transaction entity with immutable properties and status tracking
  - Category entity with name and external mapping to YNAB
  - Status transitions that enforce the workflow rules
  - Confidence score validation
- **Dependencies**:
  - None (core domain model)
- **Acceptance Criteria**:
  - All domain invariants are enforced through type constraints
  - Transaction status can only transition according to workflow rules
  - Confidence score is constrained to valid range
  - All domain objects are immutable
- **Implementation Guide**: Functional Core Architecture Guide

### Step 2: Domain Services and Repositories Interfaces
- **Component Type**: Service Interface, Repository Interface
- **Component Name**: `ImportService`, `CategorizationService`, `SubmissionService`, `TransactionRepository`, `CategoryRepository`
- **Package Location**: `works.iterative.incubator.ynab.domain.service`, `works.iterative.incubator.ynab.domain.repository`
- **Purpose**: Define the service interfaces that will coordinate domain operations
- **Key Behaviors**:
  - ImportService: Coordinate transaction import workflow
  - CategorizationService: Apply categorization rules to transactions
  - SubmissionService: Manage submission of transactions to YNAB
  - TransactionRepository: Store and retrieve transaction entities
  - CategoryRepository: Store and retrieve category entities
- **Dependencies**:
  - Domain Entities and Value Objects
- **Acceptance Criteria**:
  - All service interfaces use pure functions with explicit effect types
  - Repository interfaces support required query patterns
  - All interfaces align with domain scenarios
- **Implementation Guide**: ZIO Service Pattern Guide

### Step 3: Domain Event Implementation
- **Component Type**: Domain Event
- **Component Name**: `ImportCompletedEvent`, `TransactionsCategorizedEvent`, `CategoryUpdatedEvent`, `TransactionsSubmittedEvent`
- **Package Location**: `works.iterative.incubator.ynab.domain.event`
- **Purpose**: Define the domain events that signal important state changes
- **Key Behaviors**:
  - Event publication when domain actions complete
  - Carrying relevant context data with events
- **Dependencies**:
  - Domain Entities and Value Objects
- **Acceptance Criteria**:
  - Events contain all necessary information for subscribers
  - Events are immutable and serializable
- **Implementation Guide**: Domain Events Implementation Guide

### Step 4: External Ports Interfaces
- **Component Type**: Port Interface
- **Component Name**: `TransactionProvider`, `CategorizationProvider`, `TransactionSubmissionPort`
- **Package Location**: `works.iterative.incubator.ynab.domain.port`
- **Purpose**: Define the interfaces for external system interactions
- **Key Behaviors**:
  - TransactionProvider: Interface for external transaction sources
  - CategorizationProvider: Interface for categorization services
  - TransactionSubmissionPort: Interface for submitting to external systems
- **Dependencies**:
  - Domain Entities and Value Objects
- **Acceptance Criteria**:
  - Ports clearly define required external capabilities
  - All external dependencies are abstracted behind interfaces
- **Implementation Guide**: Ports and Adapters Pattern Guide

### Step 5: Mock Implementations for Domain Testing
- **Component Type**: Mock Implementation
- **Component Name**: `InMemoryTransactionRepository`, `InMemoryCategoryRepository`, `MockTransactionProvider`, `MockCategorizationProvider`, `MockTransactionSubmissionPort`
- **Package Location**: `works.iterative.incubator.ynab.domain.mock`
- **Purpose**: Create in-memory implementations for testing domain logic
- **Key Behaviors**:
  - In-memory storage of domain objects
  - Configurable behaviors for testing edge cases
  - Event publication simulation
- **Dependencies**:
  - Domain interfaces from Steps 2 and 4
- **Acceptance Criteria**:
  - Mocks implement all interface methods
  - Mocks allow verification of domain behavior
  - Mocks support test configuration
- **Implementation Guide**: Mock Implementation Guide

### Step 6: Domain-Level Test Implementation
- **Component Type**: Test Suite
- **Component Name**: `ImportServiceSpec`, `CategorizationServiceSpec`, `SubmissionServiceSpec`
- **Package Location**: `works.iterative.incubator.ynab.domain.service.test`
- **Purpose**: Verify domain logic through comprehensive tests
- **Key Behaviors**:
  - Test all domain scenarios from scenario analysis
  - Verify event publication
  - Test edge cases and error conditions
- **Dependencies**:
  - Domain services and mock implementations
- **Acceptance Criteria**:
  - All @domain scenarios are covered by tests
  - All domain invariants are verified
  - Edge cases are handled properly
- **Implementation Guide**: ZIO Test Implementation Guide

### Step 7: View Models Definition
- **Component Type**: View Model
- **Component Name**: `DashboardViewModel`, `TransactionTableViewModel`, `ImportViewModel`, `CategoryViewModel`
- **Package Location**: `works.iterative.incubator.ynab.presentation.viewmodel`
- **Purpose**: Define the data models for UI presentation
- **Key Behaviors**:
  - Transform domain objects to presentation format
  - Support UI state management
  - Handle user input validation
- **Dependencies**:
  - Domain model
- **Acceptance Criteria**:
  - View models contain all data needed for UI rendering
  - Clear separation from domain models
- **Implementation Guide**: View Model Pattern Guide

### Step 8: UI Components Implementation
- **Component Type**: UI Component
- **Component Name**: `DashboardView`, `TransactionTable`, `ImportDialog`, `CategoryDropdown`
- **Package Location**: `works.iterative.incubator.ynab.presentation.component`
- **Purpose**: Implement the user interface components
- **Key Behaviors**:
  - Render view models as HTML/HTMX
  - Handle user interactions
  - Support responsive display
- **Dependencies**:
  - View models
  - Mock service implementations
- **Acceptance Criteria**:
  - Components render correctly with mock data
  - Interactive elements function properly
  - Layout is responsive
- **Implementation Guide**: Scalatags + HTMX Guide

### Step 9: UI Controller Implementation
- **Component Type**: Controller
- **Component Name**: `DashboardController`, `TransactionController`, `ImportController`
- **Package Location**: `works.iterative.incubator.ynab.presentation.controller`
- **Purpose**: Coordinate between UI and domain services
- **Key Behaviors**:
  - Handle HTTP requests
  - Delegate to domain services
  - Prepare view models for rendering
- **Dependencies**:
  - Domain services
  - View models
- **Acceptance Criteria**:
  - Controllers properly delegate to domain services
  - Controllers transform domain results to view models
  - Error handling is implemented
- **Implementation Guide**: HTTP Controller Pattern Guide

### Step 10: UI-Level Test Implementation
- **Component Type**: Test Suite
- **Component Name**: `DashboardViewSpec`, `TransactionTableSpec`, `ImportDialogSpec`
- **Package Location**: `works.iterative.incubator.ynab.presentation.component.test`
- **Purpose**: Verify UI behavior and rendering
- **Key Behaviors**:
  - Test UI rendering with different view models
  - Test UI interactions
  - Verify controller integration
- **Dependencies**:
  - UI components
  - Mock service implementations
- **Acceptance Criteria**:
  - All @ui scenarios are covered by tests
  - UI components render correctly
  - User interactions work as expected
- **Implementation Guide**: UI Testing Guide

### Step 11: External System Adapters Implementation
- **Component Type**: Adapter
- **Component Name**: `FioBankAdapter`, `OpenAIAdapter`, `YNABAdapter`
- **Package Location**: `works.iterative.incubator.ynab.infrastructure.adapter`
- **Purpose**: Implement the concrete adapters for external systems
- **Key Behaviors**:
  - Connect to external APIs
  - Transform data between external and domain formats
  - Handle authentication and error conditions
- **Dependencies**:
  - Domain port interfaces
  - External libraries for API access
- **Acceptance Criteria**:
  - Adapters properly implement port interfaces
  - Error handling for API failures
  - Proper authentication management
- **Implementation Guide**: External System Integration Guide

### Step 12: Persistent Repository Implementation
- **Component Type**: Repository Implementation
- **Component Name**: `PostgreSQLTransactionRepository`, `PostgreSQLCategoryRepository`
- **Package Location**: `works.iterative.incubator.ynab.infrastructure.repository`
- **Purpose**: Implement persistent storage for domain entities
- **Key Behaviors**:
  - Map between domain objects and database schema
  - Execute database queries efficiently
  - Handle database errors
- **Dependencies**:
  - Domain repository interfaces
  - Mangum for database access
- **Acceptance Criteria**:
  - Repositories properly implement domain interfaces
  - Database queries are type-safe and efficient
  - Proper error handling for database failures
- **Implementation Guide**: Mangum Repository Guide

### Step 13: Integration and E2E Test Implementation
- **Component Type**: Test Suite
- **Component Name**: `TransactionRepositoryIntegrationSpec`, `FioBankAdapterIntegrationSpec`, `YNABAdapterIntegrationSpec`, `E2EWorkflowSpec`
- **Package Location**: `works.iterative.incubator.ynab.it`
- **Purpose**: Verify integration between components and end-to-end workflows
- **Key Behaviors**:
  - Test repository with real database
  - Test adapters with mock external services
  - Test complete workflows
- **Dependencies**:
  - All implemented components
  - TestContainers for database testing
- **Acceptance Criteria**:
  - All @integration scenarios are covered
  - All @e2e scenarios are covered
  - Performance requirements are verified
- **Implementation Guide**: Integration Testing Guide

## Bounded Context Integration
- **Primary Bounded Context**: Transaction Processing
- **Related Bounded Contexts**:
  - Source Account Management: Provides account information for transaction import via context map
  - YNAB Integration: Consumes transaction data via downstream integration

## Environment Composition
- **Required ZIO Services**:
  - HTTP4S for web server
  - ZIO Config for configuration management
  - ZIO Logging for structured logging
  - ZIO Json for serialization
  - Magnum for database access
- **New Services to Implement**:
  - ImportService
  - CategorizationService
  - SubmissionService
  - TransactionRepository
  - CategoryRepository
  - StatisticsService
  - FioBankService
  - YNABService
  - OpenAIService

## Testing Strategy
### Unit Tests
- **Domain Logic Tests**:
  - Test transaction import workflow creates proper domain records
  - Test transaction categorization applies rules correctly
  - Test manual category override updates transaction correctly
  - Test transaction submission workflow marks records as submitted
  - Test duplicate transaction detection prevents reprocessing

### Integration Tests
- **Repository Tests**:
  - Test transaction persistence and retrieval
  - Test category persistence and retrieval
  - Test transaction status updates
- **External System Tests**:
  - Test Fio Bank API connection and data retrieval
  - Test YNAB API connection and data submission
  - Test OpenAI API for transaction categorization

### End-to-End Tests
- **Feature Scenarios**:
  - Complete transaction import-categorize-submit workflow
  - User modifies categories and submits transactions
  - System prevents duplicate transaction submission
  - System calculates and displays transaction statistics correctly

## Implementation Schedule
- **Estimated Total Time**: 80 hours
- **Step Dependencies**:
  1. Step 1 → Step 2, Step 3
  2. Step 2, Step 3 → Step 4
  3. Step 4 → Step 5
  4. Step 1-5 → Step 6
  5. Step 1-6 → Step 7, Step 11
  6. Step 7 → Step 8
  7. Step 2, Step 7 → Step 9
  8. Step 8, Step 9 → Step 10
  9. Step 2, Step 4 → Step 12
  10. All previous steps → Step 13

| Step | Description | Est. Time | Prerequisites | Developer |
|------|-------------|-----------|--------------|-----------|
| 1 | Core Domain Entities and Value Objects | 8h | None | TBD |
| 2 | Domain Services and Repository Interfaces | 6h | Step 1 | TBD |
| 3 | Domain Event Implementation | 4h | Step 1 | TBD |
| 4 | External Ports Interfaces | 4h | Steps 2, 3 | TBD |
| 5 | Mock Implementations for Domain Testing | 8h | Step 4 | TBD |
| 6 | Domain-Level Test Implementation | 10h | Steps 1-5 | TBD |
| 7 | View Models Definition | 4h | Steps 1-6 | TBD |
| 8 | UI Components Implementation | 12h | Step 7 | TBD |
| 9 | UI Controller Implementation | 6h | Steps 2, 7 | TBD |
| 10 | UI-Level Test Implementation | 6h | Steps 8, 9 | TBD |
| 11 | External System Adapters Implementation | 10h | Steps 1-6 | TBD |
| 12 | Persistent Repository Implementation | 8h | Steps 2, 4 | TBD |
| 13 | Integration and E2E Test Implementation | 12h | All previous steps | TBD |

## Risk Assessment
- **Technical Risks**:
  - **Risk 1**: Transaction volume could be very large, impacting performance
    - **Mitigation**: Implement batch processing with pagination and optimize database queries
  - **Risk 2**: External API failures (Fio Bank, YNAB, OpenAI)
    - **Mitigation**: Implement robust error handling, retry mechanisms, and circuit breaker patterns
  - **Risk 3**: Database performance with large transaction sets
    - **Mitigation**: Optimize schema design, use proper indexing, implement query optimization
  - **Risk 4**: UI performance with large transaction tables
    - **Mitigation**: Implement pagination, lazy loading, and optimize rendering

- **Domain Risks**:
  - **Risk 1**: AI categorization might not meet accuracy requirements
    - **Mitigation**: Implement feedback mechanism to improve categorization over time, allow manual overrides
  - **Risk 2**: Transaction data format from Fio Bank might vary or contain unexpected values
    - **Mitigation**: Create robust data validation and error handling
  - **Risk 3**: Category mapping between systems might be inconsistent
    - **Mitigation**: Implement mapping validation and fallback categories
  - **Risk 4**: Duplicate detection might miss some edge cases
    - **Mitigation**: Implement multiple detection methods based on different transaction properties

## Review Checklist
- [ ] All domain concepts clearly identified
- [ ] Steps are properly sequenced
- [ ] Dependencies between components identified
- [ ] Each step has clear acceptance criteria
- [ ] Testing strategy covers all components
- [ ] Implementation guides referenced for each step
- [ ] Risks identified with mitigation strategies

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2023-04-25 | Initial draft | AI Assistant & Human Partner |
