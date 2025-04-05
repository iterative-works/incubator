# Component Classification Examples

This document provides examples of classification comments for different types of components in our restructured architecture. These examples should be used as templates when adding classification comments to existing files during the migration process.

## Domain Model

```scala
/**
 * DOMAIN MODEL: Core entity representing a bank account from which transactions are imported.
 * 
 * This entity belongs to the Transaction Management bounded context and contains essential
 * attributes of a source account without any implementation details.
 */
case class SourceAccount(
    id: SourceAccountId,
    name: String,
    accountNumber: String,
    bankCode: String,
    currency: String,
    active: Boolean = true,
    lastImportDate: Option[LocalDate] = None,
    ynabAccountId: Option[String] = None
)
```

## Domain Repository Interface

```scala
/**
 * DOMAIN REPOSITORY: Interface for accessing and persisting SourceAccount entities.
 * 
 * This repository interface belongs to the Transaction Management bounded context and
 * defines the contract for storing and retrieving source accounts without specifying
 * implementation details.
 */
trait SourceAccountRepository:
    def findById(id: SourceAccountId): Task[Option[SourceAccount]]
    def findByQuery(query: SourceAccountQuery): Task[Seq[SourceAccount]]
    def save(account: SourceAccount): Task[SourceAccount]
    def update(account: SourceAccount): Task[SourceAccount]
    def delete(id: SourceAccountId): Task[Boolean]
```

## Domain Service

```scala
/**
 * DOMAIN SERVICE: Service for processing transactions through various business rules.
 * 
 * This domain service belongs to the Transaction Management bounded context and contains
 * pure business logic for transaction processing independent of infrastructure concerns.
 */
trait TransactionProcessor:
    def process(transaction: Transaction): ZIO[Any, TransactionProcessingError, TransactionProcessingState]
    def updateProcessingState(transaction: Transaction, state: TransactionProcessingState): ZIO[Any, TransactionProcessingError, TransactionProcessingState]
```

## Application Service

```scala
/**
 * APPLICATION SERVICE: Orchestrates use cases for importing transactions from source accounts.
 * 
 * This application service belongs to the Transaction Management bounded context and coordinates 
 * interactions between domain entities, repositories, and external services for the transaction 
 * import process.
 */
trait TransactionImportService:
    def importTransactions(sourceAccountId: SourceAccountId): Task[ImportResult]
    def scheduleImport(sourceAccountId: SourceAccountId, schedule: ImportSchedule): Task[Boolean]
    def getImportHistory(sourceAccountId: SourceAccountId): Task[Seq[ImportRecord]]
```

## Infrastructure Adapter

```scala
/**
 * INFRASTRUCTURE ADAPTER: Client for communicating with the Fio Bank API.
 * 
 * This adapter belongs to the Fio Bank bounded context and handles the technical details
 * of HTTP communication with the bank's API, including authentication, request formatting,
 * and response parsing.
 */
class FioClient(config: FioConfig, httpClient: SttpBackend[Task, Any]):
    def getTransactions(token: String, from: LocalDate, to: LocalDate): Task[Seq[FioTransaction]] = ???
    def getAccountInfo(token: String): Task[FioAccountInfo] = ???
```

## Infrastructure Repository

```scala
/**
 * REPOSITORY IMPLEMENTATION: PostgreSQL implementation of the SourceAccountRepository.
 * 
 * This implementation belongs to the Transaction Management bounded context infrastructure layer
 * and provides concrete data access logic for SourceAccount entities using PostgreSQL.
 */
class PostgreSQLSourceAccountRepository(transactor: Transactor[Task]) extends SourceAccountRepository:
    override def findById(id: SourceAccountId): Task[Option[SourceAccount]] = ???
    override def findByQuery(query: SourceAccountQuery): Task[Seq[SourceAccount]] = ???
    override def save(account: SourceAccount): Task[SourceAccount] = ???
    override def update(account: SourceAccount): Task[SourceAccount] = ???
    override def delete(id: SourceAccountId): Task[Boolean] = ???
```

## View

```scala
/**
 * VIEW: Presentation logic for rendering SourceAccount entities in the UI.
 * 
 * This view component belongs to the Transaction Management bounded context web layer
 * and provides HTML rendering logic for source accounts using ScalaTags.
 */
trait SourceAccountViews:
    def listView(accounts: Seq[SourceAccount]): Tag
    def detailView(account: SourceAccount): Tag
    def editView(account: Option[SourceAccount] = None, errors: Map[String, String] = Map.empty): Tag
```

## Module

```scala
/**
 * MODULE: Web module for source account management features.
 * 
 * This module belongs to the Transaction Management bounded context web layer
 * and provides HTTP routes, services, and views for the source account management feature.
 */
class SourceAccountModule extends ZIOWebModule:
    val service: ZIO[AppEnv & MkHttpApp.Env, Throwable, Unit] = ???
    val view: ZIO[AppEnv & MkHttpApp.Env, Throwable, SourceAccountViews] = ???
    val routes: ZIO[AppEnv & MkHttpApp.Env, Throwable, Routes[Task, Nothing]] = ???
```

## Port

```scala
/**
 * PORT: Interface defining how the Transaction Management context interacts with the YNAB context.
 * 
 * This port belongs to the Transaction Management bounded context application layer and
 * defines the operations required from YNAB for transaction processing.
 */
trait YnabPort:
    def submitTransaction(transaction: Transaction): ZIO[Any, YnabError, String]
    def getYnabAccounts(): ZIO[Any, YnabError, Seq[YnabAccountRef]]
    def linkSourceAccount(sourceAccountId: SourceAccountId, ynabAccountId: String): ZIO[Any, YnabError, Boolean]
```

## Domain Event

```scala
/**
 * DOMAIN EVENT: Event representing a transaction that has been categorized.
 * 
 * This domain event is emitted by the Transaction Management bounded context when a 
 * transaction has been successfully categorized and can be consumed by other contexts.
 */
case class TransactionCategorized(
    transactionId: TransactionId,
    categoryId: String,
    categoryName: String,
    assignedBy: String,
    assignedAt: LocalDateTime
)
```

## Anti-corruption Layer

```scala
/**
 * ANTI-CORRUPTION LAYER: Translates between YNAB and Transaction Management contexts.
 * 
 * This component sits between the YNAB Integration and Transaction Management bounded contexts,
 * translating concepts and models between them to ensure context integrity.
 */
class YnabTransactionTranslator:
    def toYnabTransaction(transaction: Transaction, account: SourceAccount): YnabTransaction = ???
    def fromYnabTransaction(ynabTransaction: YnabTransaction): Option[Transaction] = ???
```