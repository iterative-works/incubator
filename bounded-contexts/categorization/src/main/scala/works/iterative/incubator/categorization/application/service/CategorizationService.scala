package works.iterative.incubator.categorization.application.service

import zio.Task
import works.iterative.incubator.categorization.domain.model.*

/** Service interface for AI-based transaction categorization
  *
  * This service provides methods for categorizing transactions using various
  * approaches, including rule-based matching and machine learning algorithms.
  *
  * Classification: Application Service
  */
trait CategorizationService:
    /** Categorize a transaction by description and amount
      *
      * @param description The transaction description to analyze
      * @param amount The transaction amount
      * @return A categorization suggestion
      */
    def categorizeTransaction(
        description: String,
        amount: Double
    ): Task[CategorySuggestion]
    
    /** Add a new categorization rule
      *
      * @param rule The rule to add
      * @return The ID of the newly created rule
      */
    def addRule(rule: CategoryRule): Task[String]
    
    /** Get all categorization rules
      *
      * @return A list of all categorization rules
      */
    def getAllRules(): Task[List[CategoryRule]]
    
    /** Update an existing categorization rule
      *
      * @param rule The updated rule
      * @return Unit
      */
    def updateRule(rule: CategoryRule): Task[Unit]
    
    /** Delete a categorization rule
      *
      * @param ruleId The ID of the rule to delete
      * @return True if the rule was deleted, false if it wasn't found
      */
    def deleteRule(ruleId: String): Task[Boolean]
    
    /** Train the categorization model with new examples
      *
      * @param examples List of (description, category) pairs for training
      * @return Unit
      */
    def trainModel(examples: List[(String, String)]): Task[Unit]