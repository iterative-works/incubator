# Bounded Context Analysis of YNAB Importer Project

This document outlines the current and planned bounded contexts for the YNAB Importer project, along with their relationships and recommendations for architectural alignment. This analysis serves as a foundation for organizing our codebase according to Domain-Driven Design principles.

## Current Bounded Contexts

### 1. Transaction Management Context

**Characteristics:**
- **Purpose**: Managing bank transactions from import to processing
- **Core Entities**: Transaction, TransactionProcessingState, SourceAccount
- **Domain Language**: Transactions, accounts, import, processing
- **Business Process**: Import → Process → Track → Submit
- **Data Ownership**: Responsible for bank transaction data
- **Repository Layer**: TransactionRepository, SourceAccountRepository

This bounded context handles the complete lifecycle of financial transactions from initial import from the bank through processing, categorization, and eventual submission to YNAB.

### 2. YNAB Integration Context

**Characteristics:**
- **Purpose**: Interacting with YNAB APIs and budgeting features
- **Core Entities**: YnabBudget, YnabAccount, YnabCategory, YnabTransaction
- **Domain Language**: Budgets, accounts, categories specific to YNAB's model
- **Business Process**: Connect → Configure → Submit → Track
- **Data Ownership**: YNAB-specific entities and mappings
- **Repository Layer**: N/A (still in development)

This context is responsible for the specific interaction with YNAB, handling the mapping between local transaction models and YNAB's domain model, and managing the integration points.

### 3. Fio Bank Integration Context (Adapter)

**Characteristics:**
- **Purpose**: Integration with Fio Bank API
- **Core Entities**: FioTransaction, FioConfig
- **Domain Language**: Specific to Fio Bank's API and data model
- **Business Process**: Connect → Fetch → Transform
- **Repository Layer**: N/A (adapter to Transaction Management)

This context is currently implemented as an adapter within the Transaction Management context, but it represents a distinct integration point with its own language and models.

## Planned Bounded Contexts

### 4. User Management Context

**Characteristics:**
- **Purpose**: Handling user authentication and permissions
- **Core Entities**: User, Role, Permission
- **Business Process**: Register → Authenticate → Authorize

This context is mentioned in the requirements but not yet implemented. It would handle admin authentication and access control.

### 5. AI Categorization Context

**Characteristics:**
- **Purpose**: Analyzing and categorizing transactions
- **Core Entities**: Category, CategoryRule, CategorizationModel
- **Domain Language**: Categories, confidence scores, rule-based matching
- **Business Process**: Analyze → Match → Learn → Suggest

This context is mentioned in the requirements but not yet implemented. It would be responsible for AI-based transaction categorization.

## Context Mapping Analysis

### Current Context Relationships

1. **Transaction Management ⟷ YNAB Integration**:
   - **Type**: Customer/Supplier
   - **Integration**: Transaction Management uses YNAB Integration to submit processed transactions
   - **Translation**: Local Transaction model → YnabTransaction model

2. **Fio Bank Integration → Transaction Management**:
   - **Type**: Anticorruption Layer / Adapter
   - **Integration**: Fio adapter translates Fio-specific data to the Transaction domain model
   - **Translation**: FioTransaction → Transaction

### Planned Context Relationships

1. **Transaction Management ⟷ AI Categorization**:
   - **Type**: Partnership
   - **Integration**: Bidirectional flow where transactions are sent for categorization, and categorization results update transaction state
   - **Translation**: Transaction → Categorization Request → Categorization Result

2. **Transaction Management ⟷ User Management**:
   - **Type**: Conformist
   - **Integration**: Transaction Management accepts User Management's authentication and authorization model
   - **Translation**: User permissions determine transaction operations

## Ubiquitous Language by Context

### Transaction Management Language
- **Source Account**: A bank account from which transactions are imported
- **Transaction**: A financial transaction imported from a bank
- **Processing State**: The current state of a transaction in the processing workflow
- **Import Service**: Service responsible for importing transactions from external sources

### YNAB Integration Language
- **Budget**: A YNAB budget containing accounts, categories, and transactions
- **YNAB Account**: An account within YNAB (not the same as a Source Account)
- **Category**: A transaction classification in YNAB's budgeting system
- **Category Group**: A collection of related categories in YNAB

### Fio Bank Language
- **Fio Transaction**: Transaction data in Fio Bank's format
- **Account ID**: Fio Bank's identifier for an account
- **Bank ID**: Fio Bank's identifier for a financial institution

## Package Structure Alignment

Our ideal package structure should reflect these bounded contexts:

```
works.iterative.incubator/
  ├── transactions/     # Transaction Management Context
  │   ├── domain/       # Domain model
  │   ├── application/  # Application services
  │   ├── infrastructure/ # Repository implementations
  │   └── web/          # UI components
  │
  ├── ynab/             # YNAB Integration Context
  │   ├── domain/       # YNAB-specific domain model
  │   ├── application/  # Integration services
  │   ├── infrastructure/ # YNAB API client
  │   └── web/          # YNAB configuration UI
  │
  ├── fio/              # Fio Bank Context
  │   ├── domain/       # Fio-specific domain model
  │   ├── application/  # Fio integration services
  │   └── infrastructure/ # Fio API client
  │
  ├── categorization/   # Future AI Categorization Context
  │   ├── domain/
  │   ├── application/
  │   ├── infrastructure/
  │   └── web/
  │
  └── auth/             # Future User Management Context
      ├── domain/
      ├── application/
      ├── infrastructure/
      └── web/
```

## Implementation Recommendations

1. **Clear Bounded Context Boundaries**:
   - Each bounded context should have its own package hierarchy
   - Domain models should be kept separate between contexts
   - Cross-context communication should occur via well-defined interfaces

2. **Context Independence**:
   - Each context should be capable of independent development and testing
   - Use dependency inversion to maintain separation
   - Implement cross-cutting concerns via service interfaces

3. **Domain Model Integrity**:
   - Ensure each bounded context maintains its own consistent domain model
   - Use Anti-Corruption Layers (ACLs) for external integrations
   - Prevent concept leakage between contexts

4. **Integration Patterns**:
   - Use domain events for asynchronous cross-context communication
   - Implement dedicated translation services between contexts
   - Consider CQRS for complex query scenarios across contexts

5. **Testing Strategies**:
   - Unit test domain models within each bounded context
   - Integration test the interfaces between contexts
   - End-to-end test complete business processes

## Implementation Priority

1. Complete the separation of YNAB Integration as a distinct bounded context
2. Extract Fio Bank Integration into its own bounded context
3. Define clear interfaces between Transaction Management and these integrations
4. Implement the AI Categorization context with proper boundaries
5. Add User Management as a separate bounded context