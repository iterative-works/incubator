# YNAB Importer Architecture

## Overview
The YNAB Importer follows a BDD-Driven UI-First approach with a Functional Core/Imperative Shell architecture. The application imports bank transactions from various sources, processes them with AI-assisted categorization, and exports them to YNAB (You Need A Budget). Our development is scenario-driven, using Gherkin features as the core of our workflow.

## Architecture Approach

### BDD-Driven UI-First Development

Our development follows these key principles:

1. **Scenarios Drive Everything**: All development is guided by Gherkin scenarios
2. **UI-First Development**: We build and validate the user experience with mocks before implementing real infrastructure
3. **Functional Core, Imperative Shell**: We maintain strict separation between pure functional domain logic and side-effecting code
4. **Multi-Level Testing**: We test scenarios at domain, UI, and end-to-end levels

This approach gives us several advantages:
- Early validation of user experience
- Clear connection between requirements and implementation
- Faster feedback cycles
- Maintainable architecture with clear separation of concerns

## UI Module Vertical Slices

Based on our scenario analysis, we organize the application into vertical slices within a bounded context:

```
fio-ynab-integration/            # Bounded Context
├── domain/                      # Shared Domain Core
├── import/                      # Import Module (Vertical Slice)
├── categorization/              # Categorization Module
├── submission/                  # Submission Module 
└── transaction-management/      # Transaction Management Module
```

Each vertical slice:
- Maps to specific Gherkin scenarios in our feature files
- Contains a complete implementation from UI to infrastructure
- Can be developed and tested independently
- Uses the shared domain core for common functionality

## Domain Model Design

### Event-Centric Approach

The core of our design is an event-centric model that treats transactions as immutable events:

1. **Transaction (Immutable Event)**
   - Represents a financial transaction exactly as it occurred
   - Contains only raw transaction data from the bank
   - Never changes after creation
   - References a source account by ID
   - Functions as a value object with identity

2. **TransactionProcessingState (Mutable State)**
   - Tracks the processing lifecycle of a transaction
   - Contains categorization, status, AI suggestions
   - References the immutable Transaction by ID
   - Can change over time as the transaction is processed
   - Maintains a clear processing history

3. **SourceAccount (Reference Entity)**
   - Contains configuration for bank connections
   - Maps between bank accounts and YNAB accounts
   - Stores metadata about synchronization
   - Functions as a lookup/reference entity

This approach gives us several advantages:
- Clear separation between "what happened" and "what we're doing with it"
- Better audit trail of transaction processing
- More explicit handling of state changes
- Cleaner domain model with proper boundaries

### Repository Design

Our repositories reflect this event-centric model:

1. **TransactionRepository**
   - Manages immutable Transaction events
   - Supports queries based on transaction attributes
   - Ensures immutability of core transaction data

2. **TransactionProcessingStateRepository**
   - Manages mutable TransactionProcessingState entities
   - Supports queries based on processing status
   - Facilitates transaction lifecycle management

3. **SourceAccountRepository**
   - Manages SourceAccount entities
   - Supports account discovery and configuration
   - Tracks synchronization state

## Architecture Layers

The application follows the Functional Core/Imperative Shell pattern:

### Functional Core (Domain Layer)
- **Domain Models**: Pure representations of business concepts
  - `Transaction`, `TransactionId`, `TransactionProcessingState`, `SourceAccount`
- **Query Models**: Composable filtering capabilities
  - `TransactionQuery`, `TransactionProcessingStateQuery`, `SourceAccountQuery`
- **Repository Interfaces**: Domain-defined capabilities
  - `TransactionRepository`, `TransactionProcessingStateRepository`, `SourceAccountRepository`

### Imperative Shell (Infrastructure Layer)
- **Repository Implementations**: Concrete data access
  - `PostgreSQLTransactionRepository`, `PostgreSQLTransactionProcessingStateRepository`, `PostgreSQLSourceAccountRepository`
  - `MockTransactionRepository`, `MockTransactionProcessingStateRepository`, `MockSourceAccountRepository`
- **Adapters**: External service integration
  - FIO bank adapter (`FioClient`, `FioTransactionImportService`)
  - Mock adapters for UI development
- **Database Components**:
  - `PostgreSQLTransactor`, `PostgreSQLDataSource`
  - `FlywayMigrationService` for schema management

### Web Layer
- **UI Modules**: Scenario-based vertical slices
  - **ImportModule**: For handling transaction imports (Scenarios 1, 9)
  - **CategorizationModule**: For AI and manual categorization (Scenarios 2, 3, 4)
  - **SubmissionModule**: For submitting to YNAB (Scenarios 5, 6, 7)
  - **TransactionManagementModule**: For filtering and viewing (Scenario 8)

## Module Structure

Each module follows our Functional MVP pattern with this structure:

```
import/                      # Import Module
├── domain/                  # Module-specific domain logic
│   ├── ImportService.scala
│   └── DateRangeValidator.scala
├── application/             # Application Services
│   ├── ImportApplicationService.scala
│   └── ImportTransactionsCommand.scala
├── infrastructure/          # Infrastructure
│   ├── MockFioAdapter.scala # Mock implementation (UI-First)
│   ├── LiveFioAdapter.scala # Real implementation (Production)
│   └── ...
└── web/                     # UI components
    ├── ImportModule.scala   # Module definition
    ├── ImportViewModel.scala # View Model
    ├── ImportService.scala  # UI Service
    └── views/
        ├── ImportView.scala # View
        └── ...
```

## Database Schema

The database schema reflects our event-centric model:

1. **source_account**: Bank account information
   - Maps bank accounts to YNAB accounts
   - Tracks synchronization state

2. **transaction**: Immutable transaction events
   - Core transaction data from the bank
   - References source_account

3. **transaction_processing_state**: Mutable processing state
   - Processing status, categorization, YNAB integration
   - References transaction

## Bounded Context Integration

With our UI-Module Vertical Slices approach, we're transitioning from technology-oriented bounded contexts (transactions, fio, ynab) to a more cohesive feature-oriented bounded context (fio-ynab-integration) with a shared domain core.

Within this bounded context, we:
- Maintain direct database references (as in the previous architecture)
- Share the domain core across all vertical slices
- Keep clear boundaries between UI modules

## Scenario-to-Module Mapping

Based on our BDD approach, we map scenarios to modules as follows:

| Module | Scenarios | Key Functionality |
|--------|-----------|-------------------|
| Import | 1, 9 | Import transactions from Fio Bank, Date range validation |
| Categorization | 2, 3, 4 | AI categorization, Manual modification, Bulk modification |
| Submission | 5, 6, 7 | Submit to YNAB, Error handling, Duplicate prevention |
| Transaction Management | 8 | Filtering and viewing transactions |

## Implementation Process

For each module, our implementation follows this process:

1. **Domain Model First**: 
   - Implement domain entities, value objects, and services
   - Create repository interfaces based on scenario needs

2. **Mock Services**:
   - Implement in-memory repository implementations
   - Create mock implementations of external services

3. **UI Development**:
   - Build UI components using mock implementations
   - Implement user flows per scenarios
   - Validate with users

4. **Real Infrastructure Implementation**:
   - Implement real repositories and external service adapters
   - Replace mocks with real implementations
   - Test scenarios with real infrastructure

## Implementation Patterns

### Effect Management with ZIO
- Pure domain logic with effect-tracked IO operations
- ZIO Environment for dependency injection
- ZLayer for composable component dependencies

### Repository Pattern
- Domain-defined interfaces in the core
- Both mock and real infrastructure implementations
- Generic operations with domain-specific queries

### Performance Optimization
- In-memory caching using ZIO Ref for thread-safety
- Caching frequently accessed reference data (like SourceAccount)
- Cache invalidation at logical boundaries (e.g., import batch starts)
- Unit tests to verify caching behavior

### Data Transfer and Mapping
- DTOs for database communication
- Chimney for type-safe transformations
- Clear separation between domain models and DTOs

## Processing Flow

1. **Import Phase** (Import Module)
   - External bank data is fetched through adapters (e.g., FIO Bank)
   - Raw data is transformed into immutable Transaction events
   - Initial TransactionProcessingState is created with Imported status

2. **Processing Phase** (Categorization Module)
   - AI categorization is applied to transactions
   - Self-learning payee name cleanup is applied to transactions
   - User can review and override categorizations
   - TransactionProcessingState is updated to Categorized status

3. **Export Phase** (Submission Module)
   - Categorized transactions are submitted to YNAB
   - TransactionProcessingState is updated with YNAB IDs and Submitted status
   - Synchronization metadata is updated

## Self-Learning Payee Cleanup System

This subsystem improves transaction data quality by cleaning up messy payee names before they're sent to YNAB.

### Design Approach
- Combines LLM-based processing with a rule-based system that learns over time
- Starts with no predefined rules and builds a rule database through usage
- Uses feedback mechanisms to evaluate and improve rule quality

### Key Components

1. **PayeeCleanupRule (Domain Entity)**
   - Defines patterns for matching transaction payees
   - Supports multiple pattern types (exact, contains, startsWith, regex)
   - Tracks usage statistics and success rates
   - Maintains approval workflow statuses

2. **Rule Application Tracking**
   - Records every rule application to a transaction
   - Collects feedback on rule effectiveness
   - Updates rule metrics based on feedback

### Learning Workflow

1. **Initial Processing**
   - When no matching rules exist, LLM is used to clean payee names
   - LLM suggests potential rules based on the cleaning process

2. **Rule Management**
   - Suggested rules start in "Pending" status for admin review
   - Rules can be approved, rejected, or modified
   - Human-created rules start as "Approved"

3. **Continuous Improvement**
   - Rules are evaluated based on application success rate
   - Higher confidence rules are preferred over lower confidence ones
   - Over time, the system relies less on LLM calls and more on proven rules

### Integration Points
- Integrates with the TransactionProcessor during the Processing Phase
- Updates the TransactionProcessingState with cleaned payee names
- Provides feedback mechanisms for users to improve rule quality

## Technologies

- **Scala 3**: Primary language with pragmatic functional programming
- **ZIO**: Effect system and dependency management
- **PostgreSQL**: Relational database
- **Flyway**: Database migration
- **Magnum**: SQL library with type-safe queries
- **Chimney**: Object-to-object mapping
