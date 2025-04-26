package works.iterative.incubator.budget.domain.event

import java.time.Instant
import works.iterative.incubator.budget.domain.model.TransactionId

/** Event emitted when a duplicate transaction is detected.
  *
  * This event is published when the system detects an attempt to import a transaction that has
  * already been processed.
  *
  * @param externalId
  *   The external ID of the duplicate transaction
  * @param sourceAccountId
  *   The ID of the source account
  * @param existingTransactionId
  *   The ID of the existing transaction in the system
  * @param occurredAt
  *   When the duplicate was detected
  */
case class DuplicateTransactionDetected(
    externalId: String,
    sourceAccountId: Long,
    existingTransactionId: TransactionId,
    occurredAt: Instant
) extends DomainEvent
