package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import works.iterative.incubator.budget.domain.model.AccountId
import java.time.{Instant, LocalDate}
import zio.*
import scala.util.Random
import scala.util.Try

/** Enum representing different import scenarios for demonstration
  */
enum ImportScenario:
    case SuccessfulImport, NoTransactions, ErrorDuringImport

/** Mock implementation of TransactionImportPresenter for UI development. Simulates the import
  * process with configurable scenarios for demonstration.
  *
  * Category: Presenter Layer: UI/Presentation
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

    /** Get the initial form view model for the import page.
      *
      * @return
      *   A ZIO effect that returns the TransactionImportFormViewModel with default values
      */
    override def getImportViewModel(): ZIO[Any, String, TransactionImportFormViewModel] =
        for
            accounts <- getAccounts()
        yield TransactionImportFormViewModel(
            accounts = accounts,
            selectedAccountId = None,
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now(),
            importStatus = currentStatus,
            importResults = lastImportResults
        )

    /** Validate and process a transaction import command.
      *
      * @param command
      *   The command to validate and process
      * @return
      *   A ZIO effect that returns Either validation errors or import results
      */
    override def validateAndProcess(
        command: TransactionImportCommand
    ): ZIO[Any, String, Either[ValidationErrors, ImportResults]] =
        ZIO.attempt {
            // Parse dates to validate them
            val startDateResult = Try(LocalDate.parse(command.startDate))
            val endDateResult = Try(LocalDate.parse(command.endDate))
            val accountIdResult = AccountId.fromString(command.accountId)

            // Collect validation errors
            val errors = Map.newBuilder[String, String]

            // Validate account ID
            if command.accountId.isEmpty then
                errors += ("accountId" -> "Account selection is required")
            else
                accountIdResult match
                    case Left(error) => errors += ("accountId" -> s"Invalid account ID: $error")
                    case Right(_)    => ()
            end if

            // Validate start date
            if command.startDate.isEmpty then
                errors += ("startDate" -> "Start date is required")
            else if startDateResult.isFailure then
                errors += ("startDate" -> "Invalid start date format")

            // Validate end date
            if command.endDate.isEmpty then
                errors += ("endDate" -> "End date is required")
            else if endDateResult.isFailure then
                errors += ("endDate" -> "Invalid end date format")

            // Cross-field validation for dates
            if startDateResult.isSuccess && endDateResult.isSuccess then
                val startDate = startDateResult.get
                val endDate = endDateResult.get

                val dateValidation = if startDate.isAfter(endDate) then
                    Some("Start date cannot be after end date")
                else if startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now()) then
                    Some("Dates cannot be in the future")
                else if startDate.plusDays(90).isBefore(endDate) then
                    Some("Date range cannot exceed 90 days (Fio Bank API limitation)")
                else
                    None

                dateValidation match
                    case Some(error) => errors += ("dateRange" -> error)
                    case None        => ()
            end if

            val validationErrors = ValidationErrors(errors.result())

            if !validationErrors.hasErrors then
                // Start the import process
                currentStatus = ImportStatus.InProgress
                importStartTime = Some(Instant.now())

                // For the mock, we're synchronously processing the import
                // In a real implementation, this would likely be asynchronous
                val result = activeScenario match
                    case ImportScenario.SuccessfulImport =>
                        // Random transaction count based on date range (1 to days between dates)
                        val startDate = startDateResult.get
                        val endDate = endDateResult.get
                        val daysBetween =
                            java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt + 1
                        val transactionCount =
                            if daysBetween <= 0 then 1 else (random.nextInt(daysBetween) + 1)

                        val now = Instant.now()
                        val importResults = ImportResults(
                            transactionCount = transactionCount,
                            errorMessage = None,
                            startTime = importStartTime.getOrElse(now.minusSeconds(5)),
                            endTime = Some(now)
                        )
                        lastImportResults = Some(importResults)
                        currentStatus = ImportStatus.Completed
                        Right(importResults)

                    case ImportScenario.NoTransactions =>
                        val now = Instant.now()
                        val importResults = ImportResults(
                            transactionCount = 0,
                            errorMessage = None,
                            startTime = importStartTime.getOrElse(now.minusSeconds(3)),
                            endTime = Some(now)
                        )
                        lastImportResults = Some(importResults)
                        currentStatus = ImportStatus.Completed
                        Right(importResults)

                    case ImportScenario.ErrorDuringImport =>
                        val now = Instant.now()
                        val importResults = ImportResults(
                            transactionCount = 0,
                            errorMessage = Some("Connection to Fio Bank failed: Network timeout"),
                            startTime = importStartTime.getOrElse(now.minusSeconds(2)),
                            endTime = Some(now)
                        )
                        lastImportResults = Some(importResults)
                        currentStatus = ImportStatus.Error
                        Right(importResults)

                result
            else
                Left(validationErrors)
            end if
        }.mapError(_.getMessage)

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
