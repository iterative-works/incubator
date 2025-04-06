# Package Restructuring by Bounded Context - Task List

## Overview

This document provides a detailed, actionable task list for implementing the package restructuring described in the [package structure design document](20260405-package-structure-design.md). Each task is specific and can be implemented incrementally, allowing for continuous integration and testing throughout the migration process.

## Task List

### Phase 1: Transaction Management Context Restructuring

#### Preparation
- [x] 1.1 Create new package structure in core module:
  ```
  works.iterative.incubator.transactions.domain.model
  works.iterative.incubator.transactions.domain.repository
  works.iterative.incubator.transactions.domain.query
  works.iterative.incubator.transactions.domain.service
  works.iterative.incubator.transactions.application.service
  works.iterative.incubator.transactions.application.port
  ```

- [x] 1.2 Create new package structure in infrastructure module:
  ```
  works.iterative.incubator.transactions.infrastructure.persistence
  works.iterative.incubator.transactions.infrastructure.adapter
  works.iterative.incubator.transactions.infrastructure.config
  ```

- [x] 1.3 Create new package structure in web module:
  ```
  works.iterative.incubator.transactions.web.view
  works.iterative.incubator.transactions.web.module
  ```

#### Domain Model Migration
- [x] 1.4 Move `Transaction.scala` to `transactions.domain.model` and add classification comment
- [x] 1.5 Move `TransactionId.scala` to `transactions.domain.model` and add classification comment
- [x] 1.6 Move `TransactionStatus.scala` to `transactions.domain.model` and add classification comment
- [x] 1.7 Move `SourceAccount.scala` to `transactions.domain.model` and add classification comment
- [x] 1.8 Move `TransactionProcessingState.scala` to `transactions.domain.model` and add classification comment

#### Domain Query Migration
- [x] 1.9 Move `TransactionQuery.scala` to `transactions.domain.query` and add classification comment
- [x] 1.10 Move `SourceAccountQuery.scala` to `transactions.domain.query` and add classification comment
- [x] 1.11 Move `TransactionProcessingStateQuery.scala` to `transactions.domain.query` and add classification comment

#### Domain Repository Interface Migration
- [x] 1.12 Move `TransactionRepository.scala` to `transactions.domain.repository` and add classification comment
- [x] 1.13 Move `SourceAccountRepository.scala` to `transactions.domain.repository` and add classification comment
- [x] 1.14 Move `TransactionProcessingStateRepository.scala` to `transactions.domain.repository` and add classification comment

#### Application Service Migration
- [x] 1.15 Move `TransactionManagerService.scala` to `transactions.application.service` and add classification comment
- [x] 1.16 Move `TransactionProcessor.scala` to `transactions.application.service` and add classification comment
- [x] 1.17 Move `TransactionImportService.scala` to `transactions.application.service` and add classification comment

#### Infrastructure Repository Implementation Migration
- [x] 1.18 Move `PostgreSQLTransactionRepository.scala` to `transactions.infrastructure.persistence` and add classification comment
- [x] 1.19 Move `PostgreSQLSourceAccountRepository.scala` to `transactions.infrastructure.persistence` and add classification comment
- [x] 1.20 Move `PostgreSQLTransactionProcessingStateRepository.scala` to `transactions.infrastructure.persistence` and add classification comment
- [x] 1.21 Move `InMemoryTransactionRepository.scala` to `transactions.infrastructure.persistence` and add classification comment

#### Infrastructure Service Implementation Migration
- [x] 1.22 Move `DefaultTransactionManagerService.scala` to `transactions.infrastructure.service` and add classification comment
- [x] 1.23 Move `DefaultTransactionProcessor.scala` to `transactions.infrastructure.service` and add classification comment

#### Infrastructure Configuration Migration
- [x] 1.24 Move `PostgreSQLConfig.scala` to `transactions.infrastructure.config` and add classification comment
- [x] 1.25 Move `PostgreSQLDataSource.scala` to `transactions.infrastructure.config` and add classification comment
- [x] 1.26 Move `PostgreSQLTransactor.scala` to `transactions.infrastructure.config` and add classification comment
- [x] 1.27 Move `PosgreSQLDatabaseModule.scala` to `transactions.infrastructure.config` and add classification comment

#### Web View Migration
- [x] 1.28 Move `TransactionViews.scala` to `transactions.web.view` and add classification comment
- [x] 1.29 Move `TransactionViewsImpl.scala` to `transactions.web.view` and add classification comment
- [x] 1.30 Move `SourceAccountViews.scala` to `transactions.web.view` and add classification comment
- [x] 1.31 Move `SourceAccountViewsImpl.scala` to `transactions.web.view` and add classification comment
- [x] 1.32 Move `TransactionWithState.scala` to `transactions.web.view` and add classification comment

#### Web Module Migration
- [x] 1.33 Move `TransactionImportModule.scala` to `transactions.web.module` and add classification comment
- [x] 1.34 Move `SourceAccountModule.scala` to `transactions.web.module` and add classification comment

#### Integration Testing
- [x] 1.35 Compile and test Transaction Management context after migration
- [ ] 1.36 Update import statements in all test files referring to moved classes
- [ ] 1.37 Run integration tests to ensure all functionality works correctly

### Phase 2: YNAB Integration Context Restructuring

#### Preparation
- [x] 2.1 Create new package structure in core module:
  ```
  works.iterative.incubator.ynab.domain.model
  works.iterative.incubator.ynab.domain.service
  works.iterative.incubator.ynab.application.service
  works.iterative.incubator.ynab.application.port
  works.iterative.incubator.ynab.infrastructure.config
  ```

#### Domain Model Migration
- [x] 2.2 Move domain models from `YnabDomain.scala` to separate files in `ynab.domain.model`:
  - [x] 2.2.1 Create `YnabBudget.scala` in `ynab.domain.model`
  - [x] 2.2.2 Create `YnabAccount.scala` in `ynab.domain.model`
  - [x] 2.2.3 Create `YnabCategory.scala` in `ynab.domain.model`
  - [x] 2.2.4 Create `YnabCategoryGroup.scala` in `ynab.domain.model`
  - [x] 2.2.5 Create `YnabTransaction.scala` in `ynab.domain.model`
  - [x] 2.2.6 Create `YnabApiError.scala` in `ynab.domain.model`
  - [x] 2.2.7 Create `YnabTransactionImportResult.scala` in `ynab.domain.model`

#### Application Service Migration
- [x] 2.3 Move `YnabService.scala` interface to `ynab.application.service` and add classification comment
- [x] 2.4 Move `YnabTransactionImportService.scala` interface to `ynab.application.service`

#### Infrastructure Configuration
- [x] 2.5 Move `YnabConfig.scala` to `ynab.infrastructure.config` and add classification comment

#### Create Ports
- [x] 2.6 Create `TransactionPort.scala` in `transactions.application.port` to define interface from Transaction context to YNAB
- [x] 2.6.1 Create `YnabTransactionPort.scala` in `ynab.application.port` to define interface to YNAB integration

#### Integration Testing
- [x] 2.7 Compile and test YNAB Integration context after migration
- [ ] 2.8 Update import statements in all test files referring to moved classes
- [ ] 2.9 Run integration tests to ensure all functionality works correctly

### Phase 3: Fio Bank Context Restructuring

#### Preparation
- [ ] 3.1 Create new package structure:
  ```
  works.iterative.incubator.fio.domain.model
  works.iterative.incubator.fio.domain.service
  works.iterative.incubator.fio.application.service
  works.iterative.incubator.fio.application.port
  works.iterative.incubator.fio.infrastructure.client
  works.iterative.incubator.fio.infrastructure.config
  works.iterative.incubator.fio.infrastructure.service
  ```

#### Domain Model Migration
- [ ] 3.2 Move `FioTransaction.scala` to `fio.domain.model` and add classification comment
- [ ] 3.3 Create `FioAccount.scala` in `fio.domain.model` for Fio-specific account data

#### Application Service
- [ ] 3.4 Create `FioImportService.scala` interface in `fio.application.service`

#### Infrastructure Migration
- [ ] 3.5 Move `FioClient.scala` to `fio.infrastructure.client` and add classification comment
- [ ] 3.6 Move `FioConfig.scala` to `fio.infrastructure.config` and add classification comment
- [ ] 3.7 Move `FioCodecs.scala` to `fio.infrastructure.client` and add classification comment
- [ ] 3.8 Move `FioTransactionImportService.scala` to `fio.infrastructure.service` and add classification comment

#### Create Ports
- [ ] 3.9 Create `TransactionPort.scala` in `fio.application.port` to define interface from Fio to Transaction context

#### Integration Testing
- [ ] 3.10 Compile and test Fio Bank context after migration
- [ ] 3.11 Update import statements in all test files referring to moved classes
- [ ] 3.12 Run integration tests to ensure all functionality works correctly

### Phase 4: Future Contexts (Skeleton Only)

#### AI Categorization Context
- [ ] 4.1 Create basic package structure:
  ```
  works.iterative.incubator.categorization.domain.model
  works.iterative.incubator.categorization.domain.service
  works.iterative.incubator.categorization.application.service
  ```

- [ ] 4.2 Create placeholder domain models:
  - [ ] 4.2.1 Create `CategorySuggestion.scala` in `categorization.domain.model`
  - [ ] 4.2.2 Create `CategoryRule.scala` in `categorization.domain.model`

- [ ] 4.3 Create placeholder service interfaces:
  - [ ] 4.3.1 Create `CategorizationService.scala` in `categorization.application.service`

#### User Management Context
- [ ] 4.4 Create basic package structure:
  ```
  works.iterative.incubator.auth.domain.model
  works.iterative.incubator.auth.domain.service
  works.iterative.incubator.auth.application.service
  ```

- [ ] 4.5 Create placeholder domain models:
  - [ ] 4.5.1 Create `User.scala` in `auth.domain.model`
  - [ ] 4.5.2 Create `Role.scala` in `auth.domain.model`
  - [ ] 4.5.3 Create `Permission.scala` in `auth.domain.model`

- [ ] 4.6 Create placeholder service interfaces:
  - [ ] 4.6.1 Create `UserService.scala` in `auth.application.service`
  - [ ] 4.6.2 Create `AuthenticationService.scala` in `auth.application.service`

## Testing and Verification

- [ ] 5.1 Compile the entire project after each context migration
- [ ] 5.2 Run all unit tests after each context migration
- [ ] 5.3 Run all integration tests after each context migration
- [ ] 5.4 Verify application functionality manually through UI after all migrations

## Documentation Updates

- [ ] 6.1 Update architecture documentation with new package structure
- [ ] 6.2 Create package diagram showing the new organization
- [ ] 6.3 Update developer onboarding documentation to reflect new structure
- [ ] 6.4 Document inter-context communication patterns and interfaces
