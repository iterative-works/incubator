# Task 11: Create Working Prototype for Transaction Import

## Overview

This document outlines the implementation steps required to create a working prototype of the TransactionImportModule that demonstrates all scenarios from the Gherkin feature file. The prototype will leverage the existing UI components, view models, and service interfaces that have already been developed.

## Requirements

The working prototype should demonstrate the following scenarios:

```gherkin
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

## Current Implementation Status

The following components have already been implemented:

- View Models:
  - `ImportPageViewModel`, `DateRangeSelectorViewModel`, `ImportButtonViewModel`, `StatusIndicatorViewModel`, `ResultsPanelViewModel`, etc.
  - `ImportStatus` enum and `ImportResults` class

- UI Components:
  - `DateRangeSelector`, `ImportButton`, `StatusIndicator`, `ResultsPanel`
  - `TailwindStyles` utility

- Service Interfaces:
  - `TransactionImportService` interface
  - `MockTransactionImportService` implementation

- Module Framework:
  - `TransactionImportModule` with endpoint definitions
  - `TransactionImportView` for rendering the UI

What's missing is the proper wiring and configuration to make these components work together as a functional prototype.

## Implementation Tasks

### 1. Update AppEnv Configuration

The `AppEnv` needs to include the `TransactionImportService` to make it available throughout the application.

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/src/main/scala/works/iterative/incubator/server/AppEnv.scala`

```scala
package works.iterative.incubator.server

import works.iterative.incubator.budget.ui.transaction_import.TransactionImportService
import works.iterative.incubator.budget.ui.transaction_import.MockTransactionImportService
import works.iterative.core.config.Config
import works.iterative.incubator.components.ScalatagsViteSupport
import zio.*

// Define the application environment
type AppEnv = TransactionImportService

// Companion object with layer definitions
object AppEnv:
  // Live layer that provides all required services
  val live: ZLayer[ScalatagsViteSupport, Config.Error, AppEnv] =
    ZLayer.make[AppEnv](
      // Use MockTransactionImportService for the prototype
      MockTransactionImportService.layer
    )
```

### 2. Update ModuleRegistry to Include TransactionImportModule

Register the TransactionImportModule in the application's ModuleRegistry to make it available for HTTP requests.

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/src/main/scala/works/iterative/incubator/server/view/modules/ModuleRegistry.scala`

```scala
package works.iterative.incubator.server.view.modules

import works.iterative.incubator.budget.ui.transaction_import.TransactionImportModule
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportService
import works.iterative.incubator.components.ScalatagsAppShell
import works.iterative.incubator.components.ScalatagsViteSupport
import works.iterative.incubator.server.AppEnv
import works.iterative.server.http.WebFeatureModule
import works.iterative.server.http.tapir.TapirWebModuleAdapter
import works.iterative.tapir.BaseUri
import works.iterative.core.config.Config
import sttp.tapir.server.http4s.Http4sServerOptions
import zio.*

class ModuleRegistry(
    baseUri: BaseUri,
    viteConfig: AssetsModule.ViteConfig,
    viteSupport: ScalatagsViteSupport
):
    // Create AppShell for consistent UI wrapping
    private val appShell = ScalatagsAppShell(viteSupport)
    
    // Existing modules
    private val helloWorldModule = HelloWorldModule
    private val assetsModule = AssetsModule(viteConfig)
    
    // Transaction Import Module
    private val transactionImportModule: TransactionImportModule =
        TransactionImportModule(appShell, baseUri)
    
    // Adapt to WebFeatureModule
    private val transactionImportWebModule =
        TapirWebModuleAdapter.adapt[TransactionImportService](
            options = Http4sServerOptions.default,
            module = transactionImportModule
        )
    
    // List of all modules to be registered with the HTTP server
    def modules: List[WebFeatureModule[RIO[AppEnv, *]]] = List(
        helloWorldModule.widen,
        assetsModule.widen,
        transactionImportWebModule
    )
end ModuleRegistry

object ModuleRegistry:
    val layer: ZLayer[ScalatagsViteSupport, Config.Error, ModuleRegistry] =
        ZLayer {
            for
                baseUri <- ZIO.config[BaseUri](BaseUri.config)
                viteConfig <- ZIO.config(AssetsModule.ViteConfig.config)
                viteSupport <- ZIO.service[ScalatagsViteSupport]
            yield ModuleRegistry(baseUri, viteConfig, viteSupport)
        }
end ModuleRegistry
```

### 3. Fix HTMX URL Paths in UI Components

The HTMX attributes in UI components must match the endpoint paths defined in the TransactionImportModule.

#### 3.1 Update DateRangeSelector Component

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/DateRangeSelector.scala`

```scala
// In the render method, update the hx-post attribute
attr("hx-post") := "/transactions/import/validate-dates"
```

#### 3.2 Update ImportButton Component

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ImportButton.scala`

```scala
// In the render method, update the hx-post attribute
attr("hx-post") := s"/transactions/import?startDate=$startDateParam&endDate=$endDateParam"
```

#### 3.3 Update ResultsPanel Component

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ResultsPanel.scala`

```scala
// In the retry button section, update the hx-post attribute
attr("hx-post") := s"/transactions/import?startDate=${viewModel.startDate}&endDate=${viewModel.endDate}"
```

### 4. Configure HTMX Target IDs for Consistency

Ensure that the HTMX target IDs match between components for proper updates.

#### 4.1 Update ImportButton Component

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ImportButton.scala`

```scala
// Update the hx-target attribute to target the results panel container
attr("hx-target") := "#results-panel-container"
```

#### 4.2 Update ResultsPanel Component

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ResultsPanel.scala`

```scala
// Update the ID of the main div to match the target
div(
    id := "results-panel-container",
    cls := "rounded-lg shadow-md mt-6",
    // ...
)
```

### 5. Enhance MockTransactionImportService for Scenario Demonstration

Enhance the `MockTransactionImportService` to support all three scenarios from the Gherkin feature file.

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/MockTransactionImportService.scala`

```scala
package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.{Instant, LocalDate}
import zio.*
import scala.util.Random

/**
 * Enum representing different import scenarios for demonstration
 */
enum ImportScenario:
    case SuccessfulImport, NoTransactions, ErrorDuringImport

/**
 * Mock implementation of TransactionImportService for UI development.
 * Simulates the import process with configurable scenarios for demonstration.
 */
class MockTransactionImportService extends TransactionImportService:
    private val random = new Random()
    private var currentStatus: ImportStatus = ImportStatus.NotStarted
    private var lastImportResults: Option[ImportResults] = None
    private var importStartTime: Option[Instant] = None
    
    // Default to successful import scenario
    private var activeScenario: ImportScenario = ImportScenario.SuccessfulImport
    
    /**
     * Set the active scenario for demonstration purposes
     */
    def setScenario(scenario: ImportScenario): Unit =
        activeScenario = scenario
        // Reset status for a clean demonstration
        currentStatus = ImportStatus.NotStarted
        lastImportResults = None
        importStartTime = None
    
    /**
     * Get the initial view model for the import page.
     */
    override def getImportViewModel(): ZIO[Any, String, ImportPageViewModel] =
        ZIO.succeed(
            ImportPageViewModel(
                startDate = LocalDate.now().withDayOfMonth(1),
                endDate = LocalDate.now(),
                importStatus = currentStatus,
                importResults = lastImportResults,
                validationError = None
            )
        )
    
    /**
     * Validate a date range based on business rules.
     */
    override def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, Either[String, Unit]] =
        ZIO.succeed {
            if startDate == null || endDate == null then
                Left("Both start and end dates are required")
            else if startDate.isAfter(endDate) then
                Left("Start date cannot be after end date")
            else if startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now()) then
                Left("Dates cannot be in the future")
            else if startDate.plusDays(90).isBefore(endDate) then
                Left("Date range cannot exceed 90 days (Fio Bank API limitation)")
            else
                Right(())
        }
    
    /**
     * Import transactions for the specified date range based on the active scenario.
     */
    override def importTransactions(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults] =
        for
            // Validate date range first
            _ <- validateDateRange(startDate, endDate).flatMap {
                case Left(error) => ZIO.fail(error)
                case Right(_)    => ZIO.unit
            }
            
            // Start the import process
            _ <- ZIO.succeed {
                currentStatus = ImportStatus.InProgress
                importStartTime = Some(Instant.now())
            }
            
            // Initial connecting status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(800))
            
            // Process according to the active scenario
            results <- activeScenario match
                case ImportScenario.SuccessfulImport => 
                    handleSuccessfulImport(startDate, endDate)
                
                case ImportScenario.NoTransactions => 
                    handleNoTransactionsScenario
                
                case ImportScenario.ErrorDuringImport => 
                    handleErrorScenario
        yield results
    
    /**
     * Handle the successful import scenario with random transaction count
     */
    private def handleSuccessfulImport(
        startDate: LocalDate, 
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults] =
        for
            // Retrieving transactions status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(1200))
            
            // Random transaction count based on date range (1 to days between dates)
            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt + 1
            transactionCount = if daysBetween <= 0 then 1 else (random.nextInt(daysBetween) + 1)
            
            // Storing transactions status - simulate delay based on count
            _ <- ZIO.sleep(Duration.fromMillis(500 + (transactionCount * 50).min(2000)))
            
            // Complete the import
            results <- ZIO.succeed {
                val now = Instant.now()
                val importResults = ImportResults(
                    transactionCount = transactionCount,
                    errorMessage = None,
                    startTime = importStartTime.getOrElse(now.minusSeconds(5)),
                    endTime = Some(now)
                )
                lastImportResults = Some(importResults)
                currentStatus = ImportStatus.Completed
                importResults
            }
        yield results
    
    /**
     * Handle the scenario where no transactions are found
     */
    private def handleNoTransactionsScenario: ZIO[Any, String, ImportResults] =
        for
            // Retrieving transactions status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(1000))
            
            // Complete the import with zero transactions
            results <- ZIO.succeed {
                val now = Instant.now()
                val importResults = ImportResults(
                    transactionCount = 0,
                    errorMessage = None,
                    startTime = importStartTime.getOrElse(now.minusSeconds(3)),
                    endTime = Some(now)
                )
                lastImportResults = Some(importResults)
                currentStatus = ImportStatus.Completed
                importResults
            }
        yield results
    
    /**
     * Handle the error scenario where the API is unavailable
     */
    private def handleErrorScenario: ZIO[Any, Nothing, ImportResults] =
        for
            // Short delay to simulate connection attempt
            _ <- ZIO.sleep(Duration.fromMillis(1500))
            
            // Set error status
            results <- ZIO.succeed {
                val now = Instant.now()
                val importResults = ImportResults(
                    transactionCount = 0,
                    errorMessage = Some("Connection to Fio Bank failed: Network timeout"),
                    startTime = importStartTime.getOrElse(now.minusSeconds(2)),
                    endTime = Some(now)
                )
                lastImportResults = Some(importResults)
                currentStatus = ImportStatus.Error
                importResults
            }
        yield results
    
    /**
     * Get the current status of the import operation.
     */
    override def getImportStatus(): ZIO[Any, String, ImportStatus] =
        ZIO.succeed(currentStatus)
end MockTransactionImportService

object MockTransactionImportService:
    /**
     * Create a new instance of MockTransactionImportService.
     */
    val layer: ULayer[TransactionImportService] =
        ZLayer.succeed(new MockTransactionImportService())
end MockTransactionImportService
```

### 6. Add Main.scala Update (If Needed)

If any changes are needed to the main server initialization:

**File:** `/Users/mph/Devel/commercial/iw/iw-incubator/src/main/scala/works/iterative/incubator/server/Main.scala`

- Ensure that `ModuleRegistry` is properly integrated
- Verify that the HTTP server configuration includes the appropriate routes

## Verification Tasks

### 1. Compile the Project

```bash
sbtn compile
```

Verify that there are no compilation errors.

### 2. Run Tests

```bash
sbtn test
```

Ensure all existing tests pass without errors.

### 3. Start the Server

```bash
sbtn reStart
```

Verify that the server starts correctly without errors.

### 4. Manual Testing Plan

Navigate to `http://localhost:8080/transactions/import` and verify:

#### Scenario 1: Successful Import
1. Select a valid date range
2. Click "Import Transactions"
3. Verify the status indicator shows "In Progress" during import
4. Verify the results panel shows a success message with transaction count
5. Verify the "View Transactions" button is displayed

#### Scenario 2: No Transactions
1. Update the `MockTransactionImportService` to use the `NoTransactions` scenario
2. Select a valid date range
3. Click "Import Transactions"
4. Verify the results panel shows a message indicating no transactions were found

#### Scenario 3: Error During Import
1. Update the `MockTransactionImportService` to use the `ErrorDuringImport` scenario
2. Select a valid date range
3. Click "Import Transactions"
4. Verify the status indicator shows "Error"
5. Verify the error message is displayed
6. Verify the retry button is displayed and functional

## References

### Codebase Reference Files

- **View Models**:
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/ImportPageViewModel.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/DateRangeSelectorViewModel.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/ImportButtonViewModel.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/ImportStatus.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/ImportResults.scala`

- **Components**:
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/DateRangeSelector.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ImportButton.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/StatusIndicator.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ResultsPanel.scala`

- **Services**:
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/TransactionImportService.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/MockTransactionImportService.scala`

- **Module**:
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/TransactionImportModule.scala`
  - `/Users/mph/Devel/commercial/iw/iw-incubator/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/TransactionImportView.scala`

### Documentation References

- Vertical Slice Implementation Plan: `/Users/mph/Devel/commercial/iw/iw-incubator/features/BUDGET-001/VS001/implementation_plan.md`
- Gherkin Scenarios: `/Users/mph/Devel/commercial/iw/iw-incubator/features/BUDGET-001/VS001/scenarios/transaction_import.feature`
- Web Module Implementation Guide: `/Users/mph/Devel/commercial/iw/iw-incubator/ai-context/architecture/guides/web_module_implementation_guide.md`
- Project Development Workflow: `/Users/mph/Devel/commercial/iw/iw-incubator/ai-context/workflows/development_workflow.md`

## Timeline and Effort Estimate

- Environment Setup & Configuration: 2 hours
- UI Component Updates: 1 hour
- Service Implementation Enhancements: 2 hours
- Testing & Debugging: 3 hours

Total Estimated Effort: 8 hours

## Next Steps After Prototype

1. Iterate on the UI based on stakeholder feedback
2. Begin implementation of real infrastructure components:
   - FioBankApiClient for real API integration
   - PostgreSQLTransactionRepository for database persistence
3. Implement real authentication and authorization
4. Create automated end-to-end tests for all scenarios