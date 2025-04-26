package works.iterative.incubator.budget.domain.event

import java.time.Instant

/** Event emitted when a submission to YNAB fails.
  *
  * This event is published when the system encounters an error while attempting to submit
  * transactions to YNAB.
  *
  * @param reason
  *   The reason for the submission failure
  * @param transactionCount
  *   The number of transactions that were being submitted
  * @param occurredAt
  *   When the failure occurred
  */
case class SubmissionFailed(
    reason: String,
    transactionCount: Int,
    occurredAt: Instant
) extends DomainEvent
