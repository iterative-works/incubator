package works.iterative.incubator.budget.domain.model

/** Unique identifier for a transaction
  *
  * This identifier is composed of a source account ID and the bank's transaction ID. Together, they
  * uniquely identify a transaction event.
  *
  * @param sourceAccountId
  *   The ID of the source account this transaction belongs to
  * @param transactionId
  *   The bank-assigned transaction identifier
  *
  * Classification: Domain Value Object
  */
case class TransactionId(sourceAccountId: Long, transactionId: String)
