# Migration Task List for Bounded Context Restructuring

This document outlines the tasks needed to complete the migration of our codebase to the new bounded context structure. Each bounded context will follow the same migration pattern, focusing on one layer at a time.

## Transaction Context Migration

### Domain Layer
- [ ] Move domain models to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/domain/model/`
  - [ ] SourceAccount.scala
  - [ ] Transaction.scala
  - [ ] TransactionId.scala
  - [ ] TransactionProcessingState.scala
  - [ ] TransactionStatus.scala
- [ ] Move query objects to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/domain/query/`
  - [ ] SourceAccountQuery.scala
  - [ ] TransactionProcessingStateQuery.scala
  - [ ] TransactionQuery.scala
- [ ] Move repository interfaces to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/domain/repository/`
  - [ ] SourceAccountRepository.scala
  - [ ] TransactionProcessingStateRepository.scala
  - [ ] TransactionRepository.scala

### Application Layer
- [ ] Move service interfaces to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/application/service/`
  - [ ] TransactionImportService.scala
  - [ ] TransactionManagerService.scala
  - [ ] TransactionProcessor.scala
- [ ] Move ports to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/application/port/`
  - [ ] TransactionPort.scala

### Infrastructure Layer
- [ ] Move codecs to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/infrastructure/`
  - [ ] Codecs.scala
  - [ ] DbCodecs.scala
- [ ] Move service implementations to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/infrastructure/service/`
  - [ ] DefaultTransactionManagerService.scala
  - [ ] DefaultTransactionProcessor.scala
  - [ ] FlywayMigrationService.scala
- [ ] Move configuration to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/infrastructure/config/`
  - [ ] PosgreSQLDatabaseModule.scala
  - [ ] PostgreSQLConfig.scala
  - [ ] PostgreSQLDataSource.scala
  - [ ] PostgreSQLTransactor.scala
- [ ] Move repository implementations to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/infrastructure/persistence/`
  - [ ] InMemoryTransactionRepository.scala
  - [ ] PostgreSQLSourceAccountRepository.scala
  - [ ] PostgreSQLTransactionProcessingStateRepository.scala
  - [ ] PostgreSQLTransactionRepository.scala

### Web Layer
- [ ] Move modules to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/web/module/`
  - [ ] SourceAccountModule.scala
  - [ ] TransactionImportModule.scala
- [ ] Move views to `bounded-contexts/transactions/src/main/scala/works/iterative/incubator/transactions/web/view/`
  - [ ] SourceAccountViews.scala
  - [ ] SourceAccountViewsImpl.scala
  - [ ] TransactionViews.scala
  - [ ] TransactionViewsImpl.scala
  - [ ] TransactionWithState.scala

## YNAB Context Migration

### Domain Layer
- [x] Move domain models to `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/domain/model/`
  - [x] YnabAccount.scala
  - [x] YnabApiError.scala
  - [x] YnabBudget.scala
  - [x] YnabCategory.scala
  - [x] YnabCategoryGroup.scala
  - [x] YnabTransaction.scala
  - [x] YnabTransactionImportResult.scala

### Application Layer
- [x] Move ports to `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/application/port/`
  - [x] YnabTransactionPort.scala
- [x] Move service interfaces to `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/application/service/`
  - [x] YnabService.scala
  - [x] YnabTransactionImportService.scala

### Infrastructure Layer
- [x] Move configuration to `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/infrastructure/config/`
  - [x] YnabConfig.scala

(Note: YnabServiceImpl will be implemented later according to the integration plan)

## Fio Context Migration

### Domain Layer
- [x] Move domain models to `bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/domain/model/`
  - [x] FioAccount.scala
  - [x] FioTransaction.scala

### Application Layer
- [x] Move ports to `bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/application/port/`
  - [x] TransactionPort.scala
- [x] Move service interfaces to `bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/application/service/`
  - [x] FioImportService.scala

### Infrastructure Layer
- [x] Move client implementation to `bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/client/`
  - [x] FioClient.scala
  - [x] FioCodecs.scala
- [x] Move configuration to `bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/config/`
  - [x] FioConfig.scala
- [x] Move service implementations to `bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/service/`
  - [x] FioTransactionImportService.scala

## Categorization Context Migration

### Domain Layer
- [x] Move domain models to `bounded-contexts/categorization/src/main/scala/works/iterative/incubator/categorization/domain/model/`
  - [x] CategoryRule.scala
  - [x] CategorySuggestion.scala

### Application Layer
- [x] Move service interfaces to `bounded-contexts/categorization/src/main/scala/works/iterative/incubator/categorization/application/service/`
  - [x] CategorizationService.scala

## Auth Context Migration

### Domain Layer
- [x] Move domain models to `bounded-contexts/auth/src/main/scala/works/iterative/incubator/auth/domain/model/`
  - [x] Permission.scala
  - [x] Role.scala
  - [x] User.scala

### Application Layer
- [x] Move service interfaces to `bounded-contexts/auth/src/main/scala/works/iterative/incubator/auth/application/service/`
  - [x] AuthenticationService.scala
  - [x] UserService.scala

## Final Steps

- [ ] Update import statements in all files
- [ ] Determine and implement the correct approach for backward compatibility
  - [ ] Research the proper export directive syntax or alternative approaches
  - [ ] Apply the chosen approach across all migrated files
- [x] Compile and verify all bounded contexts
- [ ] Run tests to ensure functionality is preserved
- [ ] Update documentation to reflect new structure