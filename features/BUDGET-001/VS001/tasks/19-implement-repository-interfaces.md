# BDD-Driven Implementation Step: Implement Repository Interfaces for Transactions and Import Batches

As my AI development partner, I need your help implementing step 19: Implement Repository Interfaces for Transactions and Import Batches from our feature implementation plan for BUDGET-001-VS001.

## Workflow Setup
Before we begin implementation, let's set up our working environment:

1. **Branch Management**:
   - Check if a branch named `feature/BUDGET-001-VS001-step19` already exists
   - If it doesn't exist, create this branch from the branch `feature/BUDGET-001-VS001`
   - Switch to this branch for our implementation

## Component Details

- **Component Type**: Repository Implementation
- **Component Name**: `PostgreSQLTransactionRepository` and `PostgreSQLImportBatchRepository`
- **Package Location**: `works.iterative.incubator.budget.infrastructure.persistence`
- **Purpose**: Create persistent database implementations of the `TransactionRepository` and `ImportBatchRepository` interfaces that were previously defined in the domain layer. These implementations will store transactions and import batches in a PostgreSQL database, allowing for persistent data storage and retrieval.
- **Key Behaviors**:
  - Implement all methods defined in the `TransactionRepository` interface
  - Implement all methods defined in the `ImportBatchRepository` interface
  - Translate between domain entities and database DTOs
  - Handle database-specific errors and map them to domain errors
  - Ensure thread-safety using ZIO constructs
  - Optimize for performance with appropriate indexing and query strategies
  - Support transactional operations when needed
- **Dependencies**:
  - `TransactionRepository` and `ImportBatchRepository` domain interfaces
  - Domain entities: `Transaction`, `ImportBatch`, `AccountId`, `ImportBatchId`, `TransactionId`
  - Magnum library for database access
  - ZIO for effect management
  - Chimney for object-to-object mapping
  - Flyway for database migrations
- **Acceptance Criteria**:
  - Successfully implements all methods from the domain repository interfaces
  - Properly translates between domain entities and database DTOs
  - Handles database errors and maps them to domain-specific errors
  - Supports thread-safe concurrent operations using ZIO Ref or transactions
  - Uses appropriate database indices for optimized queries
  - Follows functional programming principles with ZIO effect management
  - Includes comprehensive database schema migration scripts
- **Implementation Guide** - these guides are in obsidian vault, use get_vault_file to retrieve these, and please do:
  - [Repository Implementation Guide](/+Encounters/architecture/guides/repository_implementation_guide.md) - For domain-driven repository implementation
  - [IW Support SQL DB Guide](/Docs/iw_sqldb_support_guide.md) - Detailed guide on how we do the SQL DB access and integration

## Supported Scenarios

This component supports these scenarios from the feature file:

```gherkin
Scenario: Successfully import transactions for a date range
  Given I am on the transaction import page
  When I select "2025-04-01" as the start date
  And I select "2025-04-15" as the end date
  And I click the "Import Transactions" button
  Then I should see a progress indicator with status "Connecting to Fio Bank"
  And then the status should change to "Retrieving transactions"
  And then the status should change to "Storing transactions"
  And finally I should see a summary showing "15 transactions successfully imported"
  And the transactions should appear in the transaction list with "Imported" status
  And the import history should be updated with this import session

Scenario: Import with no transactions available
  Given I am on the transaction import page
  When I select "2025-06-01" as the start date
  And I select "2025-06-02" as the end date
  And I click the "Import Transactions" button
  Then the system should connect to Fio Bank API
  And after the import completes, I should see a message "No transactions found for the selected date range"
  And the import history should be updated with this import session marked as "No transactions"

Scenario: Handle Fio Bank API connection failure
  Given I am on the transaction import page
  And the Fio Bank API is temporarily unavailable
  When I select "2025-04-01" as the start date
  And I select "2025-04-15" as the end date
  And I click the "Import Transactions" button
  Then I should see an error message "Unable to connect to Fio Bank. Please try again later."
  And I should see a "Retry" button
  And the import should not be recorded in the import history
```

## Implementation Requirements

### Domain Model Requirements
| Component | Behavior | Connected Scenarios |
|-----------|----------|---------------------|
| PostgreSQLTransactionRepository | Store imported transactions | All scenarios |
| PostgreSQLTransactionRepository | Retrieve transactions by import batch | "Successfully import" scenario |
| PostgreSQLTransactionRepository | Support concurrent operations | All scenarios |
| PostgreSQLImportBatchRepository | Store import batch information | All scenarios |
| PostgreSQLImportBatchRepository | Update import batch status | All scenarios |
| PostgreSQLImportBatchRepository | Track import history | All scenarios |

### Database Schema
Create Flyway migration scripts that define the following tables:

1. **transactions Table**
```sql
CREATE TABLE transactions (
  id VARCHAR(100) PRIMARY KEY,
  source_account_id VARCHAR(50) NOT NULL,
  transaction_date DATE NOT NULL,
  amount_value DECIMAL(19, 4) NOT NULL,
  amount_currency VARCHAR(3) NOT NULL,
  description TEXT NOT NULL,
  counterparty TEXT,
  counter_account TEXT,
  reference TEXT,
  import_batch_id VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  FOREIGN KEY (import_batch_id) REFERENCES import_batches(id)
);

CREATE INDEX idx_transactions_source_account_date
  ON transactions(source_account_id, transaction_date);
CREATE INDEX idx_transactions_import_batch
  ON transactions(import_batch_id);
CREATE INDEX idx_transactions_status
  ON transactions(status);
```

2. **import_batches Table**
```sql
CREATE TABLE import_batches (
  id VARCHAR(100) PRIMARY KEY,
  account_id VARCHAR(50) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  transaction_count INTEGER NOT NULL DEFAULT 0,
  error_message TEXT,
  start_time TIMESTAMP WITH TIME ZONE NOT NULL,
  end_time TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_import_batches_account_id
  ON import_batches(account_id);
CREATE INDEX idx_import_batches_dates
  ON import_batches(start_date, end_date);
CREATE INDEX idx_import_batches_status
  ON import_batches(status);
```

### Implementation Risks and Considerations
- Concurrent operations: Ensure thread safety with proper transaction boundaries
- Error handling: Map database-specific errors to domain errors
- Performance: Optimize for bulk transaction imports
- Proper indexing for frequent query patterns
- Migration script completeness and correctness

## Technical Details

### Data Transfer Objects (DTOs)
Create DTO classes with appropriate Magnum annotations:

```scala
@SqlName("transactions")
@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class TransactionDTO(
  id: String,
  sourceAccountId: String,
  transactionDate: LocalDate,
  amountValue: BigDecimal,
  amountCurrency: String,
  description: String,
  counterparty: Option[String],
  counterAccount: Option[String],
  reference: Option[String],
  importBatchId: String,
  status: String,
  createdAt: Instant,
  updatedAt: Instant
)

@SqlName("import_batches")
@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class ImportBatchDTO(
  id: String,
  accountId: String,
  startDate: LocalDate,
  endDate: LocalDate,
  status: String,
  transactionCount: Int,
  errorMessage: Option[String],
  startTime: Instant,
  endTime: Option[Instant],
  createdAt: Instant,
  updatedAt: Instant
)
```

### Mappers
Create bidirectional mappers between domain entities and DTOs:

```scala
object TransactionMapper:
  def toDTO(entity: Transaction): TransactionDTO = /* mapping logic */
  def toDomain(dto: TransactionDTO): Transaction = /* mapping logic */

object ImportBatchMapper:
  def toDTO(entity: ImportBatch): ImportBatchDTO = /* mapping logic */
  def toDomain(dto: ImportBatchDTO): ImportBatch = /* mapping logic */
```

## Implementation Structure
The implementation should consist of the following components:

1. **PostgreSQLTransactionRepository**
   - Implements the `TransactionRepository` interface
   - Uses Magnum for database access
   - Translates between domain entities and DTOs

2. **PostgreSQLImportBatchRepository**
   - Implements the `ImportBatchRepository` interface
   - Uses Magnum for database access
   - Translates between domain entities and DTOs

3. **Database Migration Scripts**
   - V1__create_import_batches.sql
   - V2__create_transactions.sql

4. **Repository Layers**
   - ZLayer definitions for providing repository implementations

## Test Considerations
1. Unit tests for DTO mappers
2. Unit tests for repository implementations with a test database
3. Integration tests with a real PostgreSQL database
4. Performance tests for bulk operations

## Current Project State

We have already implemented the domain model entities and interfaces, as well as mock implementations for testing UI components. The Fio Bank adapter has been implemented from the previous task (#18) and now we need to implement the repository interfaces to store the imported data persistently in PostgreSQL.

## Implementation Plan

1. Create Flyway migration scripts for database schema
2. Implement DTO classes and mapper functions
3. Implement the PostgreSQLTransactionRepository
4. Implement the PostgreSQLImportBatchRepository
5. Create ZLayer definitions for the repositories
6. Add unit and integration tests

## Estimated Effort
- 1 day for initial implementation
- 0.5 day for database schema design and migration scripts
- 0.5 day for unit tests
- 0.5 day for integration with existing components

## Next Steps After Implementation
1. Connect repositories to the domain services
2. Integrate with the Fio Bank infrastructure adapter
3. Integrate with the UI components
4. Set up proper logging and monitoring

## Expected workflow

1. Setup the branch
2. Implement the changes in the task description
3. Make sure compilation works with `sbtn compile` and fix any errors
4. Make sure tests work with `sbtn test` and fix any errors
5. Make sure there are no warnings with `sbtn printWarnings` and fix these, if any
6. Reformat the code using `sbt scalafmtAll`
