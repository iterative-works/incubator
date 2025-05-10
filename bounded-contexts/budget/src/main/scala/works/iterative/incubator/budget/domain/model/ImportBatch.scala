package works.iterative.incubator.budget.domain.model

import java.time.{Instant, LocalDate}

/** Represents a batch of transactions imported from a bank.
  *
  * @param id
  *   Unique identifier for the import batch
  * @param accountId
  *   ID of the source account that transactions were imported from
  * @param startDate
  *   Start date of the date range for the import
  * @param endDate
  *   End date of the date range for the import
  * @param status
  *   Current status of the import process
  * @param transactionCount
  *   Number of transactions imported in this batch
  * @param errorMessage
  *   Optional error message if the import failed
  * @param startTime
  *   When the import operation started
  * @param endTime
  *   Optional time when the import operation completed
  * @param createdAt
  *   When this import batch record was created
  * @param updatedAt
  *   When this import batch record was last updated
  *
  * Category: Entity Layer: Domain
  */
case class ImportBatch(
    id: ImportBatchId,
    accountId: AccountId,
    startDate: LocalDate,
    endDate: LocalDate,
    status: ImportStatus,
    transactionCount: Int,
    errorMessage: Option[String],
    startTime: Instant,
    endTime: Option[Instant],
    createdAt: Instant,
    updatedAt: Instant
):
    /** Calculate the time taken to complete the import operation in seconds.
      *
      * @return
      *   Option containing the completion time in seconds, or None if the operation hasn't
      *   completed
      */
    def completionTimeSeconds: Option[Long] =
        endTime.map(end => java.time.temporal.ChronoUnit.SECONDS.between(startTime, end))

    /** Determines if the import operation was successful.
      *
      * @return
      *   true if no error message is present, false otherwise
      */
    def isSuccess: Boolean = errorMessage.isEmpty

    /** Updates the import batch status to in progress.
      *
      * @return
      *   A new ImportBatch with updated status and timestamp
      */
    def markInProgress(): Either[String, ImportBatch] =
        if status != ImportStatus.NotStarted then
            Left(s"Cannot start import with status $status")
        else
            Right(
                this.copy(
                    status = ImportStatus.InProgress,
                    updatedAt = Instant.now
                )
            )

    /** Marks the import as completed successfully.
      *
      * @param transactionCount
      *   Number of transactions imported
      * @return
      *   A new ImportBatch with updated status, count, and timestamps
      */
    def markCompleted(transactionCount: Int): Either[String, ImportBatch] =
        if status != ImportStatus.InProgress then
            Left(s"Cannot complete import with status $status")
        else
            val now = Instant.now
            Right(
                this.copy(
                    status = ImportStatus.Completed,
                    transactionCount = transactionCount,
                    endTime = Some(now),
                    updatedAt = now
                )
            )

    /** Marks the import as failed with an error message.
      *
      * @param errorMessage
      *   Description of what went wrong
      * @return
      *   A new ImportBatch with updated status, error message, and timestamps
      */
    def markFailed(errorMessage: String): Either[String, ImportBatch] =
        val now = Instant.now
        Right(
            this.copy(
                status = ImportStatus.Error,
                errorMessage = Some(errorMessage),
                endTime = Some(now),
                updatedAt = now
            )
        )
    end markFailed
end ImportBatch

object ImportBatch:
    /** Creates a new import batch with basic validation. Note: Date range validation should be done
      * with BankTransactionService.validateDateRange() before calling this method, as different
      * banks have different date range constraints.
      *
      * @param accountId
      *   ID of the source account
      * @param startDate
      *   Start date of the import range
      * @param endDate
      *   End date of the import range
      * @return
      *   Either a valid ImportBatch or an error message
      */
    def create(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): Either[String, ImportBatch] =
        // Basic input validation
        if accountId == null then
            Left("Account ID must not be null")
        else if startDate == null then
            Left("Start date must not be null")
        else if endDate == null then
            Left("End date must not be null")
        else if startDate.isAfter(endDate) then
            Left("Start date cannot be after end date")
        else if startDate.isAfter(LocalDate.now) || endDate.isAfter(LocalDate.now) then
            Left("Dates cannot be in the future")
        else
            val now = Instant.now
            val id = ImportBatchId.generate()

            // Create the import batch record with validated data
            Right(
                ImportBatch(
                    id = id,
                    accountId = accountId,
                    startDate = startDate,
                    endDate = endDate,
                    status = ImportStatus.NotStarted,
                    transactionCount = 0,
                    errorMessage = None,
                    startTime = now,
                    endTime = None,
                    createdAt = now,
                    updatedAt = now
                )
            )
end ImportBatch
