# Fio Bank Integration Implementation Plan - 2025-04-09

## Current Status

Based on analyzing the existing Fio Bank integration code in the bounded-contexts/fio directory, we have:

1. A well-structured bounded context with:
   - Domain models for FioAccount and FioTransaction
   - Application services (FioImportService interface)
   - Infrastructure components (FioClient, configs, codecs)
   - Service implementation (FioTransactionImportService)

2. The following components are already implemented:
   - HTTP client for Fio Bank API communication using sttp
   - JSON decoders for parsing API responses
   - Base service implementation for importing transactions
   - Code structure follows DDD principles with clean separation of layers

3. The following items are missing or incomplete:
   - No tests for the Fio integration (no bounded-contexts/fio/src/test directory)
   - The FioTransactionImportService has a TODO item for tracking the last imported transaction ID
   - No implementation for account management (creating, updating Fio accounts)
   - No CLI tools or UI components for Fio integration
   - No integration tests with real Fio API

## Implementation Plan

### 1. Test Infrastructure (High Priority)

1. Create unit tests for the existing components:
   - Create `FioCodecsSpec` to test JSON decoding with example responses
   - Create `FioClientSpec` to test HTTP client with stubbed responses
   - Create `FioTransactionImportServiceSpec` to test transaction mapping logic

2. Create integration tests:
   - Create a test suite that conditionally runs against the Fio API when credentials are available
   - Similar approach as with the YNAB integration tests

### 2. Core Functionality Completion (High Priority)

1. Implement last transaction ID tracking:
   - Create a new repository interface `FioImportStateRepository`
   - Implement the repository to store and retrieve the last imported transaction ID
   - Update `FioTransactionImportService.importNewTransactions()` to use this repository

2. Implement account management:
   - Create a `FioAccountRepository` interface and implementation
   - Add methods to `FioImportService` for managing Fio accounts
   - Implement validation for Fio API tokens

3. Enhance error handling:
   - Create specific error types for different Fio API errors
   - Improve error handling in the client implementation
   - Add proper logging throughout the implementation

### 3. CLI Tool for Testing (Medium Priority)

1. Create a CLI tool for Fio integration testing:
   - Similar to the YNAB CLI tool
   - Allow importing transactions for a date range
   - Allow listing Fio accounts
   - Support testing with configuration from environment variables or config file

2. Add documentation for the CLI tool usage

### 4. Web UI Components (Low Priority)

1. Create web UI components for Fio integration:
   - Account management form (add/edit Fio accounts)
   - Transaction import trigger UI
   - Import history/status view

## Implementation Steps

### Phase 1: Test Infrastructure (2 days)

1. Create `/bounded-contexts/fio/src/test/scala/works/iterative/incubator/fio/infrastructure/client/FioCodecsSpec.scala`
   - Test JSON parsing of example Fio API responses
   - Include test data for various transaction types

2. Create `/bounded-contexts/fio/src/test/scala/works/iterative/incubator/fio/infrastructure/client/FioClientSpec.scala`
   - Test client methods with mocked HTTP responses
   - Test error handling for different API response codes
   - Test URL construction for different API endpoints

3. Create `/bounded-contexts/fio/src/test/scala/works/iterative/incubator/fio/infrastructure/service/FioTransactionImportServiceSpec.scala`
   - Test mapping from Fio transactions to domain model
   - Test source account resolution logic
   - Test transaction saving logic

4. Create `/bounded-contexts/fio/it/src/test/scala/works/iterative/incubator/fio/FioIntegrationSpec.scala`
   - Conditionally run tests when Fio API credentials are available
   - Test real API communication
   - Test full import workflow

### Phase 2: Core Functionality (3 days)

1. Create import state tracking components:
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/domain/model/FioImportState.scala`
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/domain/repository/FioImportStateRepository.scala`
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/persistence/PostgreSQLFioImportStateRepository.scala`

2. Update FioTransactionImportService to use the import state repository:
   - Add repository dependency to the service constructor
   - Modify importNewTransactions to retrieve and update the last ID

3. Implement account management:
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/domain/repository/FioAccountRepository.scala`
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/persistence/PostgreSQLFioAccountRepository.scala`
   - Update FioImportService to include account management methods

4. Enhance error handling:
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/domain/model/FioApiError.scala`
   - Update client implementation to map API errors to domain errors

### Phase 3: CLI Tool (2 days)

1. Create a CLI tool for testing the Fio integration:
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/cli/FioCliMain.scala`
   - Implement commands for importing transactions and managing accounts
   - Add configuration loading from environment variables or file

2. Add documentation for the CLI tool:
   - Update README with usage instructions
   - Add example commands

### Phase 4: Web UI (Optional - 3 days)

1. Create web module for Fio integration:
   - Create `/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/web/module/FioModule.scala`
   - Create views for account management and transaction import
   - Integrate with existing UI components

## Risks and Mitigation

1. **Risk**: Fio API changes or inconsistencies
   **Mitigation**: Use integration tests with real API to detect issues early, add comprehensive logging

2. **Risk**: Security concerns with API tokens
   **Mitigation**: Ensure tokens are stored securely, use environment variables for testing

3. **Risk**: Performance issues with large transaction imports
   **Mitigation**: Implement pagination and batch processing, add metrics and monitoring

## Success Criteria

1. All unit tests pass
2. Integration tests pass when Fio API credentials are available
3. Can successfully import transactions from Fio API
4. Last transaction ID tracking works correctly
5. Account management (CRUD operations) works correctly
6. CLI tool functions properly for testing
7. Web UI components are integrated with the main application (if implemented)

## Next Steps After Completion

1. Integrate with AI categorization system
2. Add scheduling for automatic imports
3. Enhance monitoring and alerts for import failures
4. Add reporting features for imported transactions