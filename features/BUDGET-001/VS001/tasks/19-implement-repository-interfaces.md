# Task 19: Implement Repository Interfaces for Transactions and Import Batches

## Component Type
Repository Implementation

## Component Name
`PostgreSQLTransactionRepository` and `PostgreSQLImportBatchRepository`

## Package Location
`works.iterative.incubator.budget.infrastructure.persistence`

## Purpose
Create persistent database implementations of the `TransactionRepository` and `ImportBatchRepository` interfaces that were previously defined in the domain layer. These implementations will store transactions and import batches in a PostgreSQL database, allowing for persistent data storage and retrieval.

## Key Behaviors
1. Implement all methods defined in the `TransactionRepository` interface
2. Implement all methods defined in the `ImportBatchRepository` interface
3. Translate between domain entities and database DTOs
4. Handle database-specific errors and map them to domain errors
5. Ensure thread-safety using ZIO constructs
6. Optimize for performance with appropriate indexing and query strategies
7. Support transactional operations when needed

## Dependencies
1. `TransactionRepository` and `ImportBatchRepository` domain interfaces
2. Domain entities: `Transaction`, `ImportBatch`, `AccountId`, `ImportBatchId`, `TransactionId`
3. Magnum library for database access
4. ZIO for effect management
5. Chimney for object-to-object mapping
6. Flyway for database migrations

## Acceptance Criteria
1. Successfully implements all methods from the domain repository interfaces
2. Properly translates between domain entities and database DTOs
3. Handles database errors and maps them to domain-specific errors
4. Supports thread-safe concurrent operations using ZIO Ref or transactions
5. Uses appropriate database indices for optimized queries
6. Follows functional programming principles with ZIO effect management
7. Includes comprehensive database schema migration scripts

## Implementation Guide
Follow the Repository pattern with a focus on:
1. Clean separation between domain models and database DTOs
2. Use of type-safe SQL libraries (Magnum) for database access
3. Appropriate error handling and mapping to domain errors
4. Transaction management for atomic operations
5. Proper indexing for performance optimization

## Relevant Scenarios
These repositories support all scenarios in the feature file, particularly:
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
```

## Technical Details
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

## Performance Considerations
1. Use appropriate indices for frequently queried fields
2. Batch database operations where possible
3. Consider pagination for large result sets
4. Use optimistic locking for concurrent modifications
5. Implement caching for frequently accessed data

## Test Plan
1. Unit tests for DTO mappers
2. Unit tests for repository implementations with a test database
3. Integration tests with a real PostgreSQL database
4. Performance tests for bulk operations

## Estimated Effort
- 1 day for initial implementation
- 0.5 day for database schema design and migration scripts
- 0.5 day for unit tests
- 0.5 day for integration with existing components

## Next Steps After Implementation
1. Implement the Fio Bank infrastructure adapter
2. Connect repositories to the domain services
3. Integrate with the UI components
4. Set up proper logging and monitoring