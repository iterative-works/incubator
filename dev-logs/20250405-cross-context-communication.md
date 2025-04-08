# Cross-Context Communication Patterns

This document outlines the patterns for communication between bounded contexts in our restructured architecture. These patterns ensure that each bounded context maintains its internal integrity while allowing necessary interactions with other contexts.

## Current Context Map

```
Transaction Management     <----->     YNAB Integration
        ^                                    ^
        |                                    |
        v                                    v
   Fio Bank              Future:        Future:
                        AI Categorization   User Management
```

## Communication Patterns

### 1. Ports and Adapters (Hexagonal Architecture)

Each bounded context should define interfaces (ports) through which it expects to interact with other contexts. The implementation of these interfaces (adapters) can then be provided by the other context or through a translation layer.

#### Example: Transaction Management to YNAB Integration

```scala
// In Transaction Management context (ports)
package works.iterative.incubator.transactions.application.port

trait YnabPort:
  def submitTransaction(transaction: Transaction): ZIO[Any, YnabError, String]
  def getYnabAccounts(): ZIO[Any, YnabError, Seq[YnabAccountRef]]

// In YNAB Integration context (adapters)
package works.iterative.incubator.ynab.infrastructure.adapter

class YnabAdapter(ynabService: YnabService) extends YnabPort:
  override def submitTransaction(transaction: Transaction): ZIO[Any, YnabError, String] =
    // Convert Transaction to YnabTransaction
    // Call ynabService.createTransaction
  
  override def getYnabAccounts(): ZIO[Any, YnabError, Seq[YnabAccountRef]] =
    // Call ynabService.getAccounts and convert to YnabAccountRef
```

### 2. Domain Events

For loosely coupled contexts, domain events provide a way to communicate changes without direct dependencies. Events are emitted by one context and can be consumed by any interested context.

#### Example: Transaction Management emitting events

```scala
// In Transaction Management context
package works.iterative.incubator.transactions.domain.event

case class TransactionCreated(
  transactionId: TransactionId,
  sourceAccountId: SourceAccountId,
  amount: BigDecimal,
  date: LocalDate,
  description: String,
  timestamp: LocalDateTime = LocalDateTime.now()
)

// Event publisher in Application Service
class DefaultTransactionManagerService extends TransactionManagerService:
  override def createTransaction(transaction: Transaction): Task[Transaction] =
    for
      saved <- transactionRepository.save(transaction)
      _ <- eventPublisher.publish(TransactionCreated(
        transactionId = saved.id,
        sourceAccountId = saved.sourceAccountId,
        amount = saved.amount,
        date = saved.date,
        description = saved.description
      ))
    yield saved
```

#### Example: YNAB Integration consuming events

```scala
// In YNAB Integration context
package works.iterative.incubator.ynab.infrastructure.event

class TransactionEventSubscriber(ynabService: YnabService) extends EventSubscriber:
  def subscribe(): Task[Unit] =
    eventBus.subscribe[TransactionCreated] { event =>
      for
        // Get YNAB account ID for the source account
        sourceAccount <- sourceAccountRepository.findById(event.sourceAccountId)
        ynabAccountId <- ZIO.fromOption(sourceAccount.flatMap(_.ynabAccountId))
          .orElseFail(new RuntimeException("Source account not linked to YNAB"))
        
        // Create YNAB transaction
        ynabTransaction = YnabTransaction(
          date = event.date,
          amount = event.amount,
          memo = Some(event.description),
          accountId = ynabAccountId
        )
        
        // Submit to YNAB
        _ <- ynabService.createTransaction(ynabTransaction)
      yield ()
    }
```

### 3. Anti-Corruption Layers (ACL)

When contexts have different models or speak different languages, an anti-corruption layer helps translate between them while maintaining the integrity of each context.

#### Example: Fio Bank to Transaction Management ACL

```scala
// In Fio Bank context
package works.iterative.incubator.fio.infrastructure.acl

class FioTransactionTranslator:
  def toTransaction(fioTransaction: FioTransaction, sourceAccount: SourceAccount): Transaction =
    Transaction(
      id = TransactionId(UUID.randomUUID()),
      sourceAccountId = sourceAccount.id,
      amount = fioTransaction.amount,
      currency = fioTransaction.currency,
      date = fioTransaction.date,
      description = fioTransaction.message.getOrElse("") + " " + fioTransaction.comment.getOrElse(""),
      counterpartyAccount = fioTransaction.counterpartyAccount,
      counterpartyBankCode = fioTransaction.counterpartyBankCode,
      transactionType = mapTransactionType(fioTransaction.type),
      variableSymbol = fioTransaction.variableSymbol,
      specificSymbol = fioTransaction.specificSymbol,
      constantSymbol = fioTransaction.constantSymbol,
      status = TransactionStatus.Imported
    )
  
  private def mapTransactionType(fioType: String): String =
    fioType match
      case "PAYMENT_HOME" => "OUTGOING"
      case "PAYMENT_FOREIGN" => "OUTGOING"
      case "COLLECTION" => "INCOMING"
      case _ => "OTHER"
```

### 4. Shared Kernel

In some cases, contexts may share a small set of models or interfaces. This should be used sparingly to avoid tight coupling.

#### Example: Shared Money Value Object

```scala
// In shared kernel
package works.iterative.incubator.shared.domain

case class Money(amount: BigDecimal, currency: String):
  def plus(other: Money): Either[CurrencyMismatchError, Money] =
    if (currency == other.currency)
      Right(Money(amount + other.amount, currency))
    else
      Left(CurrencyMismatchError(currency, other.currency))
  
  def minus(other: Money): Either[CurrencyMismatchError, Money] =
    if (currency == other.currency)
      Right(Money(amount - other.amount, currency))
    else
      Left(CurrencyMismatchError(currency, other.currency))
```

## Implementation Steps

1. **Identify Context Boundaries**: Clearly define the responsibilities of each bounded context
2. **Define Interfaces**: Each context should expose well-defined interfaces for other contexts to use
3. **Implement Translation Layers**: Create anti-corruption layers where necessary
4. **Set Up Event Infrastructure**: Implement domain event publishing and subscription
5. **Review Dependencies**: Ensure dependencies flow in the right direction (domain → application → infrastructure)
6. **Test Context Interactions**: Verify that contexts can interact as expected

## Context-Specific Recommendations

### Transaction Management → YNAB Integration

- **Pattern**: Ports and Adapters
- **Direction**: Transaction Management depends on YNAB Integration through ports
- **Implementation**:
  1. Define `YnabPort` in Transaction Management context
  2. Implement adapter in YNAB Integration context
  3. Inject adapter into Transaction Management services

### Fio Bank → Transaction Management

- **Pattern**: Anti-Corruption Layer
- **Direction**: Fio Bank translates to Transaction Management concepts
- **Implementation**:
  1. Create translator in Fio Bank context
  2. Use translator to convert Fio concepts to Transaction Management concepts
  3. Pass translated objects to Transaction Management interfaces

### Future: AI Categorization → Transaction Management

- **Pattern**: Domain Events
- **Direction**: Transaction Management emits events, AI Categorization reacts
- **Implementation**:
  1. Define relevant domain events in Transaction Management
  2. Set up event subscription in AI Categorization
  3. Implement reaction logic in AI Categorization

### Future: User Management → All Contexts

- **Pattern**: Shared Kernel
- **Direction**: All contexts use User Management for authentication
- **Implementation**:
  1. Define minimal shared security interfaces
  2. Implement in User Management context
  3. Use through dependency injection in other contexts