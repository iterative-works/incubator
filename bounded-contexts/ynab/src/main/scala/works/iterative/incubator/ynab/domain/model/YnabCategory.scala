package works.iterative.incubator.ynab.domain.model

/** YNAB Category
  *
  * Represents a category that transactions can be assigned to
  *
  * Domain Model: This is a core domain entity representing a YNAB category.
  */
case class YnabCategory(
    id: String,
    name: String,
    groupId: String,
    groupName: String,
    hidden: Boolean = false,
    deleted: Boolean = false,
    budgeted: Option[BigDecimal] = None,
    activity: Option[BigDecimal] = None,
    balance: Option[BigDecimal] = None
)
