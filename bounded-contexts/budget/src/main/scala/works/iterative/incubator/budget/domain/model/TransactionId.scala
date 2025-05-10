package works.iterative.incubator.budget.domain.model

import java.util.UUID

/** Value object that uniquely identifies a transaction in the system. Uses a composite natural
  * identifier that combines the source account identifier and the bank's transaction identifier for
  * better traceability and deduplication.
  *
  * Category: Value Object Layer: Domain
  */
case class TransactionId(sourceAccount: AccountId, bankTransactionId: String):
    require(sourceAccount != null, "Source account must not be null")
    require(bankTransactionId != null, "Bank transaction ID must not be null")
    require(bankTransactionId.nonEmpty, "Bank transaction ID must not be empty")

    /** The combined unique identifier
      * @return
      *   a string representation of the composite ID
      */
    def value: String = s"${sourceAccount.toString}-$bankTransactionId"

    /** String representation of this identifier
      * @return
      *   the same as value
      */
    override def toString: String = value
end TransactionId

object TransactionId:
    /** Creates a TransactionId from a composite string in the format
      * "bankId-bankAccountId-bankTransactionId".
      *
      * @param s
      *   The string representation of the composite ID
      * @return
      *   Either a TransactionId or an error message
      */
    def fromString(s: String): Either[String, TransactionId] =
        if s == null || s.isEmpty then
            Left("Transaction ID string must not be null or empty")
        else
            val parts = s.split("-", 3)
            if parts.length < 3 then
                Left(
                    s"Invalid composite ID format: $s. Expected format: 'bankId-bankAccountId-bankTransactionId'"
                )
            else if parts(0).isEmpty then
                Left("Bank ID or source account ID part must not be empty")
            else if parts(parts.length - 1).isEmpty then
                Left("Bank transaction ID part must not be empty")
            else
                // New format: bankId-bankAccountId-bankTransactionId
                AccountId.create(parts(0), parts(1)) match
                    case Right(accountId) => Right(TransactionId(accountId, parts(2)))
                    case Left(error)      => Left(error)
            end if

    /** Creates a TransactionId for the specified source account and bank transaction.
      *
      * @param sourceAccount
      *   The source account ID
      * @param bankTransactionId
      *   The bank's original transaction ID
      * @return
      *   A new TransactionId
      */
    def create(sourceAccount: AccountId, bankTransactionId: String): Either[String, TransactionId] =
        try
            Right(TransactionId(sourceAccount, bankTransactionId))
        catch
            case e: IllegalArgumentException => Left(e.getMessage)

    /** Generates a random TransactionId for testing purposes.
      *
      * @return A new random TransactionId
      */
    def generate(): TransactionId =
        val randomBankId = java.util.UUID.randomUUID().toString.take(8)
        val randomAccountId = java.util.UUID.randomUUID().toString.take(12)
        val randomTxId = java.util.UUID.randomUUID().toString
        TransactionId(AccountId(randomBankId, randomAccountId), randomTxId)
end TransactionId
