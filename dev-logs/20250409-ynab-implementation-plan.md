# YNAB Implementation Plan - 2025-04-09

## Overview

With the bounded context migration complete, our next priority is implementing the YNAB API integration. This document outlines our plan for implementing the service that will connect our application to the YNAB API.

## Current State

- We have defined the domain models for YNAB in `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/domain/model/`
- We have defined the service interface `YnabService` in `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/application/service/`
- We have configuration classes in `bounded-contexts/ynab/src/main/scala/works/iterative/incubator/ynab/infrastructure/config/`

## What's Missing

We need to implement:

1. **YnabServiceImpl** - The actual implementation of the service interface
2. **HTTP Client** - The client that will make HTTP requests to the YNAB API
3. **YNAB Data Mapping** - Functions to map between our domain models and YNAB API responses
4. **Error Handling** - Proper error handling for API requests
5. **CLI Tool** - A command-line tool for testing the YNAB API integration
6. **Integration with Transaction Processing** - Connect YNAB with transaction processing flow

## Implementation Tasks

### 1. Create HTTP Client for YNAB API (High Priority)

- Create a client class that handles authentication and HTTP requests
- Implement methods for retrieving budgets, accounts, and categories
- Implement methods for submitting transactions
- Use http4s for making HTTP requests

### 2. Implement YnabServiceImpl (High Priority)

- Create `YnabServiceImpl` class that implements the `YnabService` interface
- Use the HTTP client to make API calls
- Implement all required methods:
  - `verifyConnection()`
  - `getBudgets()`
  - `getAccounts()`
  - `getCategoryGroups()`
  - `getCategories()`
  - `createTransaction()`
  - `createTransactions()`
- Add proper error handling and retry logic

### 3. Create YNAB API CLI Tool (Medium Priority)

- Implement a simple CLI tool for testing the YNAB API integration
- Allow testing API token validity
- Enable fetching budgets, accounts, and categories
- Support submitting test transactions
- Use for manual verification before UI integration

### 4. Connect with Transaction Processing (Medium Priority)

- Integrate YNAB service with transaction processing flow
- Implement transaction submission logic
- Update UI to show YNAB sync status

### 5. Add Comprehensive Tests (Medium Priority)

- Write unit tests for YnabServiceImpl with mocked HTTP responses
- Create integration tests that work with a test YNAB budget
- Test error conditions and recovery

## Estimated Timeline

- HTTP Client & YnabServiceImpl: 1-2 days
- CLI Tool: 1 day
- Transaction Processing Integration: 1-2 days
- Tests: 1 day

## Dependencies

- Need YNAB API token for testing
- Need to create test budget in YNAB for development

## Next Steps

1. Start by implementing the HTTP client for the YNAB API
2. Then implement YnabServiceImpl
3. Create the CLI tool for manual testing
4. Connect with transaction processing and UI
5. Add comprehensive tests