package works.iterative.incubator.budget.domain.event

import java.time.{Instant, LocalDate}
import works.iterative.incubator.budget.domain.model.TransactionId

/** Event emitted when a new transaction is imported from a bank.
  *
  * This event is published whenever a transaction is successfully imported
  * from a bank into the system.
  *
  * @param transactionId The unique identifier of the imported transaction
  * @param sourceAccountId The source account from which the transaction was imported
  * @param date The date of the transaction
  * @param amount The amount of the transaction
  * @param currency The currency of the transaction
  * @param occurredAt When the import happened
  */
case class TransactionImported(
    transactionId: TransactionId,
    sourceAccountId: Long,
    date: LocalDate,
    amount: BigDecimal,
    currency: String,
    occurredAt: Instant
) extends DomainEvent