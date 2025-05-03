# Task: Implement TransactionImportModule Web Module with Tapir

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Tapir's TapirEndpointModule framework, Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: 
  - View models already defined in `works.iterative.incubator.budget.ui.transaction_import.models`
  - UI components already created including `DateRangeSelector`, `ImportButton`, `StatusIndicator`, and `ResultsPanel`
- **Requirements**: Create a Tapir web module following our Functional MVP pattern that allows users to import transactions by selecting a date range and initiating an import, with appropriate status feedback and results display

## Reference Information
- [Web Module Implementation Guide](/ai-context/architecture/guides/web_module_implementation_guide.md)
- [Implementation Plan](/features/BUDGET-001/VS001/implementation_plan.md)
- [Transaction Import Feature](/features/BUDGET-001/VS001/scenarios/transaction_import.feature)

## Gherkin Scenarios
```gherkin
Feature: Transaction Import from Fio Bank
  As a finance team member
  I want to automatically import transactions from Fio Bank
  So that I don't have to manually export and import data

  Scenario: Successfully import transactions for a date range
    Given I am on the transaction import page
    When I select "2025-04-01" as the start date
    And I select "2025-04-15" as the end date
    And I click the "Import Transactions" button
    Then I should see a progress indicator with status "Connecting to Fio Bank"
    And then the status should change to "Retrieving transactions"
    And then the status should change to "Storing transactions"
    And finally I should see a summary showing "15 transactions successfully imported"
```

## Specific Request
1. Implement the `TransactionImportModule` extending `TapirEndpointModule` that:
   - Defines endpoints for the transaction import page
   - Handles form submission for date range selection
   - Processes the import request via HTMX
   - Updates status during the import process
   - Renders results after import completion

2. Create the `TransactionImportService` interface:
   - Define methods for handling the import process
   - Follow the ZIO effect pattern for error handling
   - Map domain objects to view models

3. Implement a `MockTransactionImportService` for UI development:
   - Simulates successful imports with random transaction counts
   - Simulates occasional errors for testing
   - Includes artificial delays to demonstrate loading states

4. Create a `TransactionImportView` class:
   - Render the import page using Scalatags
   - Use HTMX for interactive elements
   - Compose the UI from the existing components

5. Implement the necessary wiring to register the module in `ModuleRegistry`

## Output Format
1. Module implementation:
   ```scala
   // TransactionImportModule.scala
   class TransactionImportModule(
     transactionImportView: TransactionImportView
   ) extends TapirEndpointModule[TransactionImportService]:
     import CustomTapir.*
     
     // Base endpoint definition
     private val baseEndpoint = endpoint
       .in("transactions" / "import")
       .errorOut(stringBody.mapTo[String])
       .out(stringBody.mapTo[String])
       
     // Endpoint definitions
     val importPageEndpoint = baseEndpoint
       .name("Transaction Import Page")
       .get
       
     val importTransactionsEndpoint = baseEndpoint
       .name("Import Transactions")
       .post
       .in(formBody[ImportFormInput])
       
     // Implementation methods and server endpoints
   ```

2. Service interface:
   ```scala
   // TransactionImportService.scala
   trait TransactionImportService:
     def getImportViewModel(): ZIO[Any, String, ImportPageViewModel]
     def validateDateRange(startDate: LocalDate, endDate: LocalDate): ZIO[Any, String, Either[String, Unit]]
     def importTransactions(startDate: LocalDate, endDate: LocalDate): ZIO[Any, String, ImportResults]
     def getImportStatus(): ZIO[Any, String, ImportStatus]
   ```

3. Mock service implementation:
   ```scala
   // MockTransactionImportService.scala
   class MockTransactionImportService extends TransactionImportService:
     private val random = new scala.util.Random()
     private var currentStatus: ImportStatus = ImportStatus.NotStarted
     private var lastImportResults: Option[ImportResults] = None
     
     // Implementation methods
   ```

4. View implementation:
   ```scala
   // TransactionImportView.scala
   class TransactionImportView:
     def renderImportPage(viewModel: ImportPageViewModel): String =
       html(
         // HTML rendering with ScalaTags
       ).render
       
     def renderImportStatus(status: ImportStatus): String =
       // HTML for status updates
       
     def renderImportResults(results: ImportResults): String =
       // HTML for import results
   ```

5. Form input class:
   ```scala
   // ImportFormInput.scala
   case class ImportFormInput(
     startDate: String,
     endDate: String
   )
   ```

## Constraints
- Follow the TapirEndpointModule pattern from the Web Module Implementation Guide
- Implement the Functional MVP pattern with clear separation of View, ViewModel, and Service
- Use the existing view models and ScalaTags UI components
- Make all endpoints HTMX-compatible for dynamic updates
- Use ZIO effects for all service methods
- Include proper error handling with user-friendly messages
- Create a mock implementation for the service to enable parallel UI development

## Service Functionality
The TransactionImportService should provide:
1. A method to get the initial import page view model
2. A method to validate date ranges with business rules
3. A method to initiate and process the import
4. A method to check current import status

## Mock Implementation Behaviors
The mock implementation should:
1. Validate dates according to business rules (within 90 days, not in future, etc.)
2. Simulate multi-step import process with status changes
3. Randomly generate successful or error results
4. Include realistic delays to demonstrate loading states