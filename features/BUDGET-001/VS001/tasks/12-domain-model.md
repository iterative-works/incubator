# Task: Implement Transaction Import Domain Entities and Value Objects

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities
- **Technical context**: Following Functional Core architecture with ZIO for effects
- **Current implementation**: UI components, view models, and mock services are already implemented in the `bounded-contexts/budget` module
- **Requirements**: Implement the core domain model for transaction import functionality to serve the existing view model

## Existing Implementation Context
The UI layer is already implemented with the following components:

1. **View Models**: All view models for transaction import are defined in `bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/`:
   - `ImportStatus.scala`: Enum with values `NotStarted`, `InProgress`, `Completed`, `Error`
   - `ImportResults.scala`: Case class containing `transactionCount`, `errorMessage`, `startTime`, `endTime`
   - `ImportPageViewModel.scala`: Main view model for the import page
   - Other supporting view models for UI components

2. **Service Interface**: 
   - `TransactionImportService.scala`: Defines the interface for handling transaction imports with methods:
     - `getImportViewModel()`: Get the initial view model
     - `validateDateRange(startDate, endDate)`: Validate date range based on business rules
     - `importTransactions(startDate, endDate)`: Import transactions for specified date range
     - `getImportStatus()`: Get current import status

3. **Mock Implementation**:
   - `MockTransactionImportService.scala`: Mock implementation for UI development that simulates different scenarios

Now we need to implement the domain model layer that will power the real implementation of these services.

## Reference Information
- [Entity Implementation Guide](ai-context/principles/guides/entity_implementation_guide.md)
- [Value Object Implementation Guide](ai-context/principles/guides/value_object_implementation_guide.md)
- [Functional Core Architecture](ai-context/principles/principles.md)
- [Architecture Component Classification](ai-context/architecture/architecture.md)

## Specific Request
1. Implement the Transaction domain entity:
   - Define TransactionId value object
   - Create Transaction entity with required attributes
   - Implement validation and business rules
   - Add factory methods in companion object

2. Implement the ImportBatch domain entity:
   - Define ImportBatchId value object
   - Create ImportBatch entity with required attributes
   - Implement validation and business rules
   - Add factory methods in companion object

3. Implement necessary value objects:
   - Money value object for transaction amounts
   - TransactionStatus enum
   - ImportStatus enum

4. Write comprehensive tests covering:
   - Entity creation
   - Validation rules
   - Business operations
   - Factory methods

## Implementation Structure

Your implementation should follow the project's package structure:

```
bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/domain/
  ├── model/                # Domain entities and value objects
  │   ├── Transaction.scala
  │   ├── ImportBatch.scala
  │   ├── Money.scala
  │   └── ...
  ├── service/              # Domain services
  │   └── ...
  └── repository/           # Repository interfaces
      ├── TransactionRepository.scala
      └── ImportBatchRepository.scala
```

## Output Format
1. Entity implementation:
   ```scala
   // Transaction.scala in domain/model package
   package works.iterative.incubator.budget.domain.model

   import java.time.LocalDate
   import java.util.UUID

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
   ):
     // Business methods
   
   object Transaction:
     // Factory methods
     def create(
       accountId: AccountId,
       date: LocalDate,
       amount: Money,
       description: String,
       counterparty: Option[String] = None,
       reference: Option[String] = None,
       importBatchId: ImportBatchId
     ): Transaction = {
       // Implementation with validation
     }
   ```

2. Value object implementation:
   ```scala
   // TransactionId.scala in domain/model package
   package works.iterative.incubator.budget.domain.model

   import java.util.UUID

   case class TransactionId(value: UUID)

   object TransactionId:
     def generate(): TransactionId = TransactionId(UUID.randomUUID())
   ```

3. Test implementation:
   ```scala
   // TransactionSpec.scala in test package
   package works.iterative.incubator.budget.domain.model

   import zio.test.*
   import java.time.LocalDate
   import java.util.UUID

   object TransactionSpec extends ZIOSpecDefault:
     def spec = suite("Transaction")(
       // Test cases
     )
   ```

## Integration with UI Layer

Your domain model should be designed to integrate with the existing UI layer:

1. The domain model `ImportStatus` should be mappable to the UI model `ImportStatus`
2. The domain service for transaction import should provide all data needed for the `TransactionImportService` interface
3. Be careful to maintain a clean separation between domain and UI concerns

## Constraints
- Follow the Functional Core approach with pure domain model
- Implement entities as immutable case classes
- Use typed IDs rather than primitive types
- Ensure all business rules are enforced in the model
- Include factory methods in companion objects
- Add comprehensive test coverage
- Return Either for operations that can fail
