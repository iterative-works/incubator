# YNAB Importer Architecture

## Overview
The YNAB Importer follows a Functional Core/Imperative Shell architecture that imports bank transactions from various sources and exports them to YNAB (You Need A Budget). The application implements an event-centric approach with clear separation between immutable transaction events and mutable processing state.

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
- **Adapters**: External service integration
  - FIO bank adapter (`FioClient`, `FioTransactionImportService`)
- **Database Components**:
  - `PostgreSQLTransactor`, `PostgreSQLDataSource`
  - `FlywayMigrationService` for schema management

### Web Layer
- **API Module**: Transaction import/export endpoints
- **User Interface**: Managing and categorizing transactions

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

## Implementation Patterns

### Effect Management with ZIO
- Pure domain logic with effect-tracked IO operations
- ZIO Environment for dependency injection
- ZLayer for composable component dependencies

### Repository Pattern
- Domain-defined interfaces in the core
- Infrastructure implementations in the shell
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

1. **Import Phase**
   - External bank data is fetched through adapters (e.g., FIO Bank)
   - Raw data is transformed into immutable Transaction events
   - Initial TransactionProcessingState is created with Imported status

2. **Processing Phase**
   - AI categorization is applied to transactions
   - User can review and override categorizations
   - TransactionProcessingState is updated to Categorized status

3. **Export Phase**
   - Categorized transactions are submitted to YNAB
   - TransactionProcessingState is updated with YNAB IDs and Submitted status
   - Synchronization metadata is updated

## Technologies

- **Scala 3**: Primary language with pragmatic functional programming
- **ZIO**: Effect system and dependency management
- **PostgreSQL**: Relational database
- **Flyway**: Database migration
- **Magnum**: SQL library with type-safe queries
- **Chimney**: Object-to-object mapping