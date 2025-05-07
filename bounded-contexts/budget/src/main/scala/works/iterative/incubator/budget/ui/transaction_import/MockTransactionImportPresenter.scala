package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import works.iterative.incubator.budget.domain.model.AccountId
import java.time.{Instant, LocalDate}
import zio.*
import scala.util.Random

/** Enum representing different import scenarios for demonstration
  */
enum ImportScenario:
    case SuccessfulImport, NoTransactions, ErrorDuringImport

/** Mock implementation of TransactionImportPresenter for UI development. Simulates the import process
  * with configurable scenarios for demonstration.
  *
  * Category: Presenter
  * Layer: UI/Presentation
  */
class MockTransactionImportPresenter extends TransactionImportPresenter:
    private val random = new Random()
    private var currentStatus: ImportStatus = ImportStatus.NotStarted
    private var lastImportResults: Option[ImportResults] = None
    private var importStartTime: Option[Instant] = None

    // Default to successful import scenario
    private var activeScenario: ImportScenario = ImportScenario.SuccessfulImport

    /** Set the active scenario for demonstration purposes
      */
    def setScenario(scenario: ImportScenario): Unit =
        activeScenario = scenario
        // Reset status for a clean demonstration
        currentStatus = ImportStatus.NotStarted
        lastImportResults = None
        importStartTime = None
    end setScenario

    /** Get the initial view model for the import page.
      *
      * @return
      *   A ZIO effect that returns the ImportPageViewModel with default values
      */
    override def getImportViewModel(): ZIO[Any, String, ImportPageViewModel] =
        for
            accounts <- getAccounts()
        yield ImportPageViewModel(
            accounts = accounts,
            selectedAccountId = Some("0300-0987654321"), // Default select the ČSOB account
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now(),
            importStatus = currentStatus,
            importResults = lastImportResults,
            validationError = None,
            accountValidationError = None
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
        
    /** Validate an account ID string.
      *
      * @param accountIdStr
      *   The account ID string to validate
      * @return
      *   A ZIO effect with Either an error message (Left) or the parsed AccountId (Right)
      */
    override def validateAccountId(
        accountIdStr: String
    ): ZIO[Any, String, Either[String, AccountId]] =
        ZIO.succeed {
            if accountIdStr == null || accountIdStr.isEmpty then
                Left("Account selection is required")
            else 
                AccountId.fromString(accountIdStr) match
                    case Right(accountId) => Right(accountId)
                    case Left(error) => Left(s"Invalid account ID: $error")
        }

    /** Import transactions for the specified account and date range based on the active scenario.
      *
      * @param accountId
      *   The account to import transactions from
      * @param startDate
      *   The start date for imported transactions
      * @param endDate
      *   The end date for imported transactions
      * @return
      *   A ZIO effect that returns ImportResults or an error string
      */
    override def importTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults] =
        for
            // Validate date range first
            _ <- validateDateRange(startDate, endDate).flatMap {
                case Left(error) => ZIO.fail(error)
                case Right(_)    => ZIO.unit
            }

            // Start the import process
            _ <- ZIO.succeed {
                currentStatus = ImportStatus.InProgress
                importStartTime = Some(Instant.now())
            }

            // Initial connecting status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(800))

            // Process according to the active scenario
            results <- activeScenario match
                case ImportScenario.SuccessfulImport =>
                    handleSuccessfulImport(accountId, startDate, endDate)

                case ImportScenario.NoTransactions =>
                    handleNoTransactionsScenario

                case ImportScenario.ErrorDuringImport =>
                    handleErrorScenario
        yield results

    /** Handle the successful import scenario with random transaction count
      */
    private def handleSuccessfulImport(
        accountId: AccountId, // Using the account ID for future features
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults] =
        for
            // Retrieving transactions status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(1200))

            // Random transaction count based on date range (1 to days between dates)
            daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt + 1
            transactionCount = if daysBetween <= 0 then 1 else (random.nextInt(daysBetween) + 1)

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

    /** Handle the scenario where no transactions are found
      */
    private def handleNoTransactionsScenario: ZIO[Any, String, ImportResults] =
        for
            // Retrieving transactions status - simulate delay
            _ <- ZIO.sleep(Duration.fromMillis(1000))

            // Complete the import with zero transactions
            results <- ZIO.succeed {
                val now = Instant.now()
                val importResults = ImportResults(
                    transactionCount = 0,
                    errorMessage = None,
                    startTime = importStartTime.getOrElse(now.minusSeconds(3)),
                    endTime = Some(now)
                )
                lastImportResults = Some(importResults)
                currentStatus = ImportStatus.Completed
                importResults
            }
        yield results

    /** Handle the error scenario where the API is unavailable
      */
    private def handleErrorScenario: ZIO[Any, Nothing, ImportResults] =
        for
            // Short delay to simulate connection attempt
            _ <- ZIO.sleep(Duration.fromMillis(1500))

            // Set error status
            results <- ZIO.succeed {
                val now = Instant.now()
                val importResults = ImportResults(
                    transactionCount = 0,
                    errorMessage = Some("Connection to Fio Bank failed: Network timeout"),
                    startTime = importStartTime.getOrElse(now.minusSeconds(2)),
                    endTime = Some(now)
                )
                lastImportResults = Some(importResults)
                currentStatus = ImportStatus.Error
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
        
    /** Get the list of available accounts.
      *
      * @return
      *   A ZIO effect that returns a list of AccountOption
      */
    override def getAccounts(): ZIO[Any, String, List[AccountOption]] =
        ZIO.succeed(AccountSelectorViewModel.defaultAccounts)
end MockTransactionImportPresenter

object MockTransactionImportPresenter:
    /** Create a new instance of MockTransactionImportPresenter.
      *
      * @return
      *   A ZLayer that provides a MockTransactionImportPresenter
      */
    val layer: ULayer[TransactionImportPresenter] =
        ZLayer.succeed(new MockTransactionImportPresenter())
end MockTransactionImportPresenter