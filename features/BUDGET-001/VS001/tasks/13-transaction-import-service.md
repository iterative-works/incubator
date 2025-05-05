# Task: Implement Transaction Import Service

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities
- **Technical context**: Following Functional Core architecture with ZIO for effects
- **Current implementation**: UI service interface, mock implementation, and view models are already implemented in the `bounded-contexts/budget` module
- **Requirements**: Implement the domain service layer that will power the real implementation of the UI service

## Existing Implementation Context
The UI layer is already implemented with the following components:

1. **Service Interface**:
   - `TransactionImportService.scala`: Defines the interface for handling transaction imports with methods:
     - `getImportViewModel()`: Get the initial view model
     - `validateDateRange(startDate, endDate)`: Validate date range based on business rules
     - `importTransactions(startDate, endDate)`: Import transactions for specified date range
     - `getImportStatus()`: Get current import status

2. **View Models**:
   - `ImportStatus.scala`: Enum with values `NotStarted`, `InProgress`, `Completed`, `Error`
   - `ImportResults.scala`: Case class containing `transactionCount`, `errorMessage`, `startTime`, `endTime`
   - `ImportPageViewModel.scala`: Main view model for the import page

3. **Mock Implementation**:
   - `MockTransactionImportService.scala`: Mock implementation for UI development that simulates different scenarios

Now we need to implement the domain service layer that will handle the actual transaction import logic, using the domain entities that are being developed in parallel.

## Reference Information
- [Application Service Implementation Guide](ai-context/principles/guides/application_service_implementation_guide.md)
- [Domain Service Implementation Guide](ai-context/principles/guides/domain_service_implementation_guide.md)
- [ZIO Service Pattern Guide](ai-context/architecture/guides/zio_service_pattern_guide.md)
- [Functional Core Architecture](ai-context/principles/principles.md)

## Specific Request
1. Implement the domain service for transaction import:
   - Create `TransactionImportDomainService` that handles the core transaction import logic
   - Define domain-specific error types for import operations
   - Implement methods for validating date ranges and importing transactions
   - Add function to map domain entities to view models

2. Implement application service layer:
   - Create `TransactionImportServiceLive` that implements the UI interface and bridges to domain service
   - Inject necessary dependencies (repositories, domain services, etc.)
   - Implement proper error handling and mapping between domain and UI

3. Define necessary dependencies:
   - Add repository dependencies for saving and retrieving transactions
   - Add dependency on `FioBankService` which will be implemented separately
   - Create ZIO layer for service composition

4. Setup tests:
   - Add unit tests for the domain service
   - Add tests for the application service layer
   - Include tests for error cases

## Implementation Structure

Your implementation should follow the project's package structure:

```
bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/
  ├── domain/
  │   ├── model/
  │   │   └── Transaction.scala, ImportBatch.scala, etc.
  │   ├── service/
  │   │   └── TransactionImportDomainService.scala
  │   └── repository/
  │       └── TransactionRepository.scala, ImportBatchRepository.scala
  ├── application/
  │   └── TransactionImportServiceLive.scala
  └── ui/
      └── transaction_import/
          └── TransactionImportService.scala (existing)
```

## Output Format
1. Domain service implementation:
   ```scala
   // TransactionImportDomainService.scala in domain/service package
   package works.iterative.incubator.budget.domain.service

   import works.iterative.incubator.budget.domain.model._
   import java.time.LocalDate
   import zio._

   trait TransactionImportDomainService:
     def validateDateRange(startDate: LocalDate, endDate: LocalDate): ZIO[Any, ImportError, Unit]
     def importTransactions(startDate: LocalDate, endDate: LocalDate): ZIO[Any, ImportError, ImportBatch]

   case class TransactionImportDomainServiceLive(
     fioBankService: FioBankService,
     transactionRepository: TransactionRepository,
     importBatchRepository: ImportBatchRepository,
     idGenerator: IdGenerator,
     clock: Clock
   ) extends TransactionImportDomainService:
     // Implementation
   ```

2. Application service implementation:
   ```scala
   // TransactionImportServiceLive.scala in application package
   package works.iterative.incubator.budget.application

   import works.iterative.incubator.budget.domain.service.TransactionImportDomainService
   import works.iterative.incubator.budget.ui.transaction_import.TransactionImportService
   import works.iterative.incubator.budget.ui.transaction_import.models._
   import java.time.LocalDate
   import zio._

   class TransactionImportServiceLive(
     domainService: TransactionImportDomainService
   ) extends TransactionImportService:
     // Implementation that bridges UI and domain
   ```

3. ZLayer definition:
   ```scala
   // In the TransactionImportServiceLive companion object
   val layer: ZLayer[TransactionImportDomainService, Nothing, TransactionImportService] =
     ZLayer.fromFunction { domainService =>
       new TransactionImportServiceLive(domainService)
     }
   ```

## Domain-UI Integration

Your implementation must bridge between the domain and UI layers:

1. **Domain-First Approach**: Implement pure domain logic first, then connect to UI
2. **Error Mapping**: Map domain errors to UI-friendly messages
3. **View Model Transformation**: Create functions to transform domain entities to view models
4. **Separation of Concerns**: Keep domain logic pure and separate from UI concerns

## Constraints
- Follow the Functional Core approach with pure domain model
- Implement services following ZIO service pattern
- Use ZIO effects for all operations with appropriate error types
- Inject dependencies via constructor
- Add comprehensive test coverage
- Follow the layered architecture: domain → application → UI
