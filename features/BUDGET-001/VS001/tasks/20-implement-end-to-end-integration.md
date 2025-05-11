# Task 20: Implement End-to-End Integration

## Component Type
Integration

## Component Name
`TransactionImportModule`

## Package Location
`works.iterative.incubator.budget.web.module`

## Purpose
Create the complete end-to-end integration by connecting the UI components to the domain services and infrastructure components. This task brings together all previously implemented components (UI, domain, and infrastructure) to deliver the full transaction import feature.

## Key Behaviors
1. Connect UI components to domain services through the presenter
2. Configure the ZIO environment with all required dependencies
3. Wire PostgreSQL repositories to domain services
4. Connect Fio Bank adapter to the transaction import service
5. Set up error handling and recovery at the UI level
6. Implement progress tracking and status updates
7. Ensure complete scenario coverage

## Dependencies
1. UI components: `DateRangeSelector`, `ImportButton`, `StatusIndicator`, `ResultsPanel`
2. `TransactionImportPresenter` and its implementation
3. Domain services: `TransactionImportService`, `BankTransactionService`
4. Infrastructure adapters: `FioBankTransactionService`
5. Repository implementations: `PostgreSQLTransactionRepository`, `PostgreSQLImportBatchRepository`
6. Configuration utilities for database and API connections

## Acceptance Criteria
1. All scenarios from the feature file can be executed end-to-end
2. UI updates with appropriate status messages during import process
3. Progress indicators correctly reflect the current operation
4. Successful imports are stored in the database
5. Error states are properly handled with user-friendly messages
6. All user interactions work as expected
7. The system meets performance requirements for importing transactions

## Implementation Guide
Follow the modular integration approach with a focus on:
1. Clean separation of concerns
2. ZLayer-based dependency injection
3. Proper error handling and recovery
4. Comprehensive logging
5. Responsive UI feedback

## Relevant Scenarios
All scenarios from the feature file should be supported, in particular:

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

## Technical Details
### Module Integration
1. **ZIO Environment Configuration**
   ```scala
   val liveLayer: ZLayer[Any, Nothing, TransactionImportModule] =
     ZLayer.make[TransactionImportModule](
       // UI Layer
       TransactionImportPresenterLive.layer,
       
       // Domain Layer
       TransactionImportService.live,
       
       // Infrastructure Layer
       FioBankTransactionService.layer,
       PostgreSQLTransactionRepository.layer,
       PostgreSQLImportBatchRepository.layer,
       DatabaseConfig.live
     )
   ```

2. **HTTP Route Configuration**
   ```scala
   val routes: HttpApp[TransactionImportModule] =
     HttpRoute.collect {
       case Method.GET -> Root / "import" =>
         // Render import page
         
       case req @ Method.POST -> Root / "import" / "submit" =>
         // Handle import form submission
         
       case Method.GET -> Root / "import" / "status" / batchId =>
         // Handle status polling
     }
   ```

3. **Event Handling Configuration**
   ```scala
   val importForm = ImportForm(
     onSubmit = formData => {
       // Trigger import process
       // Return immediate response
       // Setup status polling
     },
     onStatusUpdate = status => {
       // Update UI components
     },
     onError = error => {
       // Show error message
       // Provide retry option
     }
   )
   ```

4. **Progress Tracking**
   ```scala
   def importTransactions(startDate: LocalDate, endDate: LocalDate): ZIO[TransactionImportModule, Throwable, ImportResult] =
     for {
       // Update UI: Connecting
       _ <- updateStatus(ImportStatus.Connecting)
       
       // Perform import operation
       importBatch <- TransactionImportService.importTransactions(startDate, endDate)
         .tap(_ => updateStatus(ImportStatus.Retrieving))
         .tapError(err => updateStatus(ImportStatus.Error(err.toString)))
         
       // Update UI: Completed
       _ <- updateStatus(ImportStatus.Completed(importBatch.transactionCount))
       
       // Return result
       result = ImportResult(importBatch)
     } yield result
   ```

### Error Handling
Implement centralized error handling that:
1. Displays user-friendly error messages
2. Logs detailed error information for debugging
3. Provides appropriate recovery options (e.g., retry button)
4. Distinguishes between different error types (API errors, validation errors, system errors)

### Performance Optimization
1. Use a background worker for long-running import operations
2. Implement efficient status polling
3. Optimize database operations for bulk inserts
4. Consider caching for frequently accessed data

## Implementation Structure
The implementation should consist of the following components:

1. **TransactionImportModule**
   - Main integration module
   - Configures ZIO environment
   - Defines HTTP routes

2. **TransactionImportController**
   - Handles HTTP requests
   - Calls domain services
   - Returns appropriate responses

3. **Integration Configuration**
   - Database connection setup
   - API client configuration
   - Environment configuration

4. **Background Worker**
   - Handles long-running import operations
   - Updates status in database
   - Manages concurrent operations

## Monitoring and Diagnostics
1. Implement comprehensive logging throughout the integration
2. Add timing metrics for performance monitoring
3. Log key events (import start, completion, errors)
4. Create diagnostic endpoints for troubleshooting

## Test Plan
1. End-to-end tests for all scenarios
2. Performance tests with various transaction volumes
3. Error handling tests with simulated failures
4. Concurrency tests with multiple imports

## Estimated Effort
- 1 day for UI-service integration
- 0.5 day for service-repository integration
- 0.5 day for error handling and recovery
- 1 day for testing and refinement

## Next Steps After Implementation
1. Create automated end-to-end tests
2. Set up monitoring and alerting
3. Prepare deployment configuration
4. Document usage instructions