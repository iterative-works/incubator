package works.iterative.incubator.budget.application.service

import works.iterative.incubator.budget.domain.model.{AccountId, ImportBatch, ImportBatchId}
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.domain.service.TransactionImportError.*
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportPresenter
import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.{Instant, LocalDate}
import zio.*

// Import domain ImportStatus with a different name to avoid conflict
import works.iterative.incubator.budget.domain.model.{ImportStatus => DomainImportStatus}

/** Live implementation of the TransactionImportPresenter.
  *
  * This implementation bridges between the UI layer and the domain services, providing an adapter
  * that translates between UI view models and domain objects.
  *
  * @param transactionImportService
  *   The domain service for transaction imports
  * @param accountId
  *   The account ID to use for imports (hardcoded for now, will be configurable in future)
  *
  * Category: Application Service
  * Layer: Application
  */
final case class TransactionImportPresenterLive(
    transactionImportService: TransactionImportService,
    accountId: AccountId
) extends TransactionImportPresenter:

  /** The ID of the most recent import batch, cached to track status */
  private var currentImportBatchId: Option[ImportBatchId] = None

  override def getImportViewModel(): ZIO[Any, String, ImportPageViewModel] =
    (for
      // Get the most recent import batch to initialize the view model
      maybeBatch <- transactionImportService.getMostRecentImport(accountId)
      
      // Get initial view model state
      importStatus <- maybeBatch match
        case Some(batch) =>
          currentImportBatchId = Some(batch.id)
          ZIO.succeed(domainToUiStatus(batch.status))
        case None =>
          ZIO.succeed(ImportStatus.NotStarted)
          
      importResults <- maybeBatch match
        case Some(batch) =>
          ZIO.succeed(
            Some(
              ImportResults(
                transactionCount = batch.transactionCount,
                errorMessage = batch.errorMessage,
                startTime = batch.startTime,
                endTime = batch.endTime
              )
            )
          )
        case None =>
          ZIO.succeed(None)
    yield
      ImportPageViewModel(
        startDate = LocalDate.now().withDayOfMonth(1),
        endDate = LocalDate.now(),
        importStatus = importStatus,
        importResults = importResults,
        validationError = None
      )).mapError(err => s"Failed to get import page view model: $err")

  override def validateDateRange(
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, String, Either[String, Unit]] =
    transactionImportService
      .validateDateRange(startDate, endDate)
      .map(_ => Right(()))
      .catchAll {
        case InvalidDateRange(msg) => ZIO.succeed(Left(msg))
        case other => ZIO.fail(s"Unexpected error validating date range: $other")
      }

  override def importTransactions(
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, String, ImportResults] =
    (for
      // Validate date range
      _ <- transactionImportService.validateDateRange(startDate, endDate)
      
      // Start the import and save the batch ID for status tracking
      importBatch <- transactionImportService.importTransactions(accountId, startDate, endDate)
      _ <- ZIO.succeed { currentImportBatchId = Some(importBatch.id) }
      
      // Create UI-friendly result
      result = ImportResults(
        transactionCount = importBatch.transactionCount,
        errorMessage = importBatch.errorMessage,
        startTime = importBatch.startTime,
        endTime = importBatch.endTime
      )
    yield result).catchAll {
      case InvalidDateRange(msg) => ZIO.fail(s"Invalid date range: $msg")
      case NoTransactionsFound(start, end) => 
        ZIO.succeed(
          ImportResults(
            transactionCount = 0,
            errorMessage = Some(s"No transactions found between $start and $end"),
            startTime = Instant.now(),
            endTime = Some(Instant.now())
          )
        )
      case BankApiError(msg, _) => ZIO.fail(s"Bank API error: $msg")
      case TransactionStorageError(msg, _) => ZIO.fail(s"Failed to save transactions: $msg")
      case ImportBatchError(msg, _) => ZIO.fail(s"Import batch error: $msg")
      case UnexpectedError(msg, _) => ZIO.fail(s"Unexpected error: $msg")
    }

  override def getImportStatus(): ZIO[Any, String, ImportStatus] =
    currentImportBatchId match
      case Some(batchId) =>
        transactionImportService
          .getImportStatus(batchId)
          .map(batch => domainToUiStatus(batch.status))
          .mapError(err => s"Failed to get import status: $err")
      case None =>
        ZIO.succeed(ImportStatus.NotStarted)

  /** Converts domain ImportStatus to UI ImportStatus.
    *
    * @param status
    *   The domain status
    * @return
    *   The UI status
    */
  private def domainToUiStatus(status: DomainImportStatus): ImportStatus =
    status match
      case DomainImportStatus.NotStarted => ImportStatus.NotStarted
      case DomainImportStatus.InProgress => ImportStatus.InProgress
      case DomainImportStatus.Completed  => ImportStatus.Completed
      case DomainImportStatus.Error      => ImportStatus.Error

/** Companion object for TransactionImportPresenterLive.
  */
object TransactionImportPresenterLive:
  /** Creates a live implementation of TransactionImportPresenter.
    *
    * @return
    *   A ZLayer that requires TransactionImportService and provides a TransactionImportPresenter
    */
  val layer: ZLayer[TransactionImportService, Nothing, TransactionImportPresenter] =
    ZLayer {
      for
        transactionImportService <- ZIO.service[TransactionImportService]
        // TODO: In a real implementation, this would come from configuration or user selection
        accountId = AccountId("fio", "default-account")
      yield TransactionImportPresenterLive(transactionImportService, accountId)
    }