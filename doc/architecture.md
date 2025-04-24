# YNAB Importer Architecture (Revised)

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

## Single Bounded Context Architecture

Our application is organized as a single bounded context (BudgetTransactions) with anti-corruption layers for external systems:

### BudgetTransactions Context
- Core domain models (Transaction, Category, Account)
- End-to-end transaction processing workflow
- Unified repository interfaces
- Domain events for state transitions
- Complete transaction lifecycle management

### Anti-Corruption Layers
- **Bank Integration Adapters**: Convert external bank data formats to our domain model
  - Fio Bank adapter
  - Future adapters (Revolut, etc.)
- **Budget Tool Adapters**: Convert our domain model to external budget tool formats
  - YNAB adapter
  - Future adapters

### Supporting Services
- **Import Services**: Data import from various banks
- **Categorization Services**: AI and manual categorization
- **Submission Services**: Export to budget tools
- **Transaction Management Services**: Filtering, viewing, reporting

### Auth Component (Future)
- User authentication and authorization
- Role and permission management
- Security infrastructure

## Package Structure

Our package structure reflects this unified approach:

```
works.iterative.incubator/
├── budget/                   # Single BudgetTransactions Context
│   ├── domain/               # Core domain models and logic
│   │   ├── model/            # Core domain entities
│   │   │   ├── transaction/  # Transaction related models
│   │   │   ├── account/      # Account related models
│   │   │   └── category/     # Category related models
│   │   ├── service/          # Domain services
│   │   ├── repository/       # Repository interfaces
│   │   ├── event/            # Domain events
│   │   └── query/            # Query models
│   │
│   ├── application/          # Application services
│   │   ├── import/           # Import use cases
│   │   ├── categorization/   # Categorization use cases
│   │   ├── submission/       # Submission use cases
│   │   └── management/       # Transaction management use cases
│   │
│   ├── infrastructure/       # Infrastructure implementations
│   │   ├── persistence/      # Repository implementations
│   │   │   ├── transaction/  # Transaction repositories
│   │   │   ├── account/      # Account repositories
│   │   │   └── category/     # Category repositories
│   │   ├── config/           # Configuration
│   │   └── security/         # Security components
│   │
│   ├── adapters/             # Anti-corruption layers
│   │   ├── bank/             # Bank adapters
│   │   │   ├── fio/          # Fio Bank adapter
│   │   │   │   ├── client/   # Fio API client
│   │   │   │   ├── model/    # Fio-specific models
│   │   │   │   ├── mapper/   # Mappers from Fio models to domain models
│   │   │   │   ├── service/  # Fio-specific services
│   │   │   │   └── config/   # Fio-specific configuration
│   │   │   └── revolut/      # Future Revolut adapter (similar structure)
│   │   │
│   │   └── budget/           # Budget tool adapters
│   │       └── ynab/         # YNAB adapter
│   │           ├── client/   # YNAB API client
│   │           ├── model/    # YNAB-specific models
│   │           ├── mapper/   # Mappers from domain models to YNAB models
│   │           ├── service/  # YNAB-specific services
│   │           └── config/   # YNAB-specific configuration
│   │
│   └── web/                  # Web UI components
│       ├── module/           # Module definitions
│       │   ├── import/       # Import UI modules
│       │   ├── category/     # Category UI modules
│       │   ├── submission/   # Submission UI modules
│       │   └── management/   # Transaction management UI modules
│       ├── view/             # Views
│       ├── controller/       # Controllers/Routes
│       └── dto/              # Data transfer objects
│
└── server/                   # Application bootstrap and server configuration
    ├── config/               # Server configuration
    ├── http/                 # HTTP server setup
    ├── module/               # Module registry and composition
    └── Main.scala            # Application entry point
```

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
   - Maps between bank accounts and budget tool accounts
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

Our architecture follows the Functional Core/Imperative Shell pattern:

### Functional Core (Domain Layer)
- **Domain Models**: Pure representations of business concepts
- **Query Models**: Composable filtering capabilities
- **Repository Interfaces**: Domain-defined capabilities
- **Domain Services**: Business logic without side effects

### Imperative Shell (Infrastructure Layer)
- **Repository Implementations**: Concrete data access
- **Adapters**: External service integration
- **Database Components**: Data access infrastructure
- **Integration Services**: Cross-boundary communication

### Web Layer
- **UI Modules**: Scenario-based vertical slices
- **View Models**: UI-specific data representations
- **Views**: UI rendering components
- **Routes**: HTTP endpoints

## Module Structure

We organize functionality into vertical slices that correspond to specific scenarios, following this general structure:

```
budget/                     # Single BudgetTransactions context
├── domain/                 # Domain logic
│   ├── model/              # Domain models
│   ├── service/            # Domain services
│   └── repository/         # Repository interfaces
├── application/            # Application Services
│   ├── import/             # Import use cases
│   ├── categorization/     # Categorization use cases
│   ├── submission/         # Submission use cases
│   └── management/         # Management use cases
├── infrastructure/         # Infrastructure components
│   ├── config/             # Configuration
│   ├── security/           # Security components
│   └── persistence/        # Repository implementations
├── adapters/               # Anti-corruption layers
│   ├── bank/               # Bank adapters
│   │   └── fio/            # Fio Bank adapter
│   │       ├── client/     # API client
│   │       ├── model/      # External system-specific models
│   │       ├── mapper/     # Model mappers
│   │       ├── service/    # Adapter-specific services
│   │       └── config/     # Adapter-specific configuration
│   └── budget/             # Budget tool adapters
│       └── ynab/           # YNAB adapter
└── web/                    # UI components
    ├── module/             # Module definitions
    ├── view/               # Views
    ├── controller/         # Controllers/Routes
    └── dto/                # Data transfer objects
```

## Database Schema

The database schema reflects our event-centric model:

1. **source_account**: Bank account information
   - Maps bank accounts to budget tool accounts
   - Tracks synchronization state

2. **transaction**: Immutable transaction events
   - Core transaction data from the bank
   - References source_account

3. **transaction_processing_state**: Mutable processing state
   - Processing status, categorization, budget tool integration
   - References transaction

## Scenario-to-Module Mapping

Based on our BDD approach, we map scenarios to vertical slice modules:

| Module | Adapter | Scenarios | Key Functionality |
|---------|---------|-----------|-------------------|
| Import | Generic | - | Generic import infrastructure |
| Import | Fio | 1, 9 | Import transactions from Fio Bank, Date range validation |
| Categorization | - | 2, 3, 4 | AI categorization, Manual modification, Bulk modification |
| Submission | Generic | - | Generic submission infrastructure |
| Submission | YNAB | 5, 6, 7 | Submit to YNAB, Error handling, Duplicate prevention |
| Transaction Management | - | 8 | Filtering and viewing transactions |

## Feature File Organization

To align with our scenario-driven approach, our Gherkin feature files are organized by functional area:

```
features/
├── import/                # Import scenarios 
│   ├── common/            # Generic import features
│   └── adapters/          # Bank-specific features
│       └── fio_import.feature  # Fio-specific scenarios (1, 9)
├── categorization/        # Categorization scenarios (scenarios 2, 3, 4)
│   └── transaction_categorization.feature
├── submission/            # Submission scenarios
│   ├── common/            # Generic submission features
│   └── adapters/          # Budget tool-specific features
│       └── ynab_submission.feature  # YNAB-specific scenarios (5, 6, 7)
└── management/            # Management scenarios (scenario 8)
    └── transaction_management.feature
```

## Implementation Process

Our implementation follows this process:

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

## Data Flow and Integration

The single bounded context approach simplifies our processing flow, with clear phase boundaries:

1. **Import Phase**
   - External bank data is fetched through bank adapters (e.g., Fio Bank adapter)
   - Adapter-specific data is transformed into domain model using mappers
   - Raw data is transformed into immutable Transaction events
   - Initial TransactionProcessingState is created with Imported status

2. **Processing Phase**
   - AI categorization is applied to transactions
   - Self-learning payee name cleanup is applied to transactions
   - User can review and override categorizations
   - TransactionProcessingState is updated to Categorized status

3. **Export Phase**
   - Categorized transactions are submitted to budget tools through budget adapters (e.g., YNAB adapter)
   - Domain models are transformed to adapter-specific models using mappers
   - TransactionProcessingState is updated with external IDs and Submitted status
   - Synchronization metadata is updated

## Self-Learning Payee Cleanup System

This subsystem improves transaction data quality by cleaning up messy payee names before they're sent to budget tools.

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

## Technologies

- **Scala 3**: Primary language with pragmatic functional programming
- **ZIO**: Effect system and dependency management
- **PostgreSQL**: Relational database
- **Flyway**: Database migration
- **Magnum**: SQL library with type-safe queries
- **Chimney**: Object-to-object mapping