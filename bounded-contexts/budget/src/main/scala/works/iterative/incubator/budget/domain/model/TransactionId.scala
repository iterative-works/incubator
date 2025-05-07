package works.iterative.incubator.budget.domain.model

import java.util.UUID

/** Value object that uniquely identifies a transaction in the system.
  * Uses a composite natural identifier that combines the source account identifier
  * and the bank's transaction identifier for better traceability and deduplication.
  *
  * Category: Value Object
  * Layer: Domain
  */
case class TransactionId(sourceAccountId: String, bankTransactionId: String):
    require(sourceAccountId != null, "Source account ID must not be null")
    require(bankTransactionId != null, "Bank transaction ID must not be null")
    require(sourceAccountId.nonEmpty, "Source account ID must not be empty")
    require(bankTransactionId.nonEmpty, "Bank transaction ID must not be empty")

    /** The combined unique identifier
      * @return a string representation of the composite ID
      */
    def value: String = s"$sourceAccountId-$bankTransactionId"

    /** String representation of this identifier
      * @return the same as value
      */
    override def toString: String = value

object TransactionId:
    /** Generates a new random TransactionId.
      * This method is provided for backward compatibility and testing.
      * In production, use the primary constructor with actual source account ID and bank transaction ID.
      *
      * @return
      *   A new transaction ID with a random UUID as the bank transaction ID
      */
    def generate(): TransactionId = TransactionId(
        sourceAccountId = UUID.randomUUID().toString,
        bankTransactionId = UUID.randomUUID().toString
    )

    /** Creates a TransactionId from a composite string in the format "sourceAccountId-bankTransactionId".
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
            val parts = s.split("-", 2)
            if parts.length != 2 then
                Left(s"Invalid composite ID format: $s. Expected format: 'sourceAccountId-bankTransactionId'")
            else if parts(0).isEmpty then
                Left("Source account ID part must not be empty")
            else if parts(1).isEmpty then
                Left("Bank transaction ID part must not be empty")
            else
                Right(TransactionId(parts(0), parts(1)))

    /** Creates a TransactionId for the specified source account and bank transaction.
      *
      * @param sourceAccountId The ID of the source account
      * @param bankTransactionId The bank's original transaction ID
      * @return A new TransactionId
      */
    def create(sourceAccountId: String, bankTransactionId: String): Either[String, TransactionId] =
        try
            Right(TransactionId(sourceAccountId, bankTransactionId))
        catch
            case e: IllegalArgumentException => Left(e.getMessage)
end TransactionId
