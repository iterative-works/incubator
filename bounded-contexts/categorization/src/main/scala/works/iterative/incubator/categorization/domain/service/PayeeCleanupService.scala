package works.iterative.incubator.categorization.domain.service

import works.iterative.incubator.categorization.domain.model.*
import zio.*

/** Service for cleaning up payee names in transactions using rule-based and LLM approaches.
  *
  * The PayeeCleanupService is responsible for:
  * - Cleaning up raw payee names using rules or LLM
  * - Managing the lifecycle of cleanup rules
  * - Tracking rule performance
  * - Handling feedback to improve cleaning over time
  *
  * Classification: Domain Service
  */
trait PayeeCleanupService:
    /** Clean up a payee name using existing rules or LLM generation.
      *
      * This is the main method for payee cleanup. It first attempts to match the original
      * payee name against approved rules. If no rule matches, it falls back to LLM for
      * generating a cleaned name and possibly suggesting new rules.
      *
      * @param original The original payee name from the transaction
      * @param context Additional transaction context to aid in cleanup (e.g., counterAccount, message)
      * @return A task resulting in a PayeeCleanupResult with cleaned name and any rules applied/generated
      */
    def cleanupPayee(
        original: String, 
        context: Map[String, String]
    ): Task[PayeeCleanupResult]
    
    /** Get all rules with pending status that need approval.
      *
      * @return A task resulting in a sequence of pending rules
      */
    def getPendingRules(): Task[Seq[PayeeCleanupRule]]
    
    /** Get all approved rules.
      *
      * @return A task resulting in a sequence of approved rules
      */
    def getApprovedRules(): Task[Seq[PayeeCleanupRule]]
    
    /** Approve a pending rule, optionally with modifications.
      *
      * @param ruleId The ID of the rule to approve
      * @param modifications Optional modifications to the rule before approving
      * @return A task resulting in the approved rule
      */
    def approveRule(
        ruleId: String, 
        modifications: Option[Map[String, String]] = None
    ): Task[PayeeCleanupRule]
    
    /** Reject a pending rule.
      *
      * @param ruleId The ID of the rule to reject
      * @param reason Optional reason for rejection (for auditing)
      * @return A task resulting in Unit
      */
    def rejectRule(
        ruleId: String, 
        reason: Option[String] = None
    ): Task[Unit]
    
    /** Provide feedback on rule application.
      *
      * This method is used to track rule performance over time. When a user approves
      * or corrects a cleaned payee name, this feedback is used to update the rule's
      * success metrics.
      *
      * @param ruleId The ID of the rule that was applied
      * @param wasSuccessful Whether the rule application was successful
      * @return A task resulting in Unit
      */
    def provideFeedback(
        ruleId: String, 
        wasSuccessful: Boolean
    ): Task[Unit]
    
    /** Find rules that match a given payee name.
      *
      * @param payeeName The payee name to match against rules
      * @return A task resulting in a sequence of matching rules
      */
    def findMatchingRules(
        payeeName: String
    ): Task[Seq[PayeeCleanupRule]]
    
    /** Create a new rule manually (by a human).
      *
      * @param pattern The pattern to match
      * @param patternType The type of pattern (exact, contains, starts with, regex)
      * @param replacement The replacement text to use
      * @return A task resulting in the newly created rule (auto-approved)
      */
    def createRule(
        pattern: String,
        patternType: PatternType,
        replacement: String
    ): Task[PayeeCleanupRule]
end PayeeCleanupService

/** Result of a payee cleanup operation */
case class PayeeCleanupResult(
    original: String,
    cleaned: String,
    confidence: Double,
    appliedRule: Option[PayeeCleanupRule],
    generatedRule: Option[PayeeCleanupRule]
)