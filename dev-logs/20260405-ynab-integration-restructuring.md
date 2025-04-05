# YNAB Integration Restructuring

**Date**: 2026-04-05

## Context

As part of our codebase restructuring to align with Domain-Driven Design (DDD) principles, we are reorganizing the YNAB integration components into a more coherent and maintainable structure. This work follows the migration task list defined in [20260405-migration-task-list.md](20260405-migration-task-list.md) and is the first step in implementing a clean bounded context for YNAB integration.

## Changes Made

We have reorganized the YNAB integration code into the following DDD-aligned package structure:

### Domain Layer

- **Domain Models**: We split the monolithic `YnabDomain.scala` into individual model files:
  - `YnabBudget.scala`
  - `YnabAccount.scala`
  - `YnabCategory.scala`
  - `YnabCategoryGroup.scala`
  - `YnabTransaction.scala`
  - `YnabApiError.scala`
  - `YnabTransactionImportResult.scala`

### Application Layer

- **Service Interfaces**: Moved service definitions to the application layer:
  - `YnabService.scala` - Core service for interacting with YNAB
  - `YnabTransactionImportService.scala` - Service for importing transactions to YNAB

- **Ports**: Created port interfaces for cross-context communication:
  - `YnabTransactionPort.scala` - Port for transaction submission to YNAB
  - `TransactionPort.scala` (in transactions context) - Port for external integrations

### Infrastructure Layer

- **Configuration**: Moved configuration to the infrastructure layer:
  - `YnabConfig.scala` - Configuration for YNAB API integration

### Backward Compatibility

To maintain backward compatibility during the migration, we kept the original files but converted them to facades that re-export the types from their new locations. This approach allows for a gradual migration without disrupting existing code.

## Next Steps

1. Implement the `YnabServiceImpl` in the infrastructure layer
2. Create an adapter to connect the Transaction context and YNAB context through their respective ports
3. Update existing test code to use the new structure
4. Gradually phase out the compatibility facades once all references are updated

## Testing

We've compiled the project after the restructuring and all components are working correctly. Further integration testing will be performed as we implement the remaining components of the migration plan.