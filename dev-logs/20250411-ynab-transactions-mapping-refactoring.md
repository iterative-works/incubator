# YNAB-Transactions Context Decoupling Refactoring

## Problem

The current domain model incorrectly couples the Transaction context with the YNAB context by including a YNAB account ID directly in the `SourceAccount` entity. This creates a direct dependency from our core transactions domain to an external system (YNAB), which violates proper domain boundaries and makes it difficult to extend the system to support other budgeting tools in the future.

## Solution

We have implemented a proper anti-corruption layer (ACL) between the Transaction context and the YNAB context by:

1. Removing the `ynabAccountId` field from the `SourceAccount` entity
2. Creating a new `YnabAccountMapping` entity in the YNAB context that maps source accounts to YNAB accounts
3. Creating a repository interface `YnabAccountMappingRepository` and PostgreSQL implementation
4. Adding a SQL migration to create the new mapping table and migrate existing data
5. Updating the `YnabTransactionImportService` to use the mapping repository

## Migration Steps

### Completed
- Removed `ynabAccountId` from `SourceAccount` model
- Created new `YnabAccountMapping` model and repository
- Added SQL migration to create mapping table and migrate existing data
- Created PostgreSQL implementation of the mapping repository
- Updated the `YnabTransactionImportService` to use the mapping repository

### Remaining Tasks
- Update the UI to allow users to configure YNAB account mappings
- Update any existing code that directly references the YNAB account ID from source accounts
- Run the migration in development and test environments
- Update tests to reflect the new structure
- Deploy to production with the migration

## Benefits

- Proper separation of concerns between the Transaction and YNAB contexts
- The Transaction context is now independent of YNAB and can be used with other budgeting tools
- The YNAB context properly acts as an adapter/ACL to the YNAB API
- Follows domain-driven design principles for bounded contexts
- Makes it easier to add support for other budgeting tools in the future

## Migration Impact

This change requires a database migration that creates a new table and moves data from the source_accounts table to the new ynab_account_mappings table. The migration is designed to be non-destructive, maintaining all existing mappings.

## Testing Plan

1. Verify that all existing YNAB account mappings are migrated correctly
2. Ensure that transaction imports continue to work with the new structure
3. Test the new mapping repository with both existing and new accounts
4. Verify the UI changes allow proper configuration of account mappings