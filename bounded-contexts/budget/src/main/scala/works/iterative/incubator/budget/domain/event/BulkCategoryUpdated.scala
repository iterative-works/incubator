package works.iterative.incubator.budget.domain.event

import java.time.Instant

/** Event emitted when multiple transactions are updated with the same category.
  *
  * This event is published when a user performs a bulk update to assign
  * the same category to multiple transactions.
  *
  * @param count The number of transactions that were updated
  * @param category The category assigned to the transactions
  * @param filterCriteria Description of the criteria used to select the transactions
  * @param occurredAt When the bulk update happened
  */
case class BulkCategoryUpdated(
    count: Int,
    category: String,
    filterCriteria: String,
    occurredAt: Instant
) extends DomainEvent