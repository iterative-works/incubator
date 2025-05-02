# Slice Implementation Plan: VS-001 Basic Transaction Import

## Overview

This document outlines the detailed implementation plan for the Basic Transaction Import vertical slice (VS-001) following our UI-First BDD Development workflow. This slice enables the system to connect to Fio Bank API, retrieve transactions for a specified date range, and store them in the local database.

## Gherkin Scenarios

```gherkin
Feature: Fio Bank Transaction Import

Scenario: Successful import of transactions from Fio Bank
  Given I am on the import page
  When I specify a date range from "2025-04-01" to "2025-04-15"
  And I click the "Import Transactions" button
  Then the system should connect to Fio Bank API
  And retrieve all transactions for the specified date range
  And store them in the database
  And display a success message with the count of imported transactions

Scenario: Import with no new transactions
  Given I am on the import page
  When I specify a date range with no new transactions
  And I click the "Import Transactions" button
  Then the system should connect to Fio Bank API
  And determine there are no new transactions
  And display an appropriate message indicating no new transactions

Scenario: Error during import from Fio Bank
  Given I am on the import page
  When I specify a date range from "2025-04-01" to "2025-04-15"
  And I click the "Import Transactions" button
  And the Fio Bank API is unavailable
  Then the system should display an error message
  And provide retry options
```

## UI-First Implementation Plan

### 1. UI Prototype Development

#### View Models
- **ImportPageViewModel**
  - `startDate`: LocalDate
  - `endDate`: LocalDate
  - `importStatus`: ImportStatus (NotStarted, InProgress, Completed, Error)
  - `importResults`: ImportResults (transactionCount, errorMessage)
  - `isValid`: Boolean (validity of date range)

- **ImportStatus** (Enum)
  - `NotStarted`
  - `InProgress`
  - `Completed`
  - `Error`

- **ImportResults**
  - `transactionCount`: Int
  - `errorMessage`: Option[String]
  - `startTime`: Instant
  - `endTime`: Option[Instant]

#### Mock Data Providers
- **MockImportService**
  - `importTransactions(startDate: LocalDate, endDate: LocalDate): Task[ImportResults]`
  - Simulates successful import, no transactions found, and error scenarios

#### UI Components
1. **DateRangeSelector**
   - Start date picker
   - End date picker
   - Validation for date range (start ≤ end, not in future)

2. **ImportButton**
   - Enabled/disabled based on date range validity
   - Loading state during import process

3. **StatusIndicator**
   - Visual representation of import status
   - Progress animation during import

4. **ResultsPanel**
   - Displays count of imported transactions
   - Shows error messages when applicable
   - Provides retry button when errors occur

### 2. UI Testing

1. **Component Tests**
   - Test date range selector validation
   - Test button state changes based on form validity
   - Test status indicator for different states

2. **User Flow Tests**
   - Test complete happy path scenario
   - Test error scenario handling
   - Test empty results scenario

### 3. User Experience Validation

1. **User Testing Session**
   - Present UI prototype to finance team members
   - Observe interaction with the import flow
   - Collect feedback on usability and clarity

2. **UX Refinement Tasks**
   - Address any usability issues identified
   - Implement any missing features discovered
   - Refine error messages and status indicators

### 4. Domain Discovery & Implementation

#### Domain Entities

1. **Transaction**
   ```scala
   case class Transaction(
     id: TransactionId,
     accountId: AccountId,
     date: LocalDate,
     amount: Money,
     description: String,
     counterparty: Option[String],
     reference: Option[String],
     importBatchId: ImportBatchId,
     status: TransactionStatus
   )
   ```

2. **ImportBatch**
   ```scala
   case class ImportBatch(
     id: ImportBatchId,
     startDate: LocalDate,
     endDate: LocalDate,
     status: ImportStatus,
     createdAt: Instant,
     completedAt: Option[Instant],
     transactionCount: Int,
     errorMessage: Option[String]
   )
   ```

#### Domain Services

1. **TransactionImportService**
   ```scala
   trait TransactionImportService:
     def importTransactions(startDate: LocalDate, endDate: LocalDate): IO[ImportError, ImportBatch]
   ```

2. **FioBankService**
   ```scala
   trait FioBankService:
     def fetchTransactions(startDate: LocalDate, endDate: LocalDate): IO[FioBankError, List[FioBankTransaction]]
   ```

#### Repository Interfaces

1. **TransactionRepository**
   ```scala
   trait TransactionRepository:
     def saveAll(transactions: List[Transaction]): IO[RepositoryError, Unit]
     def findByImportBatch(batchId: ImportBatchId): IO[RepositoryError, List[Transaction]]
   ```

2. **ImportBatchRepository**
   ```scala
   trait ImportBatchRepository:
     def save(batch: ImportBatch): IO[RepositoryError, ImportBatch]
     def findById(id: ImportBatchId): IO[RepositoryError, Option[ImportBatch]]
     def findByDateRange(startDate: LocalDate, endDate: LocalDate): IO[RepositoryError, List[ImportBatch]]
   ```

### 5. Infrastructure Implementation

#### Repository Implementations

1. **PostgreSQLTransactionRepository**
   - Implements TransactionRepository interface
   - Uses JDBC or Quill for database access
   - Includes proper error handling and mapping

2. **PostgreSQLImportBatchRepository**
   - Implements ImportBatchRepository interface
   - Handles batch metadata storage and retrieval

#### External Service Implementations

1. **FioBankApiClient**
   - Implements FioBankService interface
   - Handles HTTP requests to Fio Bank API
   - Manages authentication and error handling
   - Maps API responses to domain entities

#### Database Schema

1. **transactions Table**
   ```sql
   CREATE TABLE transactions (
     id UUID PRIMARY KEY,
     account_id UUID NOT NULL,
     transaction_date DATE NOT NULL,
     amount DECIMAL(19, 4) NOT NULL,
     description TEXT NOT NULL,
     counterparty TEXT,
     reference TEXT,
     import_batch_id UUID NOT NULL,
     status VARCHAR(20) NOT NULL,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
     FOREIGN KEY (import_batch_id) REFERENCES import_batches(id)
   );
   ```

2. **import_batches Table**
   ```sql
   CREATE TABLE import_batches (
     id UUID PRIMARY KEY,
     start_date DATE NOT NULL,
     end_date DATE NOT NULL,
     status VARCHAR(20) NOT NULL,
     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
     completed_at TIMESTAMP WITH TIME ZONE,
     transaction_count INTEGER NOT NULL DEFAULT 0,
     error_message TEXT
   );
   ```

### 6. Integration & Testing

#### End-to-End Tests

1. **Successful Import Test**
   - Test complete flow from UI to database
   - Verify transactions are correctly stored
   - Validate import batch metadata

2. **Error Handling Test**
   - Test system response to API failures
   - Verify error messages are displayed correctly
   - Test retry functionality

3. **Performance Test**
   - Test import of 100+ transactions
   - Verify performance meets requirements

#### Monitoring & Logging

1. **Structured Logging**
   - Log import initiation, progress, completion
   - Log API interaction details
   - Log errors with context for troubleshooting

2. **Metrics Collection**
   - Track import duration
   - Track transaction counts
   - Monitor error rates

## Implementation Tasks

1. **UI Implementation (3 days)**
   - [ ] Create DateRangeSelector component
   - [ ] Implement ImportButton with state management
   - [ ] Build StatusIndicator component
   - [ ] Develop ResultsPanel component
   - [ ] Integrate components into ImportPage
   - [ ] Implement UI-level validation
   - [ ] Create mock data providers for testing

2. **User Testing (1 day)**
   - [ ] Prepare UI prototype
   - [ ] Conduct user testing session
   - [ ] Document feedback
   - [ ] Implement UX refinements

3. **Domain Implementation (2 days)**
   - [ ] Define domain entities and value objects
   - [ ] Implement TransactionImportService
   - [ ] Create FioBankService interface
   - [ ] Define repository interfaces
   - [ ] Write domain-level tests

4. **Infrastructure Implementation (3 days)**
   - [ ] Create database schema migrations
   - [ ] Implement PostgreSQL repositories
   - [ ] Develop FioBankApiClient
   - [ ] Write integration tests
   - [ ] Set up error handling

5. **Integration & Testing (2 days)**
   - [ ] Connect UI to domain services
   - [ ] Connect domain services to repositories
   - [ ] Implement end-to-end tests
   - [ ] Set up monitoring and logging
   - [ ] Perform performance testing

## Technical Risks and Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Fio Bank API changes | Medium | High | Create abstraction layer; comprehensive tests |
| Rate limiting on API | High | Medium | Implement backoff strategy; batch processing |
| Complex data mapping | Medium | Medium | Create mapping utility class; unit test edge cases |
| Performance with large imports | Low | High | Implement paging; background processing |
| Transaction data integrity | Low | High | Database constraints; validation checks |

## Dependencies

- Fio Bank API credentials
- Fio Bank API documentation
- Database access configuration
- ZIO framework knowledge
- Scala 3 syntax familiarity

## Deliverables

1. Working UI prototype
2. User testing results
3. Domain model implementation
4. Infrastructure implementation
5. End-to-end tested vertical slice
6. Documentation of implementation

## Next Steps

1. Begin UI prototype development for the DateRangeSelector component
2. Set up mock data providers for testing
3. Schedule initial user feedback session

## Document Information

- **Created**: [Current Date]
- **Author**: Team
- **Status**: Draft
- **Related Documents**:
  - [Vertical Slice Plan](vertical_slice_plan.md)
  - [Feature Specification](feature.md)