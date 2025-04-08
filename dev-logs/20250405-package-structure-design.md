# Package Structure Design Document

## Overview

This document outlines the plan for restructuring our codebase to align with Domain-Driven Design (DDD) principles, particularly focusing on organizing code by bounded contexts and architectural layers.

## Current Structure

Currently, our code is organized in a somewhat flat structure:

```
works.iterative.incubator/
  ├── transactions/
  │    ├── SourceAccount.scala
  │    ├── Transaction.scala
  │    ├── ...
  │    └── service/
  │         ├── TransactionRepository.scala
  │         ├── ...
  │         └── ...
  ├── ynab/
  │    ├── YnabConfig.scala
  │    ├── YnabDomain.scala
  │    └── ...
  └── ...
```

## Target Structure

We will reorganize the codebase to clearly reflect distinct bounded contexts with proper layering within each context:

```
works.iterative.incubator/
  ├── transactions/     # Transaction Management Context
  │    ├── domain/      # Domain model, interfaces, domain services
  │    ├── application/ # Use case orchestration, application services
  │    ├── infrastructure/ # Implementation details, repositories
  │    └── web/         # UI components, views, routes
  ├── ynab/             # YNAB Integration Context
  │    ├── domain/
  │    ├── application/
  │    ├── infrastructure/
  │    └── web/
  ├── fio/              # Fio Bank Context
  │    ├── domain/
  │    ├── application/
  │    ├── infrastructure/
  │    └── web/
  ├── categorization/   # Future AI Categorization Context (skeleton only)
  │    ├── domain/
  │    └── application/
  └── auth/             # Future User Management Context (skeleton only)
       ├── domain/
       └── application/
```

## Migration Plan

### Phase 1: Transaction Management Context

1. Create new layer packages:
   - `transactions/domain/`
   - `transactions/application/`
   - `transactions/infrastructure/`
   - `transactions/web/`

2. Migrate domain models:
   - Move `SourceAccount.scala`, `Transaction.scala`, `TransactionId.scala`, etc. to `transactions/domain/model/`
   - Move domain interfaces like `TransactionRepository.scala` to `transactions/domain/repository/`
   - Move query classes to `transactions/domain/query/`

3. Migrate application services:
   - Move `TransactionManagerService.scala` to `transactions/application/service/`
   - Move `TransactionProcessor.scala` to `transactions/application/service/`

4. Migrate infrastructure implementations:
   - Move all PostgreSQL implementations to `transactions/infrastructure/persistence/`
   - Create adapters for external services in `transactions/infrastructure/adapter/`

5. Migrate web components:
   - Move `TransactionViews.scala` and implementations to `transactions/web/view/`
   - Move `TransactionImportModule.scala` to `transactions/web/module/`

### Phase 2: YNAB Integration Context

1. Create new layer packages:
   - `ynab/domain/`
   - `ynab/application/`
   - `ynab/infrastructure/` (placeholder for future implementation)
   - `ynab/web/` (placeholder for future implementation)

2. Migrate domain models:
   - Move `YnabDomain.scala` to `ynab/domain/model/`
   - Create appropriate interfaces in `ynab/domain/service/`

3. Migrate application services:
   - Move `YnabService.scala` interface to `ynab/application/service/`
   - Create adapter interfaces in `ynab/application/port/`

4. Prepare infrastructure placeholders:
   - Create placeholder directory structure for future implementations
   - Move `YnabConfig.scala` to `ynab/infrastructure/config/`

### Phase 3: Fio Bank Context

1. Create new layer packages:
   - `fio/domain/`
   - `fio/application/`
   - `fio/infrastructure/`
   - `fio/web/`

2. Extract domain models:
   - Move `FioTransaction.scala` to `fio/domain/model/`
   - Create appropriate interfaces in `fio/domain/service/`

3. Extract application services:
   - Move Fio-specific service interfaces to `fio/application/service/`
   - Create adapter interfaces in `fio/application/port/`

4. Migrate infrastructure implementations:
   - Move `FioClient.scala`, `FioConfig.scala`, etc. to `fio/infrastructure/`
   - Move `FioTransactionImportService.scala` to `fio/infrastructure/service/`

### Phase 4: Future Contexts (Skeleton Only)

1. Create basic structure for AI Categorization:
   - `categorization/domain/model/`
   - `categorization/domain/service/`
   - `categorization/application/service/`

2. Create basic structure for User Management:
   - `auth/domain/model/`
   - `auth/domain/service/`
   - `auth/application/service/`

## Implementation Guidelines

### Component Classification

Each file should begin with a classification comment:

```scala
/**
 * DOMAIN MODEL: Core business entity representing a bank account.
 */
case class SourceAccount(...) { ... }
```

Classification types:
- DOMAIN MODEL
- DOMAIN SERVICE 
- APPLICATION SERVICE
- INFRASTRUCTURE ADAPTER
- REPOSITORY
- VIEW
- MODULE

### Interface Definitions

Interfaces between bounded contexts should be clearly defined:

```scala
// In transactions/application/port/YnabPort.scala
trait YnabPort:
  def submitTransaction(transaction: Transaction): ZIO[Any, YnabError, Unit]
```

### Dependency Rules

Always respect the dependency rule:
- Domain cannot depend on other layers
- Application can depend on domain only
- Infrastructure can depend on domain and application
- UI can depend on domain and application

### Cross-Context Communication

Communication between bounded contexts should be explicit through:
- Well-defined interfaces (ports)
- Domain events
- Anti-corruption layers when necessary

## Migration Sequence

1. Transaction Management (core entity)
2. YNAB Integration (based on existing interfaces, implementation placeholders only)
3. Fio Bank Integration
4. Future contexts (skeletons only)

## Testing Strategy

- Update import statements in all test files
- Ensure tests pass after each context migration
- Add new tests for any newly created interfaces and implementations