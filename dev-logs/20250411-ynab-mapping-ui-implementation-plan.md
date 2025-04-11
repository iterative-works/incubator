# YNAB Account Mapping UI Implementation Plan

## Overview

As part of the refactoring to properly separate the transactions and YNAB contexts, we've implemented a new anti-corruption layer with a dedicated `YnabAccountMapping` model. This document outlines the implementation and migration plan for a dedicated UI that allows users to manage these mappings.

## UI Components

### YnabAccountMappingModule

A web module that provides:
- List view of all YNAB account mappings
- Form to create new mappings
- Form to edit existing mappings
- Ability to delete mappings

### YnabAccountMappingViews

View implementation with:
- Account mapping list view
- Mapping form (create/edit)
- Error views

## Integration with Existing UI

1. Add a link to the YNAB account mappings in the source account detail view
2. Remove the YNAB account ID field from the source account form
3. Update the source account list view to link to the YNAB mappings section

## Migration Steps

1. Deploy the database migration to create the `ynab_account_mappings` table
2. Enable the new YNAB account mapping module
3. Verify that existing mappings are accessible in the new UI
4. Test creating, editing, and deleting mappings
5. Update user documentation to explain the new mapping approach

## User Flow

1. User creates a source account in the transactions section
2. After creating the source account, the user goes to the YNAB section
3. In the YNAB Account Mappings page, the user creates a new mapping for the source account
4. The user selects the appropriate YNAB account for the mapping
5. Transactions imported from the source account will now be correctly routed to the mapped YNAB account

## Benefits

- Clear separation of concerns between transactions and YNAB contexts
- Dedicated UI for managing YNAB-specific configurations
- Better support for future extensions to other budgeting systems
- Improved maintainability of the codebase

## Testing Plan

1. Verify that the migration script correctly populates the new table
2. Test the UI for creating new mappings
3. Test the UI for editing existing mappings
4. Test transaction import with the new mapping approach
5. Verify that the source account UI correctly links to the YNAB mappings section