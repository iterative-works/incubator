package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.{Instant, LocalDate}
import zio.*
import scala.util.Random

/** Mock implementation of TransactionImportService for UI development.
  * Simulates the import process with random results and artificial delays.
  */
class MockTransactionImportService extends TransactionImportService:
    private val random = new Random()
    private var currentStatus: ImportStatus = ImportStatus.NotStarted
    private var lastImportResults: Option[ImportResults] = None
    private var importStartTime: Option[Instant] = None

    /** Get the initial view model for the import page.
      *
      * @return
      *   A ZIO effect that returns the ImportPageViewModel with default values
      */
    override def getImportViewModel(): ZIO[Any, String, ImportPageViewModel] =
        ZIO.succeed(
            ImportPageViewModel(
                startDate = LocalDate.now().withDayOfMonth(1),
                endDate = LocalDate.now(),
                importStatus = currentStatus,
                importResults = lastImportResults,
                validationError = None
            )
        )

    /** Validate a date range based on business rules.
      *
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect with Either an error message (Left) or success (Right)
      */
    override def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, Either[String, Unit]] =
        ZIO.succeed {
            if startDate == null || endDate == null then
                Left("Both start and end dates are required")
            else if startDate.isAfter(endDate) then
                Left("Start date cannot be after end date")
            else if startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now()) then
                Left("Dates cannot be in the future")
            else if startDate.plusDays(90).isBefore(endDate) then
                Left("Date range cannot exceed 90 days (Fio Bank API limitation)")
            else
                Right(())
        }

    /** Import transactions for the specified date range.
      * Simulates a multi-step import process with artificial delays.
      *
      * @param startDate
      *   The start date for imported transactions
      * @param endDate
      *   The end date for imported transactions
      * @return
      *   A ZIO effect that returns ImportResults with randomly generated data
      */
    override def importTransactions(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults] =
        for
            // 5% chance of immediate failure for testing error handling
            shouldFail <- ZIO.succeed(random.nextInt(100) < 5)
            _ <- validateDateRange(startDate, endDate).flatMap {
                case Left(error) => ZIO.fail(error)
                case Right(_)    => ZIO.unit
            }
            _ <- ZIO.succeed {
                currentStatus = ImportStatus.InProgress
                importStartTime = Some(Instant.now())
            }

            // Initial connecting status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(800))

            // Fail randomly for testing error states
            _ <- ZIO.when(shouldFail)(ZIO.fail("Connection to Fio Bank failed: Network timeout"))

            // Retrieving transactions status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(1200))

            // Random transaction count based on date range (minimum 1, maximum days between dates)
            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt + 1
            transactionCount = if daysBetween <= 0 then 0 else random.nextInt(daysBetween) + 1

            // Storing transactions status - simulate delay based on count
            _ <- ZIO.sleep(Duration.fromMillis(500 + (transactionCount * 50).min(2000)))

            // Complete the import
            results <- ZIO.succeed {
                val now = Instant.now()
                val importResults = ImportResults(
                    transactionCount = transactionCount,
                    errorMessage = None,
                    startTime = importStartTime.getOrElse(now.minusSeconds(5)),
                    endTime = Some(now)
                )
                lastImportResults = Some(importResults)
                currentStatus = ImportStatus.Completed
                importResults
            }
        yield results

    /** Get the current status of the import operation.
      *
      * @return
      *   A ZIO effect that returns the current ImportStatus
      */
    override def getImportStatus(): ZIO[Any, String, ImportStatus] =
        ZIO.succeed(currentStatus)
end MockTransactionImportService

object MockTransactionImportService:
    /** Create a new instance of MockTransactionImportService.
      *
      * @return
      *   A ZLayer that provides a MockTransactionImportService
      */
    val layer: ULayer[TransactionImportService] =
        ZLayer.succeed(new MockTransactionImportService())
end MockTransactionImportService