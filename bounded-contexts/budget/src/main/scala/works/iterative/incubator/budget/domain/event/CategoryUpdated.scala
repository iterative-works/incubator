package works.iterative.incubator.budget.domain.event

import java.time.Instant
import works.iterative.incubator.budget.domain.model.TransactionId

/** Event emitted when a transaction's category is manually updated.
  *
  * This event is published when a user manually updates the category
  * of a specific transaction.
  *
  * @param transactionId The ID of the transaction that was updated
  * @param oldCategory The previous category (if any)
  * @param newCategory The new category assigned by the user
  * @param occurredAt When the update happened
  */
case class CategoryUpdated(
    transactionId: TransactionId,
    oldCategory: Option[String],
    newCategory: String,
    occurredAt: Instant
) extends DomainEvent