# TODO: YNAB Importer Project Completion

This document outlines the remaining tasks needed to complete the YNAB Importer project as described in change request #001/2025.

## Overview

The YNAB Importer project aims to create a tool to import data from Fio bank to YNAB with features including:
- Import data from Fio to PostgreSQL
- Post-process transactions with AI for categorization
- Display transaction state on a web interface
- Allow confirmation and submission to YNAB

## Completed Tasks

- ✅ Initial PostgreSQL database schema
- ✅ Source Account entity and repository
- ✅ Source Account UI implementation (listing, details, creation, editing)
- ✅ Base Transaction entity and repository

## Remaining Tasks

### 1. Complete Source Account Management

- [ ] Complete the Source Account repository update (fix the create pattern)
- [ ] Enhance SourceAccount UI to use the new repository pattern

### 1. YNAB Integration (Prioritized)

- [ ] Implement YNAB API client
- [ ] Add authentication with YNAB
- [ ] Create YNAB account lookup for SourceAccount configuration
- [ ] Add YNAB account selection dropdown in Source Account UI using YNAB API data
- [ ] Implement YNAB category synchronization for categorization
- [ ] Build transaction submission service
- [ ] Add submission confirmation and error handling

### 3. Data Import Module

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
- [ ] Create end-to-end test scenarios
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
