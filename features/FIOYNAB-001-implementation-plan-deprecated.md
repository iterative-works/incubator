---
status: deprecated
last_updated: 2025-04-23
version: "0.1"
tags:
  - workflow
  - feature-implementation
  - deprecated
---

> [!warning] Deprecated Document
> This document has been deprecated. Please refer to [FIOYNAB-001-implementation-plan.md](./FIOYNAB-001-implementation-plan.md) for the current implementation plan, which follows our BDD-driven UI-first approach.

> [!info] Draft Document
> This document is an initial draft and may change significantly.

# Feature Implementation Plan: FIOYNAB-001

## Feature Reference
- **Related Change Request**: [CR-2025001](../change-requests/CR-2025001.md)
- **Feature Specification**: [FIOYNAB-001](./FIOYNAB-001.md)
- **Business Value Decomposition**: [BVD-FIOYNAB-001](./BVD-FIOYNAB-001.md)

## Current State Analysis

### Implemented Components
After analyzing the codebase, the following components have already been implemented:

1. **Fio Bank API Integration**
   - FioClient for API communication
   - FioAccount and FioTransaction models
   - FioImportService for retrieving transactions
   - PostgreSQL repositories for Fio accounts and import state

2. **PostgreSQL Database**
   - Schema for transactions and related entities
   - Transaction repositories
   - Processing state tracking

3. **Basic Transaction Management UI**
   - TransactionImportModule for viewing transactions
   - UI for displaying transaction list
   - Basic import functionality (import-yesterday)

4. **YNAB API Integration**
   - YnabClient for API communication
   - YnabService for account and budget management
   - YnabTransactionImportService for submitting transactions
   - Account mapping functionality

5. **Authentication System**
   - Basic authentication framework

### Partially Implemented Components
1. **Payee Cleanup Processing**
   - Basic structure exists in TransactionProcessor
   - Currently only does minimal cleaning (using counterBankName or message as payee)
   - No advanced processing or pattern matching

2. **Duplicate Prevention**
   - Basic structure exists in processing state
   - Logic for tracking submitted transactions

3. **Transaction Categorization**
   - Partial implementation in Categorization bounded context
   - CategoryRule and CategorySuggestion models
   - CategorizationService interface defined

### Missing Components
1. **Self-Learning Payee Cleanup Processing**
   - No AI-based or rules-based payee name enhancement
   - No payee pattern matching or standardization
   - No self-learning system to improve over time

2. **AI Categorization**
   - Integration with external AI API (OpenAI)
   - Implementation of categorization logic

3. **Category Editing UI**
   - UI for efficient category editing
   - Bulk actions for categories

4. **Advanced Filtering & Sorting**
   - Limited filtering capabilities

## Implementation Plan

### MVS Implementation (Priority 1)
Focus on completing the minimum viable solution components:

1. **Self-Learning Payee Cleanup Processing**
   - **Tasks:**
     1. Implement PayeeCleanupService interface in categorization bounded context
     2. Create LLM integration for payee name cleanup
     3. Implement rule suggestion generation from LLM outputs
     4. Add rule storage and management (PostgreSQL repository)
     5. Develop rule review UI for administrators
     6. Integrate with TransactionProcessor
     7. Update transaction UI to display cleaned payee names
   - **Estimated Effort:** 3 person-days
   - **Technical Dependencies:** Existing Transaction model, processing state, OpenAI integration

2. **Connect Existing Components**
   - **Tasks:**
     1. Connect FioImportService to TransactionImportService
     2. Complete TransactionManagerService integration with YNAB
     3. Update UI to show import/export progress
     4. Implement submission to YNAB functionality
   - **Estimated Effort:** 1 person-day
   - **Technical Dependencies:** Existing FIO and YNAB integrations

3. **Complete Duplicate Prevention Logic**
   - **Tasks:**
     1. Enhance duplicate detection in TransactionProcessor
     2. Add fingerprinting for transactions
     3. Implement duplicate warning in UI
   - **Estimated Effort:** 1 person-day
   - **Technical Dependencies:** Transaction model and repository

### Value Increment 1 (Priority 2)

1. **AI Categorization Integration**
   - **Tasks:**
     1. Implement CategorizationServiceImpl
     2. Create OpenAI client for category suggestions
     3. Implement AI prompt engineering for transaction categorization
     4. Store and track categorization confidence
   - **Estimated Effort:** 3 person-days
   - **Technical Dependencies:** OpenAI API, Categorization bounded context

2. **Category Editing UI Enhancements**
   - **Tasks:**
     1. Enhance transaction UI for better category editing
     2. Add category history and favorites
     3. Implement manual override tracking
   - **Estimated Effort:** 2 person-days
   - **Technical Dependencies:** Transaction UI module

### Value Increment 2 (Priority 3)

1. **Enhanced Authentication**
   - **Tasks:**
     1. Integrate with Auth bounded context
     2. Add role-based permissions
   - **Estimated Effort:** 1 person-day
   - **Technical Dependencies:** Auth bounded context

2. **Advanced Filtering & Sorting**
   - **Tasks:**
     1. Enhance TransactionQuery with additional filtering options
     2. Add UI components for filtering and sorting
     3. Implement server-side filtering
   - **Estimated Effort:** 1 person-day
   - **Technical Dependencies:** Transaction Query model

3. **Bulk Actions**
   - **Tasks:**
     1. Implement multi-select functionality
     2. Add bulk categorize, bulk submit operations
     3. Add bulk payee cleanup
   - **Estimated Effort:** 2 person-days
   - **Technical Dependencies:** Transaction UI and processing services

## Technical Implementation Details

### Domain Model Updates

1. **PayeeCleanupRule Model**
   ```scala
   case class PayeeCleanupRule(
       id: String,
       pattern: String,
       patternType: String, // regex, contains, startsWith
       replacement: String,
       confidence: Double,
       generatedBy: String, // "llm" or "human"
       status: RuleStatus, // pending, approved, rejected
       usageCount: Int,
       successRate: Double,
       createdAt: java.time.Instant,
       updatedAt: Option[java.time.Instant]
   )
   
   enum RuleStatus:
       case Pending, Approved, Rejected
   ```

2. **Extended CategorySuggestion**
   ```scala
   // Add to existing CategorySuggestion:
   cleanedPayee: Option[String],
   payeeConfidence: Double
   ```

### Component Design

1. **PayeeCleanupService Interface**
   ```scala
   trait PayeeCleanupService:
       // Main cleanup method
       def cleanupPayee(
           original: String, 
           context: Map[String, String]
       ): Task[(String, Option[PayeeCleanupRule])]
       
       // Rule management
       def getPendingRules(): Task[Seq[PayeeCleanupRule]]
       def getApprovedRules(): Task[Seq[PayeeCleanupRule]]
       def approveRule(ruleId: String, modifications: Option[Map[String, String]] = None): Task[PayeeCleanupRule]
       def rejectRule(ruleId: String, reason: Option[String] = None): Task[Unit]
       
       // Feedback mechanism
       def provideFeedback(ruleId: String, wasSuccessful: Boolean): Task[Unit]
   ```

2. **Enhanced TransactionProcessor**
   ```scala
   // Update processImportedTransactions to:
   // 1. Extract transaction context (counterBank, counterAccount, message)
   // 2. Call PayeeCleanupService for payee name cleanup
   // 3. Call CategorizationService for category suggestion
   // 4. Update processing state with cleaned data
   // 5. If a rule was generated, store it for review
   ```

### Integration Points

1. **AI Integration (OpenAI)**
   - Create client for OpenAI API
   - Design prompt templates for:
     - Payee cleanup with rule generation
     - Transaction categorization
   - Implement caching for similar transactions

2. **UI/Backend Integration**
   - Add API endpoints for payee rule management
   - Develop rule review and approval interface
   - Update transaction UI to show:
     - Cleaned payee names with confidence scores
     - Rule application status indicators
   - Add detailed transaction view with edit history
   - Add feedback mechanism for incorrect cleanups

## Testing Strategy

1. **Unit Tests**
   - PayeeCleanupService implementation
   - Rule generation and matching logic
   - Rule approval workflow
   - Categorization logic

2. **Integration Tests**
   - End-to-end flow from Fio import to YNAB submission
   - Rule generation and application pipeline
   - Database storage and retrieval
   - Duplicate detection

3. **UI Tests**
   - Rule review interface
   - Payee cleanup feedback mechanism
   - Category editing
   - Bulk actions
   - Filter functionality

## Implementation Sequence

### Week 1
1. Self-Learning Payee Cleanup Processing (Phase 1: Basic LLM Integration)
2. Connect Existing Components
3. Complete Duplicate Prevention Logic

### Week 2
1. Self-Learning Payee Cleanup Processing (Phase 2: Rule Generation)
2. AI Categorization Integration 
3. Category Editing UI Enhancements

### Week 3
1. Self-Learning Payee Cleanup Processing (Phase 3: Feedback Mechanism)
2. Enhanced Authentication
3. Advanced Filtering & Sorting
4. Bulk Actions

## Risks and Mitigations

1. **Risk: OpenAI API integration complexity**
   - **Mitigation:** Start with simplified prompts, implement progressive enhancement

2. **Risk: Performance issues with large transaction volumes**
   - **Mitigation:** Implement pagination, background processing for categorization

3. **Risk: User adoption due to UI complexity**
   - **Mitigation:** Focus on intuitive design, provide defaults, add tooltips

4. **Risk: Low-quality rule suggestions from LLM**
   - **Mitigation:** Implement thorough review process, track rule performance, auto-disable underperforming rules

5. **Risk: Growing rule database complexity over time**
   - **Mitigation:** Implement rule consolidation, periodic cleanup of unused/redundant rules

## Conclusion

This implementation plan outlines the work needed to complete the Fio Bank to YNAB integration project, building on the existing foundation. The focus is on delivering the MVS components first, particularly the Self-Learning Payee Cleanup Processing which is identified as a critical component in the Business Value Decomposition.

By implementing the self-learning payee cleanup system, we address the key pain point of noisy payee information in a way that continually improves over time. The system will start by using AI (LLM) for all payee cleanups, but will gradually build a set of rules through AI suggestions, reducing costs and improving performance while maintaining high quality.

The phased implementation approach ensures we deliver value quickly with the MVS, then enhance the solution with AI categorization and improved user experience features in subsequent iterations.

## References
- [Self-Learning Payee Cleanup System Design](./payee-cleanup-self-learning.md)

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2025-04-19 | Initial draft | AI |
| 0.2 | 2025-04-19 | Incorporated self-learning payee cleanup system | AI |