# TODO: YNAB Importer Project Completion

This document outlines the remaining tasks needed to complete the YNAB Importer project as described in change request #001/2025.

## Overview

The YNAB Importer project aims to create a tool to import data from Fio bank to YNAB with features including:
- Import data from Fio to PostgreSQL
- Post-process transactions with AI for categorization
- Display transaction state on a web interface
- Allow confirmation and submission to YNAB

## Features

- [Source Account Management](../ynab-importer/features/source_account_management.feature)
- [YNAB Integration](../ynab-importer/features/ynab_integration.feature)
- Transaction Import (to be defined)
- Transaction Processing (to be defined)

## Completed Tasks

- ✅ Initial PostgreSQL database schema
- ✅ Source Account entity and repository
- ✅ Source Account UI implementation (listing, details, creation, editing)
- ✅ Base Transaction entity and repository

## Remaining Tasks

### 1. Complete Source Account Management

- ✅ Complete the Source Account repository update (fix the create pattern)
- ✅ Enhance SourceAccount UI to use the new repository pattern

### 2. YNAB Integration (Prioritized)

See [YNAB Integration Feature](../ynab-importer/features/ynab_integration.feature)

#### Architecture Restructuring
- [✅] Restructure YNAB integration into DDD-aligned packages
- [✅] Split domain models into separate files
- [✅] Create service interfaces in application layer
- [✅] Define port interfaces for cross-context communication

#### Implementation
- [✅] Create YNAB configuration model with API token storage
- [ ] Implement YNAB API client
- [ ] Add authentication with YNAB
- [ ] Create YNAB budget selection functionality
- [ ] Implement YNAB account lookup for SourceAccount configuration
- [ ] Add YNAB account selection dropdown in Source Account UI
- [ ] Implement YNAB category synchronization for categorization
- [ ] Build transaction submission service
- [ ] Add batch submission functionality
- [ ] Implement error handling and retry mechanisms
- [ ] Add transaction status tracking for YNAB submissions

### 3. Data Import Module

Transaction Import Feature (to be defined)

- [ ] Complete the FioTransactionImportService implementation
- [ ] Add authentication for Fio Bank API
- [ ] Implement transaction retrieval and mapping
- [ ] Create scheduled import mechanism or manual trigger UI
- [ ] Add transaction deduplication logic

### 4. Database and Repository Refinements

- [ ] Implement TransactionProcessingState repository
- [ ] Add transaction history and status tracking
- [ ] Create efficient query mechanisms for transaction filtering

### 5. Transaction Management UI

- [ ] Build transaction list view with filtering and sorting
- [ ] Create transaction detail view
- [ ] Implement categorization review UI
- [ ] Add batch operations (approve, edit, submit)
- [ ] Create dashboard for transaction status overview

### 6. AI Categorization System

- [ ] Design and implement AI categorization service
- [ ] Create a prompt engineering strategy for OpenAI integration
- [ ] Build categorization rules and mapping to YNAB categories
- [ ] Implement confidence scoring for AI suggestions
- [ ] Add manual override capabilities

### 7. User Authentication

- [ ] Implement admin authentication system
- [ ] Add login/logout functionality
- [ ] Create session management

### 8. Testing and Quality Assurance

- [ ] Complete unit tests for all core services
- [ ] Add integration tests for external API interactions
- [✅] Set up end-to-end testing framework with Playwright
- [🔄] Implement e2e tests for Source Account Management
- [ ] Implement e2e tests for YNAB integration
- [ ] Implement performance testing for database operations
- [ ] Test error handling and edge cases

### 9. Deployment and Operations

- [ ] Configure Docker containers for all components
- [ ] Create environment-specific configuration
- [ ] Write deployment scripts
- [ ] Set up logging and monitoring
- [ ] Add error reporting mechanisms

### 10. Documentation

- [ ] Update architecture documentation
- [ ] Create user manual for the web interface
- [ ] Document API endpoints and integration points
- [ ] Add maintenance and troubleshooting guides

## Revised Priority Order

1. Implement YNAB API client and account/category integration
2. Complete Source Account repository pattern implementation
3. Implement Fio Bank transaction import
4. Build transaction management UI
5. Create AI categorization system
6. Add user authentication
7. Complete testing and documentation
8. Configure deployment

## Timeline Reminder

- Expected Start: 15.03.2025
- Delivery to Testing: 05.04.2025
- Required Production Deployment: 15.04.2025

## Someday / Maybe

- [ ] Extract transactor support code to a shared module
- [ ] Extract testcontainer support to a shared module
- [ ] Make each web module separately startable as a web server
- [ ] Use tapir for web module endpoints
