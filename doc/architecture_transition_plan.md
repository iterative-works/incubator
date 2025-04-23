# Architecture Transition Plan: BDD-Driven UI-First Approach

## Overview

This document outlines the steps to transition our current architecture to the BDD-Driven UI-First approach with Vertical Slices. The plan is guided by the Gherkin scenarios in `BUDGET-001.feature` and follows the implementation workflow described in our development documentation.

## Goals

1. Restructure the codebase to align with BDD scenarios
2. Enable UI-first development with mock services
3. Maintain the functional core architecture
4. Create a clear path for incremental implementation
5. Enable faster feedback cycles with users

## Current vs. Target Architecture

### Current Structure
```
src/
  └── main/
      └── scala/
          └── works/iterative/incubator/
              ├── transactions/       # Bounded Context
              │   └── ...
              ├── fio/                # Bounded Context
              │   └── ...
              └── ynab/               # Bounded Context
                  └── ...
```

### Target Structure
```
src/
  └── main/
      └── scala/
          └── works/iterative/incubator/
              └── budget/            # Bounded Context
                  ├── domain/         # Shared Domain Core
                  │   └── ...
                  ├── imports/         # Import Module
                  │   └── ...
                  ├── categorization/ # Categorization Module
                  │   └── ...
                  ├── submission/     # Submission Module
                  │   └── ...
                  └── management/     # Transaction Management Module
                      └── ...
```

## Transition Phases

### Phase 1: Preparation (1-2 weeks)

#### 1.1 Domain Core Extraction

- [x] Create the `budget/domain` package
- [x] Move core entities from transactions context to shared domain core
  - [x] Transaction
  - [x] TransactionProcessingState
  - [x] SourceAccount
  - [x] Category
- [x] Create repository interfaces in domain core
  - [x] TransactionRepository
  - [x] TransactionProcessingStateRepository
  - [x] SourceAccountRepository
  - [x] CategoryRepository
- [x] Define domain events based on scenarios
  - [x] TransactionImported
  - [x] TransactionCategorized
  - [x] TransactionSubmitted

#### 1.2 Mock Service Framework

- [x] Create a mock implementation framework
- [x] Implement in-memory repositories
  - [x] InMemoryTransactionRepository
  - [x] InMemoryTransactionProcessingStateRepository
  - [x] InMemorySourceAccountRepository
  - [x] InMemoryCategoryRepository

#### 1.3 Module Structure Setup

- [x] Create basic structure for the Import Module (using 'imports' package)
- [x] Create basic structure for the Categorization Module
- [ ] Create basic structure for the Submission Module
- [ ] Create basic structure for the Transaction Management Module
- [ ] Update build configuration for new structure

### Phase 2: Import Module Implementation (1-2 weeks)

This phase focuses on implementing Scenarios 1 and 9 from the BUDGET-001.feature.

#### 2.1 Domain Layer

- [ ] Create `ImportService` interface
- [ ] Implement `DateRangeValidator`
- [ ] Define `ImportBatch` entity
- [ ] Create `ImportError` value object

#### 2.2 Mock Infrastructure

- [ ] Implement mock `FioClient`
- [ ] Create mock import data
- [ ] Set up test scenarios with mock repositories

#### 2.3 UI Layer

- [ ] Create `ImportViewModel`
- [ ] Build `ImportView` with date range selection
- [ ] Implement validation feedback UI
- [ ] Create transaction table view
- [ ] Implement `ImportModule` with routes

#### 2.4 Testing

- [ ] Write domain-level tests for scenarios 1 and 9
- [ ] Create UI-level tests for import form and validation
- [ ] Test complete import user journey

### Phase 3: Categorization Module Implementation (1-2 weeks)

This phase focuses on implementing Scenarios 2, 3, and 4 from the BUDGET-001.feature.

#### 3.1 Domain Layer

- [ ] Create `CategorizationService` interface
- [ ] Implement `CategorySuggestionEngine`
- [ ] Define `Category` value object
- [ ] Create `AuditEntry` for category changes

#### 3.2 Mock Infrastructure

- [ ] Implement mock `AIClient` for categorization
- [ ] Create mock category suggestions
- [ ] Set up auditing for category changes

#### 3.3 UI Layer

- [ ] Create `CategoryViewModel`
- [ ] Build category selection UI components
- [ ] Implement single transaction editing UI
- [ ] Create bulk editing UI
- [ ] Implement `CategorizationModule` with routes

#### 3.4 Testing

- [ ] Write domain-level tests for scenarios 2, 3, and 4
- [ ] Create UI-level tests for category editing
- [ ] Test complete categorization user journeys

### Phase 4: Remaining Modules (2-3 weeks)

Complete the implementation of the remaining modules following the same pattern.

#### 4.1 Submission Module

- [ ] Implement domain layer for scenarios 5, 6, and 7
- [ ] Create mock YNAB client
- [ ] Build submission UI
- [ ] Test submission scenarios

#### 4.2 Transaction Management Module

- [ ] Implement domain layer for scenario 8
- [ ] Create filtering components
- [ ] Build transaction management UI
- [ ] Test filtering scenario

### Phase 5: Infrastructure Implementation (2-3 weeks)

#### 5.1 Database Layer

- [ ] Implement PostgreSQL repositories
- [ ] Migrate database schema if needed
- [ ] Test repositories against scenario requirements

#### 5.2 External Service Integration

- [ ] Implement real Fio Bank client
- [ ] Create real YNAB integration
- [ ] Implement AI service connection
- [ ] Test integrations against scenarios

#### 5.3 Production Configuration

- [ ] Set up environment-based configuration
- [ ] Create deployment scripts
- [ ] Implement feature flags if needed

## Transition Approach

### Incremental Migration

We'll use an incremental approach to minimize disruption:

1. Start with the Import Module as proof of concept
2. Run the new implementation alongside the existing code
3. Gradually migrate other features to the new architecture
4. Deprecate and remove old implementations when no longer needed

### Testing Strategy

- Domain-level tests: Pure business logic testing
- UI-level tests: Component and interaction testing
- E2E tests: Complete scenario testing
- User acceptance: Get feedback on UI implementation

### Key Milestones

1. [Week 2] Shared domain core and mock framework complete
2. [Week 4] Import Module fully implemented with UI
3. [Week 6] Categorization Module complete
4. [Week 9] All modules implemented with mock services
5. [Week 12] Complete system with real infrastructure

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Team unfamiliar with new approach | Medium | Pair programming, knowledge sharing sessions |
| Integration with existing code | High | Clear interfaces, careful refactoring, comprehensive tests |
| Performance of mock services | Low | Performance testing early, optimize if needed |
| User experience issues | Medium | Early user testing with UI prototypes |
| Schedule delays | Medium | Prioritize modules by business value, focus on MVS first |

## Conclusion

This transition plan provides a structured approach to migrating our architecture to a BDD-Driven UI-First development model with vertical slices. By focusing on incremental implementation and early user validation, we can minimize risks while making progress toward a more maintainable and user-focused architecture.

Progress will be tracked in our project management system with tasks mapped directly to scenarios in our feature files.

## Progress Updates

### 2025-04-25: Completed Phase 1.2 - Mock Service Framework

We've successfully implemented the mock service framework as the second step in our transition plan. This infrastructure enables our BDD-driven UI-first development approach, allowing us to build and validate user interfaces before implementing real infrastructure.

The following components have been created:

1. **In-Memory Repository Implementations**:
   - InMemoryTransactionRepository
   - InMemoryTransactionProcessingStateRepository
   - InMemorySourceAccountRepository
   - InMemoryCategoryRepository

2. **Test Data Generation Framework**:
   - TestDataGenerator with utilities for creating realistic test data
   - ScenarioTestData with data factories mapped to specific Gherkin scenarios
   - Support for all scenarios from our feature files

3. **Integration Facilities**:
   - MockRepositories with convenient ZLayers for different testing scenarios
   - Scenario-based repository layers for specific test cases
   - Test utilities for setting up mock environments

This framework provides several key capabilities:
- Realistic data generation for UI development and testing
- Direct connection between test data and Gherkin scenarios
- In-memory repositories that work with the real domain interfaces
- Easy integration into ZIO applications

With these mock services in place, we can now proceed to implementing the UI modules using a true UI-first approach, where we build and validate the user experience before committing to real infrastructure implementations.

### 2025-04-23: Completed Phase 1.1 - Domain Core Extraction

We've successfully completed the first step of our transition plan, extracting the core domain model into the new `budget` bounded context. The following components have been created:

1. **Core Entities**:
   - Transaction and TransactionId
   - TransactionProcessingState and TransactionStatus
   - SourceAccount and CreateSourceAccount
   - Category

2. **Repository Interfaces**:
   - TransactionRepository
   - TransactionProcessingStateRepository
   - SourceAccountRepository
   - CategoryRepository

3. **Domain Events**:
   - TransactionImported
   - TransactionCategorized
   - TransactionSubmitted

All these domain components have been migrated to the new package structure and are ready for use in the subsequent phases of our implementation. The interfaces maintain the same contract as before, ensuring compatibility with existing code while we gradually migrate to the new architecture.
