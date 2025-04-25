package works.iterative.incubator.budget.domain.event

import java.time.Instant
import works.iterative.incubator.budget.domain.model.ConfidenceScore

/** Event emitted when multiple transactions have been categorized.
  *
  * This event is published when a batch of transactions has been
  * categorized, typically by an automated categorization service.
  *
  * @param transactionCount The number of transactions that were categorized
  * @param sourceAccountId The ID of the account these transactions belong to
  * @param averageConfidence The average confidence score across all categorizations
  * @param occurredAt When the categorization happened
  */
case class TransactionsCategorized(
    transactionCount: Int,
    sourceAccountId: Long,
    averageConfidence: ConfidenceScore,
    occurredAt: Instant
) extends DomainEvent