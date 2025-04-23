package works.iterative.incubator.budget.domain.event

import java.time.Instant
import works.iterative.incubator.budget.domain.model.TransactionId

/** Event emitted when a transaction is categorized.
  *
  * This event is published when a transaction has been categorized,
  * either by AI or manually by a user.
  *
  * @param transactionId The unique identifier of the categorized transaction
  * @param category The assigned category 
  * @param payeeName The assigned payee name
  * @param byAI Whether the categorization was done by AI
  * @param occurredAt When the categorization happened
  */
case class TransactionCategorized(
    transactionId: TransactionId,
    category: String,
    payeeName: String,
    byAI: Boolean,
    occurredAt: Instant
) extends DomainEvent