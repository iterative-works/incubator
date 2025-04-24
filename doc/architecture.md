# YNAB Importer Architecture

## Overview
The YNAB Importer follows a BDD-Driven UI-First approach with a Functional Core/Imperative Shell architecture across multiple bounded contexts. The application imports bank transactions from various sources, processes them with AI-assisted categorization, and exports them to YNAB (You Need A Budget). Our development is scenario-driven, using Gherkin features as the core of our workflow.

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

## Bounded Context Architecture

Our application is organized into the following bounded contexts:

### Budget (Shared Kernel)
- Core domain concepts shared across all contexts
- Repository interfaces for shared entities
- Domain events that flow between contexts

### Imports Context
- Bank integration adapters (currently Fio, future Revolut)
- Bank-specific models and mappings
- Import workflows and validation
- Anticorruption layer between bank APIs and our domain

### Categorization Context
- AI categorization services
- Category rules and management
- Payee cleanup system
- Category suggestion engines

### Submission Context
- Budget tool integrations (currently YNAB)
- Budget-specific models and mappings 
- Submission workflows and validation
- Anticorruption layer between budget APIs and our domain

### Auth Context (Future)
- User authentication and authorization
- Role and permission management
- Security infrastructure

## Package Structure

Our package structure reflects the bounded context organization:

```
works.iterative.incubator/
├── budget/                   # Shared Kernel
│   ├── domain/               # Domain models shared across contexts
│   │   ├── model/            # Core domain entities
│   │   ├── repository/       # Repository interfaces
│   │   ├── event/            # Domain events
│   │   └── query/            # Query models
│   └── infrastructure/       # Shared infrastructure implementations
│       └── persistence/      # Shared repository implementations
│
├── imports/                  # Imports Context (Generic)
│   ├── domain/               # Import-specific domain logic
│   │   ├── model/            # Import-specific models
│   │   ├── service/          # Domain services
│   │   └── port/             # Ports (interfaces) to infrastructure
│   │
│   ├── application/          # Application services
│   │   ├── service/          # Use case implementations
│   │   └── port/             # Application service interfaces
│   │
│   ├── infrastructure/       # Infrastructure components
│   │   ├── config/           # Import configuration
│   │   ├── security/         # Security components for imports
│   │   └── persistence/      # Import-specific repository implementations
│   │
│   ├── adapters/             # Bank-specific adapters
│   │   ├── fio/              # Fio Bank adapter
│   │   │   ├── client/       # Fio API client
│   │   │   ├── model/        # Fio-specific models
│   │   │   ├── mapper/       # Mappers from Fio models to domain models
│   │   │   ├── service/      # Fio-specific services
│   │   │   └── config/       # Fio-specific configuration
│   │   │
│   │   └── revolut/          # Future Revolut adapter (similar structure)
│   │
│   └── web/                  # Import web UI
│       ├── module/           # Module definition
│       ├── view/             # Views
│       ├── controller/       # Controllers/Routes
│       └── dto/              # Data transfer objects
│
├── categorization/           # Categorization Context
│   ├── domain/
│   │   ├── model/            # Categorization models
│   │   ├── service/          # Domain services
│   │   └── port/             # Ports to infrastructure
│   │
│   ├── application/
│   │   ├── service/          # AI categorization, Manual categorization, etc.
│   │   └── port/             # Application service interfaces
│   │
│   ├── infrastructure/
│   │   ├── ai/               # AI integration components
│   │   │   ├── client/       # AI service clients
│   │   │   └── mapper/       # AI response mappers
│   │   │
│   │   ├── persistence/      # Repository implementations
│   │   └── config/           # Categorization configuration
│   │
│   └── web/
│       ├── module/           # Module definition (AIModule, ManualCategoryModule)
│       ├── view/             # Views
│       ├── controller/       # Controllers/Routes
│       └── dto/              # Data transfer objects
│
├── submission/               # Submission Context (Generic)
│   ├── domain/
│   │   ├── model/            # Submission models
│   │   ├── service/          # Domain services
│   │   └── port/             # Ports to infrastructure
│   │
│   ├── application/
│   │   ├── service/          # Submission workflows
│   │   └── port/             # Application service interfaces
│   │
│   ├── infrastructure/
│   │   ├── config/           # Submission configuration
│   │   └── persistence/      # Repository implementations
│   │
│   ├── adapters/             # Budget tool adapters
│   │   └── ynab/             # YNAB adapter
│   │       ├── client/       # YNAB API client
│   │       ├── model/        # YNAB-specific models
│   │       ├── mapper/       # Mappers from domain models to YNAB models
│   │       ├── service/      # YNAB-specific services
│   │       └── config/       # YNAB-specific configuration
│   │
│   └── web/
│       ├── module/           # Module definitions (YNABSubmissionModule)
│       ├── view/             # Views
│       ├── controller/       # Controllers/Routes
│       └── dto/              # Data transfer objects
│
├── transactions/             # Transaction Management Context
│   ├── domain/
│   │   ├── model/
│   │   ├── service/
│   │   └── port/
│   │
│   ├── application/
│   │   ├── service/          # Transaction filtering, searching, etc.
│   │   └── port/
│   │
│   ├── infrastructure/
│   │   ├── persistence/      # Repository implementations
│   │   └── config/
│   │
│   └── web/
│       ├── module/           # Module definitions (TransactionFilterModule)
│       ├── view/             # Views
│       ├── controller/       # Controllers/Routes
│       └── dto/              # Data transfer objects
│
├── auth/                     # Auth Context (Future)
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── web/
│
└── server/                   # Application bootstrap and server configuration
    ├── config/               # Server configuration
    ├── http/                 # HTTP server setup
    ├── module/               # Module registry and composition
    └── Main.scala            # Application entry point
```

## Context Relationships

- **Budget ← Imports**: Imports context uses Budget's domain model to store imported transactions
- **Budget ← Categorization**: Categorization context updates Budget's TransactionProcessingState
- **Budget ← Submission**: Submission context uses Budget's model to find transactions to submit
- **Imports ⟷ Categorization**: Sequential workflow where imports trigger categorization
- **Categorization ⟷ Submission**: Sequential workflow where categorization enables submission

## Vertical Slice Modules

Within each bounded context, we organize modules into vertical slices aligned with Gherkin scenarios:

### Imports Context Modules
- ImportModule (Generic UI for all import sources)
- Specific bank adapters:
  - FioAdapter (Maps to scenarios 1, 9)
  - Future bank adapters (Revolut, etc.)

### Categorization Context Modules
- AICategoryModule (Maps to scenario 2)
- ManualCategoryModule (Maps to scenarios 3, 4)

### Submission Context Modules
- SubmissionModule (Generic UI for all budget tool submissions)
- Specific budget tool adapters:
  - YNABAdapter (Maps to scenarios 5, 6, 7)
  - Future budget tool adapters

### Transaction Management Modules
- TransactionFilterModule (Maps to scenario 8)

## Domain Model Design

### Event-Centric Approach

The core of our design in the Budget shared kernel is an event-centric model that treats transactions as immutable events:

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

Each bounded context follows the Functional Core/Imperative Shell pattern:

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

Each bounded context follows our Functional MVP pattern with this general structure:

```
context/                   # A specific bounded context (e.g., imports, categorization)
├── domain/                 # Context-specific domain logic
│   ├── model/              # Domain models
│   ├── service/            # Domain services
│   └── port/               # Ports (interfaces) to infrastructure
├── application/            # Application Services
│   ├── service/            # Application service implementations
│   └── port/               # Application service interfaces
├── infrastructure/         # Infrastructure components
│   ├── config/             # Configuration
│   ├── security/           # Security components
│   └── persistence/        # Repository implementations
├── adapters/               # External system adapters (when applicable)
│   └── specific-adapter/    # Adapter for a specific external system
│       ├── client/         # API client
│       ├── model/          # External system-specific models
│       ├── mapper/         # Model mappers
│       ├── service/        # Adapter-specific services
│       └── config/         # Adapter-specific configuration
└── web/                    # UI components
    ├── module/             # Module definitions
    ├── view/               # Views
    ├── controller/         # Controllers/Routes
    └── dto/                # Data transfer objects
```

For example, our imports context with a specific adapter for Fio Bank would have this structure:

```
imports/                   # Imports Context
├── domain/                 # Import-specific domain logic
├── application/            # Import application services 
├── infrastructure/         # Infrastructure components
├── adapters/               # Bank-specific adapters
│   └── fio/                # Fio Bank adapter
│       ├── client/         # Fio API client
│       ├── model/          # Fio-specific models
│       ├── mapper/         # Mappers from Fio models to domain models
│       ├── service/        # Fio-specific services
│       └── config/         # Fio-specific configuration
└── web/                    # Import web UI
```

Similarly, our submission context with a YNAB adapter would follow this pattern:

```
submission/                # Submission Context
├── domain/                 # Submission-specific domain logic
├── application/            # Submission application services
├── infrastructure/         # Infrastructure components
├── adapters/               # Budget tool adapters
│   └── ynab/               # YNAB adapter
│       ├── client/         # YNAB API client
│       ├── model/          # YNAB-specific models
│       ├── mapper/         # Mappers from domain models to YNAB models
│       ├── service/        # YNAB-specific services
│       └── config/         # YNAB-specific configuration
└── web/                    # Submission web UI
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

## Scenario-to-Context Mapping

Based on our BDD approach, we map scenarios to contexts:

| Context | Adapter | Scenarios | Key Functionality |
|---------|---------|-----------|-------------------|
| Imports | Generic | - | Generic import infrastructure |
| Imports | Fio | 1, 9 | Import transactions from Fio Bank, Date range validation |
| Categorization | - | 2, 3, 4 | AI categorization, Manual modification, Bulk modification |
| Submission | Generic | - | Generic submission infrastructure |
| Submission | YNAB | 5, 6, 7 | Submit to YNAB, Error handling, Duplicate prevention |
| Transaction Management | - | 8 | Filtering and viewing transactions |

## Feature File Organization

To align with our bounded context approach, our Gherkin feature files are organized by context:

```
features/
├── budget/                # Shared domain scenarios
├── imports/               # Import scenarios 
│   ├── common/             # Generic import features
│   └── adapters/           # Bank-specific features
│       └── fio_import.feature  # Fio-specific scenarios (1, 9)
├── categorization/        # Categorization scenarios (scenarios 2, 3, 4)
│   └── transaction_categorization.feature
├── submission/            # Submission scenarios
│   ├── common/             # Generic submission features
│   └── adapters/           # Budget tool-specific features
│       └── ynab_submission.feature  # YNAB-specific scenarios (5, 6, 7)
└── management/            # Management scenarios (scenario 8)
    └── transaction_management.feature
```

## Implementation Process

For each bounded context, our implementation follows this process:

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

## Cross-Context Communication

Contexts communicate through these mechanisms:

1. **Repository Access**: Contexts can access Budget shared kernel repositories
2. **Domain Events**: Events published by one context and consumed by others
3. **Service Interfaces**: Defined in the Budget shared kernel and implemented in specific contexts
4. **Integration Ports**: Handler interfaces for cross-context workflows

## Processing Flow

Our end-to-end transaction processing flow crosses multiple bounded contexts:

1. **Import Phase** (Imports Context)
   - External bank data is fetched through specific bank adapters (e.g., Fio Bank adapter)
   - Adapter-specific data is transformed into domain model using mappers
   - Raw data is transformed into immutable Transaction events
   - Initial TransactionProcessingState is created with Imported status

2. **Processing Phase** (Categorization Context)
   - AI categorization is applied to transactions
   - Self-learning payee name cleanup is applied to transactions
   - User can review and override categorizations
   - TransactionProcessingState is updated to Categorized status

3. **Export Phase** (Submission Context)
   - Categorized transactions are submitted to budget tools through specific adapters (e.g., YNAB adapter)
   - Domain models are transformed to adapter-specific models using mappers
   - TransactionProcessingState is updated with external IDs and Submitted status
   - Synchronization metadata is updated

## Self-Learning Payee Cleanup System

This subsystem in the Categorization context improves transaction data quality by cleaning up messy payee names before they're sent to YNAB.

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