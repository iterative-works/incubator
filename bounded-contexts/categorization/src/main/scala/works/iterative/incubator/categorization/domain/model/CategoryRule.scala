package works.iterative.incubator.categorization.domain.model

/** Represents a rule for automatically categorizing transactions
  *
  * This entity defines a rule that can be used to automatically categorize transactions based on
  * certain patterns or criteria. Rules have a priority and can be enabled or disabled.
  *
  * Classification: Domain Entity
  */
case class CategoryRule(
    id: String, // Unique identifier for the rule
    name: String, // User-friendly name for the rule
    description: Option[String], // Optional description of the rule's purpose
    pattern: String, // Pattern to match (supports regex or specific syntax)
    patternType: String, // Type of pattern (e.g., "regex", "simple", "fuzzy")
    fieldToMatch: String, // Which transaction field to match against (e.g., "description", "payee")
    category: String, // Category to assign if the rule matches
    subCategory: Option[String], // Optional sub-category to assign
    priority: Int, // Priority of this rule (higher numbers take precedence)
    enabled: Boolean, // Whether this rule is currently active
    createdAt: java.time.Instant, // When this rule was created
    updatedAt: java.time.Instant // When this rule was last updated
)
