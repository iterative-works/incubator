package works.iterative.incubator.ynab.domain.model

import java.time.LocalDate

/** YNAB Budget
  *
  * Represents a YNAB budget that contains accounts, categories, and transactions
  *
  * Domain Model: This is a core domain entity representing a YNAB budget.
  */
case class YnabBudget(
    id: String,
    name: String,
    lastModifiedOn: Option[LocalDate] = None,
    currencyCode: Option[String] = None
)
