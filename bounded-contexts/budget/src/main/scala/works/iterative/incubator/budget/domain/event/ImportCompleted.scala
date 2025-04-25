package works.iterative.incubator.budget.domain.event

import java.time.Instant

/** Event emitted when a batch of transactions has been imported.
  * 
  * This is a summary event that indicates the completion of an import process.
  * It contains aggregate information about the imported transactions.
  * 
  * @param sourceAccountId The ID of the source account from which transactions were imported
  * @param count The number of transactions imported
  * @param occurredAt When the import completed
  */
case class ImportCompleted(
    sourceAccountId: Long,
    count: Int,
    occurredAt: Instant
) extends DomainEvent