# Development Log: Architecture Alignment Plan - April 5, 2026

After reviewing our current implementation against our architecture document and conducting a bounded context analysis, we've identified several areas where we need to strengthen architectural alignment. This document outlines our plan to reorganize the codebase according to Domain-Driven Design principles, with a focus on clearly defined bounded contexts.

## Current Status

Our codebase shows good alignment with key architectural principles:
- Domain-first organization with interfaces defined in core modules
- Clean repository pattern implementation
- Functional MVP approach for web modules
- ZIO effect tracking

However, we've identified these areas for improvement:
- Bounded contexts are not explicitly defined in package structure
- Some context boundaries are unclear or permeable
- Limited use of domain events for cross-context communication
- Inconsistent component classification
- Unclear distinction between domain and application services

## Bounded Context Approach

Based on our analysis (see `ynab-importer/doc/bounded_contexts.md`), we've identified these key bounded contexts:

1. **Transaction Management Context**
2. **YNAB Integration Context**
3. **Fio Bank Integration Context**
4. **AI Categorization Context** (planned)
5. **User Management Context** (planned)

Our restructuring will focus on clearly separating these contexts while maintaining proper integration points between them.

## Action Plan

### 1. Package Restructuring by Bounded Context

Reorganize the codebase to reflect distinct bounded contexts with clear boundaries:

```
works.iterative.incubator/
  ├── transactions/     # Transaction Management Context
  ├── ynab/             # YNAB Integration Context
  ├── fio/              # Fio Bank Context
  ├── categorization/   # Future AI Categorization Context
  └── auth/             # Future User Management Context
```

Within each bounded context, organize by architectural layer:

```
transactions/
  ├── domain/       # Domain model, interfaces, domain services
  ├── application/  # Use case orchestration, application services
  ├── infrastructure/ # Implementation details, repositories
  └── web/          # UI components, views, routes
```

**Tasks:**
- [ ] Create design document for the new package structure
- [ ] Migrate Transaction Management Context entities and services
- [ ] Extract YNAB Integration Context from current implementation
- [ ] Extract Fio Bank Integration Context from adapters
- [ ] Set up skeleton packages for future contexts

### 2. Define Context Boundaries and Interfaces

Establish clear interfaces between bounded contexts to maintain separation:

```scala
// In transactions/domain/service/YnabIntegrationPort.scala
trait YnabIntegrationPort:
  def submitTransaction(transaction: Transaction): IO[YnabIntegrationError, String]
  def getAvailableAccounts(): IO[YnabIntegrationError, Seq[YnabAccountRef]]
  
// In ynab/application/service/TransactionManagementAdapter.scala
class TransactionManagementAdapter(ynabService: YnabService) extends YnabIntegrationPort:
  override def submitTransaction(transaction: Transaction): IO[YnabIntegrationError, String] =
    // Convert transaction to YNAB model and submit
```

**Tasks:**
- [ ] Define interfaces (ports) for each bounded context
- [ ] Implement adapters for cross-context communication
- [ ] Create anti-corruption layers for external systems
- [ ] Document the interfaces and their contracts

### 3. Implement Domain Events for Cross-Context Communication

Create an event-based communication mechanism for asynchronous integration:

```scala
// In transactions/domain/event/TransactionEvents.scala
sealed trait TransactionEvent extends DomainEvent
case class TransactionImported(transaction: Transaction, time: Instant) extends TransactionEvent
case class TransactionReadyForYnab(transactionId: TransactionId, time: Instant) extends TransactionEvent

// In the application layer
def processImportedTransaction(event: TransactionImported): ZIO[EventPublisher & AppEnv, Throwable, Unit] =
  for
    _ <- transactionProcessingService.process(event.transaction)
    _ <- eventPublisher.publish(TransactionReadyForYnab(event.transaction.id, Instant.now()))
  yield ()
```

**Tasks:**
- [ ] Define domain event base traits
- [ ] Create specific events for significant state changes
- [ ] Implement event publishing mechanism
- [ ] Add event handlers in appropriate bounded contexts
- [ ] Set up event-based integration between contexts

### 4. Component Classification and Documentation

Add explicit classification comments to all major components:

```scala
/**
 * Repository for managing source accounts from which transactions are imported.
 *
 * Category: Repository Interface
 * Layer: Domain
 * Bounded Context: Transaction Management
 */
trait SourceAccountRepository
```

**Tasks:**
- [ ] Create documentation template for component classification
- [ ] Add classification to domain entities and value objects
- [ ] Classify services by type (domain service, application service)
- [ ] Document bounded context ownership for each component
- [ ] Add bounded context identifiers to package-info files

### 5. Clarify Application vs. Domain Services

Separate domain logic from orchestration and use case implementation:

```scala
// Domain service - pure business logic
trait TransactionMatcher:  // In transactions/domain/service
  def findDuplicates(transaction: Transaction, candidates: Seq[Transaction]): Seq[Transaction]
  
// Application service - orchestration
trait TransactionImportService:  // In transactions/application/service
  def importTransactions(command: ImportTransactionsCommand): RIO[AppEnv, ImportTransactionsResult]
```

**Tasks:**
- [ ] Audit existing services and classify as domain or application
- [ ] Move domain services to domain layer
- [ ] Create application services for orchestration
- [ ] Ensure domain services remain pure with no infrastructure dependencies
- [ ] Update web modules to use application services

### 6. Strengthen Effect Abstractions by Context

Define clear effect abstractions for each bounded context:

```scala
// In transactions/domain/effect
trait TransactionImportEffect:
  def importFromBank(accountId: String, from: LocalDate, to: LocalDate): IO[ImportError, Seq[Transaction]]

// In fio/application
class FioImportEffectProvider(fioClient: FioClient) extends TransactionImportEffect:
  override def importFromBank(accountId: String, from: LocalDate, to: LocalDate): IO[ImportError, Seq[Transaction]] =
    // Implementation using FioClient
```

**Tasks:**
- [ ] Identify required effect abstractions for each bounded context
- [ ] Create effect traits in appropriate domain layers
- [ ] Implement effect providers in infrastructure layers
- [ ] Update services to use effect abstractions

## Implementation Strategy

### Phase 1: Planning and Documentation (April 5-7)
- Complete bounded context analysis (DONE)
- Design package structure that reflects bounded contexts
- Document interfaces between contexts
- Define domain events for cross-context communication

### Phase 2: Core Restructuring (April 8-12)
- Create new package structure
- Move entities to appropriate bounded contexts
- Implement ports and adapters between contexts
- Establish base domain event system

### Phase 3: Service Refactoring (April 13-15)
- Separate domain and application services
- Implement effect abstractions
- Create event handlers
- Connect contexts via defined interfaces

### Phase 4: UI Adaptation (April 16-18)
- Update web modules to align with new structure
- Connect UI to application services
- Test end-to-end workflows

### Phase 5: Testing and Verification (April 19-20)
- Expand test coverage for all contexts
- Verify cross-context communication
- Ensure all bounded contexts operate correctly

## Prioritized TODOs

1. [ ] Finalize bounded context definitions and boundaries
2. [ ] Create package structure design document
3. [ ] Define interfaces between Transaction Management and YNAB contexts
4. [ ] Extract Fio integration into separate bounded context
5. [ ] Implement domain events for cross-context communication
6. [ ] Update web modules to use new structure
7. [ ] Add comprehensive test coverage for new architecture

## Implementation Approach

We'll use an incremental approach that maintains a working system at all times:

1. Create new package structure alongside existing code
2. Move code to new locations class by class
3. Update references incrementally
4. Verify functionality after each significant change
5. Remove old code once new structure is fully operational

This approach minimizes risk and allows us to continue development on the YNAB integration while restructuring the architecture.

## Conclusion

This plan provides a roadmap for better aligning our implementation with our architecture document, focusing on bounded context separation and domain-driven design principles. The changes will create a more maintainable, testable, and understandable codebase that fully embodies our architectural vision.