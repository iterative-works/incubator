package works.iterative.incubator.categorization.domain.model

/** Represents a category suggestion generated by AI for a transaction
  *
  * This entity represents a suggestion made by an AI system for categorizing
  * a financial transaction. It contains the suggested category, confidence level,
  * and metadata about how the suggestion was generated.
  *
  * Classification: Domain Entity
  */
case class CategorySuggestion(
    transactionId: String,           // The transaction this suggestion is for
    category: String,                // The suggested category
    subCategory: Option[String],     // Optional sub-category
    confidence: Double,              // Confidence level (0.0 to 1.0)
    method: String,                  // Method used to generate the suggestion (e.g., "rules", "ml", "hybrid")
    explanation: Option[String],     // Optional explanation of why this category was suggested
    sourceRule: Option[String]       // If method is "rules", the rule ID that matched
)