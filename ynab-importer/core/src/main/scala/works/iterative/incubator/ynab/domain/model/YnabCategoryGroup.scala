package works.iterative.incubator.ynab.domain.model

/**
 * YNAB Category Group
 *
 * Represents a group of categories in YNAB
 *
 * Domain Model: This is a core domain entity representing a YNAB category group.
 */
case class YnabCategoryGroup(
    id: String,
    name: String,
    hidden: Boolean = false,
    deleted: Boolean = false
)