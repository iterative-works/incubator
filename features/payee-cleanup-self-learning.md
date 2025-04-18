---
status: draft
last_updated: 2025-04-19
version: "0.1"
tags:
  - design
  - feature-enhancement
---

> [!info] Draft Design Document
> This document outlines a self-learning payee cleanup system design.

# Self-Learning Payee Cleanup System Design

## Overview

This design proposes a self-learning system for payee cleanup, where the LLM not only cleans payee names but also generates rules that can be applied to similar transactions in the future. This approach combines the flexibility of LLMs with the efficiency and consistency of rule-based systems, while automatically improving over time.

## System Components

### 1. Rule Model

```scala
case class PayeeCleanupRule(
    id: String,
    pattern: String,           // The pattern to match (e.g., raw payee text)
    patternType: String,       // Type of pattern: "exact", "contains", "regex", etc.
    replacement: String,       // The cleaned payee name
    confidence: Double,        // Confidence score (0.0-1.0)
    generatedBy: String,       // "llm" or "human"
    status: RuleStatus,        // "pending", "approved", "rejected"
    context: Map[String, String], // Optional context that helped form the rule
    createdAt: java.time.Instant,
    approvedAt: Option[java.time.Instant],
    usageCount: Int,           // How many times this rule has been applied
    successRate: Double        // Feedback success rate
)

enum RuleStatus:
    case Pending, Approved, Rejected
```

### 2. Service Interfaces

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

### 3. Implementation Workflow

#### Self-Learning Process

1. **Transaction Processing:**
   ```
   For each transaction needing payee cleanup:
     1. Check for matching approved rules
        - If found, apply the rule and record usage
        - If not found, proceed to LLM processing
     
     2. LLM Processing:
        - Send original payee and context to LLM
        - LLM returns cleaned payee name
        - LLM also suggests a rule pattern
     
     3. Rule Generation:
        - Create a pending rule from LLM suggestion
        - Store in database with "pending" status
   ```

2. **Rule Review:**
   ```
   Payee Rules Admin Interface:
     - Display pending rules sorted by frequency/confidence
     - For each rule:
       * Show original patterns and cleaned results
       * Show example transactions where it would apply
       * Allow admin to approve, modify, or reject
     - Upon approval:
       * Rule status changes to "approved"
       * Rule becomes active in the system
   ```

3. **Feedback Loop:**
   ```
   During transaction review:
     - User can flag incorrect payee cleanups
     - System identifies which rule was applied
     - Updates rule success rate
     - Rules with low success rates are flagged for review
   ```

### 4. LLM Prompt Design

The LLM will be given a dual task: clean the payee name AND suggest a rule pattern.

```
Prompt template:

You are a financial transaction analyzer. Given the raw payment information, you need to:
1. Extract the actual business or person name (the payee)
2. Create a rule that would match similar transactions in the future

Transaction details:
Payee: {original_payee}
Amount: {amount}
Description: {description}
Date: {date}

Output in JSON format:
{
  "cleaned_payee": "The cleaned business or person name",
  "rule": {
    "pattern": "The pattern to match similar transactions",
    "pattern_type": "exact|contains|startsWith|regex",
    "explanation": "Why you created this rule"
  }
}
```

## Implementation Plan

### Phase 1: Basic LLM-Based Cleanup
- Implement PayeeCleanupService with LLM integration
- Store all processed transactions and LLM responses
- No rule generation yet
- Estimated effort: 1 person-day

### Phase 2: Rule Generation
- Enhance LLM prompts to generate rule suggestions
- Implement rule storage and matching logic
- Create admin UI for rule review
- Estimated effort: 2 person-days

### Phase 3: Feedback and Optimization
- Add user feedback mechanism
- Implement rule performance tracking
- Add rule suggestion improvements based on feedback
- Estimated effort: 1 person-day

## Benefits

1. **Efficiency:** The system becomes more efficient over time as it builds up rules
2. **Cost Reduction:** Fewer LLM API calls needed as rules handle common cases
3. **Consistency:** Rules ensure consistent handling of similar transactions
4. **Continuous Improvement:** System learns from user feedback and LLM insights
5. **Transparency:** Rules can be reviewed and understood, unlike pure ML approaches

## Example Scenario

Initial transaction:
- Raw payee: "ALBERT CZ12345 PRAGUE 8"
- LLM cleans to: "Albert Supermarket"
- LLM suggests rule: Pattern "ALBERT CZ\d+ PRAGUE", type "regex", replacement "Albert Supermarket"
- Rule stored as "pending"

Admin reviews and approves the rule.

Future transaction:
- Raw payee: "ALBERT CZ98765 PRAGUE 5"
- System matches approved rule
- Cleans to "Albert Supermarket" without LLM call

## Conclusion

This self-learning approach provides the best of both worlds: the flexibility and intelligence of LLMs combined with the efficiency and transparency of rule-based systems. The system will improve over time, requiring fewer LLM calls while maintaining or improving accuracy through continuous learning and human oversight.

Over time, the system could even learn meta-patterns and suggest rule optimizations, eventually requiring minimal human intervention while maintaining high quality payee cleanup.