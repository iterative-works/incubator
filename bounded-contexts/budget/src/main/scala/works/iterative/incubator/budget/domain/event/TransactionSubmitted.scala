package works.iterative.incubator.budget.domain.event

import java.time.Instant
import works.iterative.incubator.budget.domain.model.TransactionId

/** Event emitted when a transaction is submitted to YNAB.
  *
  * This event is published when a transaction has been successfully submitted to YNAB and received
  * a YNAB transaction ID.
  *
  * @param transactionId
  *   The unique identifier of the submitted transaction
  * @param ynabTransactionId
  *   The ID assigned by YNAB
  * @param ynabAccountId
  *   The YNAB account ID where the transaction was submitted
  * @param occurredAt
  *   When the submission happened
  */
case class TransactionSubmitted(
    transactionId: TransactionId,
    ynabTransactionId: String,
    ynabAccountId: String,
    occurredAt: Instant
) extends DomainEvent
