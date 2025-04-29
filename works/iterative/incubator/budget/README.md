# Budget Bounded Context

This bounded context handles transaction importing, categorization, and submission to external budget systems (currently YNAB). It follows a functional core architecture with immutable domain entities and clearly defined workflows.

## Architecture Overview

The Budget bounded context is structured according to the Functional Core/Imperative Shell architecture pattern, with:

- **Domain Model (Functional Core):** Pure, immutable entities and value objects defining the domain concepts and business rules
- **Domain Services:** Define the workflow operations and business logic for core domain processes
- **Repository Interfaces:** Define data access capabilities without implementation details
- **Domain Events:** Represent significant state changes and trigger workflows
- **Port Interfaces:** Define required capabilities from external systems
- **Infrastructure (Imperative Shell):** Implementations of repository and port interfaces, providing concrete data access and external system integration
- **Presentation Layer:** UI components and scenario mappings for the user interface

## Core Entities and Value Objects

### Transaction Domain Model

| Component | Type | Location | Purpose | Scenarios Supported | Key Relationships |
|-----------|------|----------|---------|---------------------|------------------|
| `Transaction` | Entity | `domain/model/Transaction.scala` | Immutable representation of a financial transaction from a bank | Transaction import workflow, Duplicate detection | Referenced by `TransactionProcessingState`, identified by `TransactionId` |
| `TransactionId` | Value Object | `domain/model/TransactionId.scala` | Composite identifier for a transaction | Duplicate transaction detection | Contained in `Transaction`, references `SourceAccount` via ID |
| `TransactionStatus` | Enum | `domain/model/TransactionStatus.scala` | Status in the processing pipeline | Workflow state tracking, Transaction submission validation | Used by `TransactionProcessingState` to track workflow stage |
| `TransactionProcessingState` | Entity | `domain/model/TransactionProcessingState.scala` | Tracks the mutable processing state of a transaction | All scenarios including categorization, submission, duplicate detection | References `Transaction` via `TransactionId`, contains confidence scores and categorization data |
| `ConfidenceScore` | Value Object | `domain/model/ConfidenceScore.scala` | Represents the confidence level of AI categorization | Transaction categorization, Confidence score validation | Used by `TransactionProcessingState` for category confidence tracking |

### Category Domain Model

| Component | Type | Location | Purpose | Scenarios Supported | Key Relationships |
|-----------|------|----------|---------|---------------------|------------------|
| `Category` | Entity | `domain/model/Category.scala` | Represents a transaction category with hierarchy | Transaction categorization, Manual category updates, Category mapping to YNAB | Referenced by `TransactionProcessingState`, includes YNAB mapping |

### Source Account Domain Model

| Component | Type | Location | Purpose | Scenarios Supported | Key Relationships |
|-----------|------|----------|---------|---------------------|------------------|
| `SourceAccount` | Entity | `domain/model/SourceAccount.scala` | Bank account from which transactions are imported | Transaction import workflow | Referenced by `TransactionId` and `Transaction` |
| `CreateSourceAccount` | Command | `domain/model/SourceAccount.scala` | Data transfer object for account creation | Account management | Creates `SourceAccount` instances |

## Domain Services

Service interfaces and implementations that define the core business operations:

| Service | Location | Purpose | Scenarios Supported | Key Relationships |
|---------|----------|---------|---------------------|------------------|
| `ImportService` | `domain/service/ImportService.scala` | Defines transaction import workflow | Transaction import, Duplicate detection | Works with `TransactionRepository`, publishes `ImportCompleted` events |
| `CategorizationService` | `domain/service/CategorizationService.scala` | Defines transaction categorization | Transaction categorization, Manual overrides, Bulk updates | Works with `TransactionProcessingStateRepository` and `CategoryRepository` |
| `SubmissionService` | `domain/service/SubmissionService.scala` | Defines submission to external systems | Transaction submission, Validation, Statistics | Works with `TransactionProcessingStateRepository`, validates submission requirements |

## Domain Events

Events that represent significant occurrences in the transaction processing workflow:

| Event | Location | Purpose | Scenarios Supported | Key Relationships |
|-------|----------|---------|---------------------|------------------|
| `DomainEvent` | `domain/event/DomainEvent.scala` | Base trait for all domain events | All event-driven scenarios | Parent of all event types |
| `TransactionImported` | `domain/event/TransactionImported.scala` | Single transaction imported | Transaction import workflow | References `TransactionId` |
| `ImportCompleted` | `domain/event/ImportCompleted.scala` | Batch import completion | Transaction import workflow | Contains count of imported transactions |
| `TransactionCategorized` | `domain/event/TransactionCategorized.scala` | Single transaction categorized | Transaction categorization | References `TransactionId` and category |
| `TransactionsCategorized` | `domain/event/TransactionsCategorized.scala` | Batch categorization | Transaction categorization | Contains `ConfidenceScore` average |
| `CategoryUpdated` | `domain/event/CategoryUpdated.scala` | Manual category update | Manual category override | References `TransactionId` and categories |
| `BulkCategoryUpdated` | `domain/event/BulkCategoryUpdated.scala` | Bulk category update | Bulk category update | Contains update filter criteria |
| `TransactionSubmitted` | `domain/event/TransactionSubmitted.scala` | Single transaction submitted | Transaction submission workflow | References `TransactionId` and YNAB IDs |
| `TransactionsSubmitted` | `domain/event/TransactionsSubmitted.scala` | Batch submission completed | Transaction submission workflow | Contains count of submitted transactions |
| `DuplicateTransactionDetected` | `domain/event/DuplicateTransactionDetected.scala` | Duplicate transaction found | Duplicate transaction detection | References external ID and existing transaction |
| `SubmissionFailed` | `domain/event/SubmissionFailed.scala` | YNAB submission error | Transaction submission validation | Contains failure reason |

## Presentation Components

UI components and related artifacts for the presentation layer:

| Component | Type | Location | Purpose | Scenarios Supported | Key Relationships |
|-----------|------|----------|---------|---------------------|------------------|
| `UIScenarioMap` | Documentation | `presentation/scenarios/UIScenarioMap.scala` | Maps Gherkin scenarios to UI components and interactions | All UI scenarios (UI-1 through UI-7) | References domain services for integration points, defines view models for UI components |

## Key Domain Workflows

### Transaction Import Workflow
1. Import transactions from a source account
2. Check for duplicates using `TransactionId`
3. Create `Transaction` entities for new transactions
4. Initialize `TransactionProcessingState` for each transaction with `Imported` status
5. Emit `TransactionImported` events for individual transactions
6. Emit an `ImportCompleted` event with the total count

### Transaction Categorization Workflow
1. Process imported transactions through categorization logic
2. Assign suggested categories with confidence scores
3. Update processing state to `Categorized` status
4. Emit `TransactionCategorized` events for individual transactions
5. Emit a `TransactionsCategorized` event with batch information

### Transaction Submission Workflow
1. Identify categorized transactions that are ready for submission
2. Submit transactions to YNAB
3. Update processing state with YNAB IDs and `Submitted` status
4. Emit `TransactionSubmitted` events for successful submissions
5. Emit a `TransactionsSubmitted` event with the batch count