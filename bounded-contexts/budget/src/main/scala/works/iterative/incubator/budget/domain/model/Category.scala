package works.iterative.incubator.budget.domain.model

/** Represents a transaction category
  *
  * Categories are used to classify transactions for budgeting and reporting purposes.
  * A category can have a parent category, creating a hierarchy.
  *
  * @param id
  *   Unique identifier for the category
  * @param name
  *   Display name of the category
  * @param parentId
  *   Optional parent category ID if this is a subcategory
  * @param active
  *   Whether this category is active and available for selection
  *
  * Classification: Domain Entity
  */
case class Category(
    id: String,
    name: String,
    parentId: Option[String],
    active: Boolean = true
)