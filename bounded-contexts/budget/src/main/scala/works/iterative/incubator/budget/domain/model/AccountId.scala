package works.iterative.incubator.budget.domain.model

import java.util.UUID

/** Value object that uniquely identifies a financial account in the system.
  * Uses a composite natural identifier that combines the bank identifier
  * and the bank account number for better traceability and integration.
  */
case class AccountId(bankId: String, bankAccountId: String):
    require(bankId != null, "Bank ID must not be null")
    require(bankAccountId != null, "Bank account ID must not be null")
    require(bankId.nonEmpty, "Bank ID must not be empty")
    require(bankAccountId.nonEmpty, "Bank account ID must not be empty")

    /** The combined unique identifier
      * @return a string representation of the composite ID
      */
    def value: String = s"$bankId-$bankAccountId"

    /** String representation of this identifier
      * @return the same as value
      */
    override def toString: String = value

object AccountId:
    /** Generates a new random AccountId.
      * This method is provided for backward compatibility and testing.
      * In production, use the primary constructor with actual bank ID and bank account ID.
      *
      * @return
      *   A new account ID with a random values
      */
    def generate(): AccountId = AccountId(
        bankId = UUID.randomUUID().toString.take(8),
        bankAccountId = UUID.randomUUID().toString
    )

    /** Creates an AccountId from a composite string in the format "bankId-bankAccountId".
      *
      * @param s
      *   The string representation of the composite ID
      * @return
      *   Either an AccountId or an error message
      */
    def fromString(s: String): Either[String, AccountId] =
        if s == null || s.isEmpty then
            Left("Account ID string must not be null or empty")
        else
            val parts = s.split("-", 2)
            if parts.length != 2 then
                Left(s"Invalid composite ID format: $s. Expected format: 'bankId-bankAccountId'")
            else if parts(0).isEmpty then
                Left("Bank ID part must not be empty")
            else if parts(1).isEmpty then
                Left("Bank account ID part must not be empty")
            else
                Right(AccountId(parts(0), parts(1)))

    /** Creates an AccountId for the specified bank and bank account.
      *
      * @param bankId The ID of the bank
      * @param bankAccountId The bank's account ID or number
      * @return A new AccountId
      */
    def create(bankId: String, bankAccountId: String): Either[String, AccountId] =
        try
            Right(AccountId(bankId, bankAccountId))
        catch
            case e: IllegalArgumentException => Left(e.getMessage)
end AccountId
