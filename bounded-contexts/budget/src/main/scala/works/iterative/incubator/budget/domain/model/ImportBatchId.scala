package works.iterative.incubator.budget.domain.model

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** Value object that uniquely identifies an import batch in the system.
  * Uses a sequential identifier (accountId-sequence) for better human readability
  * and chronological ordering.
  * 
  * Category: Value Object
  * Layer: Domain
  */
case class ImportBatchId(accountId: String, sequenceNumber: Long):
    require(accountId != null, "Account ID must not be null")
    require(accountId.nonEmpty, "Account ID must not be empty")
    require(sequenceNumber > 0, "Sequence number must be positive")

    /** The combined unique identifier
      * @return a string representation of the composite ID
      */
    def value: String = s"$accountId-$sequenceNumber"

    /** String representation of this identifier
      * @return the same as value
      */
    override def toString: String = value

object ImportBatchId:
    // For testing/backward compatibility: in-memory sequence counter
    private val sequenceCounter = new AtomicLong(1)
    
    /** Generates a new ImportBatchId with the next sequence number.
      * This method is primarily for testing purposes.
      * In production, the sequence should be managed by the repository.
      *
      * @param accountId The account ID associated with this import batch
      * @return A new import batch ID with the next sequence number
      */
    def generate(accountId: String = UUID.randomUUID().toString.take(8)): ImportBatchId = 
        ImportBatchId(accountId, sequenceCounter.getAndIncrement())

    /** Legacy method for backward compatibility.
      * Generates a new import batch ID with a random account ID.
      *
      * @return A new import batch ID with a random account ID
      */
    def generate(): ImportBatchId = generate(UUID.randomUUID().toString.take(8))

    /** Creates an ImportBatchId from a composite string in the format "accountId-sequenceNumber".
      *
      * @param s
      *   The string representation of the composite ID
      * @return
      *   Either an ImportBatchId or an error message
      */
    def fromString(s: String): Either[String, ImportBatchId] =
        if s == null || s.isEmpty then
            Left("Import batch ID string must not be null or empty")
        else
            val parts = s.split("-", 2)
            if parts.length != 2 then
                Left(s"Invalid composite ID format: $s. Expected format: 'accountId-sequenceNumber'")
            else if parts(0).isEmpty then
                Left("Account ID part must not be empty")
            else
                try
                    val seqNum = parts(1).toLong
                    if seqNum <= 0 then
                        Left("Sequence number must be positive")
                    else
                        Right(ImportBatchId(parts(0), seqNum))
                catch
                    case _: NumberFormatException => 
                        Left(s"Invalid sequence number format: ${parts(1)}. Expected a positive number.")

    /** Creates an ImportBatchId for the specified account with a specific sequence number.
      *
      * @param accountId The ID of the account
      * @param sequenceNumber The sequence number for this import batch
      * @return A new ImportBatchId
      */
    def create(accountId: String, sequenceNumber: Long): Either[String, ImportBatchId] =
        try
            Right(ImportBatchId(accountId, sequenceNumber))
        catch
            case e: IllegalArgumentException => Left(e.getMessage)
end ImportBatchId
