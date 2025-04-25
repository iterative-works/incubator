package works.iterative.incubator.budget.domain.event

import java.time.Instant

/** Event emitted when multiple transactions are submitted to YNAB.
  *
  * This event is published when a batch of transactions has been
  * successfully submitted to YNAB.
  *
  * @param count The number of transactions that were submitted
  * @param ynabAccountId The YNAB account ID to which the transactions were submitted
  * @param occurredAt When the submission happened
  */
case class TransactionsSubmitted(
    count: Int,
    ynabAccountId: String,
    occurredAt: Instant
) extends DomainEvent