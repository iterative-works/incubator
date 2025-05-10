package works.iterative.incubator.budget.domain.service

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.service.TransactionImportError.*
import java.time.LocalDate
import zio.*

/** Service for importing transactions from a bank into the system.
  *
  * This service handles the core domain logic for importing transactions, including validating date
  * ranges, interacting with the bank API, and managing import batches.
  *
  * Category: Domain Service Layer: Domain
  */
trait TransactionImportService:
    /** Validates a date range for a specific account based on bank-specific rules. This delegates
      * validation to the appropriate bank transaction service.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if the date range is valid, or fails with an
      *   InvalidDateRange error if invalid
      */
    def validateDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit]

    /** Validates a date range without account context.
      *
      * @deprecated
      *   Use validateDateRange(accountId, startDate, endDate) instead
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if the date range is valid, or fails with an
      *   InvalidDateRange error if invalid
      */
    @deprecated("Use validateDateRange with accountId parameter instead", "2025.05")
    def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit]

    /** Imports transactions for the specified date range from the bank.
      *
      * This method handles the complete import process:
      *   1. Validates the date range 2. Creates an import batch record 3. Fetches transactions from
      *      the bank API 4. Saves the transactions to the repository 5. Updates the import batch
      *      with results
      *
      * @param accountId
      *   The account ID to import transactions for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that returns the completed import batch or an error
      */
    def importTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, ImportBatch]

    /** Gets the current status of an import batch.
      *
      * @param batchId
      *   The ID of the import batch to check
      * @return
      *   A ZIO effect that returns the import batch or an error if not found
      */
    def getImportStatus(
        batchId: ImportBatchId
    ): ZIO[Any, TransactionImportError, ImportBatch]

    /** Gets the most recent import batch for an account.
      *
      * @param accountId
      *   The account ID to get the latest import for
      * @return
      *   A ZIO effect that returns an Option containing the most recent import batch if any exists,
      *   or None if no imports have been done
      */
    def getMostRecentImport(
        accountId: AccountId
    ): ZIO[Any, TransactionImportError, Option[ImportBatch]]
end TransactionImportService

/** Implementation of the TransactionImportService.
  *
  * @param transactionRepository
  *   Repository for saving and retrieving transactions
  * @param importBatchRepository
  *   Repository for managing import batches
  * @param bankTransactionService
  *   Service for fetching transactions from a bank API
  */
final case class TransactionImportServiceLive(
    transactionRepository: TransactionRepository,
    importBatchRepository: ImportBatchRepository,
    bankTransactionService: BankTransactionService
) extends TransactionImportService:

    /** Delegates date range validation to the bank transaction service. Each bank may have
      * different validation rules based on the account.
      */
    override def validateDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        bankTransactionService.validateDateRangeForAccount(accountId, startDate, endDate)

    /** Legacy validation method without account context.
      *
      * @deprecated
      *   Use validateDateRange with accountId parameter instead
      */
    @deprecated("Use validateDateRange with accountId parameter instead", "2025.05")
    override def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        bankTransactionService.validateDateRange(startDate, endDate)

    override def importTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, ImportBatch] =
        for
            // Step 1: Validate the date range through the bank service
            // Use account-aware validation to apply bank-specific rules
            _ <- validateDateRange(accountId, startDate, endDate)

            // Step 2: Create an import batch record through the repository
            // This automatically assigns a unique ID and saves the batch
            initialBatch <- ImportBatchRepository
                .createBatch(accountId, startDate, endDate)
                .provide(ZLayer.succeed(importBatchRepository))
                .mapError(err => ImportBatchError(s"Failed to create import batch: $err", None))

            // Step 3: Update batch to in-progress
            inProgressBatch <- ZIO
                .fromEither(initialBatch.markInProgress())
                .mapError(msg =>
                    ImportBatchError(s"Failed to mark batch as in progress: $msg", None)
                )
            _ <- importBatchRepository
                .save(inProgressBatch)
                .mapError(err =>
                    ImportBatchError(s"Failed to update import batch status: $err", None)
                )

            // Step 4: Fetch transactions from bank API
            transactions <- fetchBankTransactions(accountId, startDate, endDate, inProgressBatch.id)
                .tapError(err =>
                    // On failure, mark the batch as failed
                    markBatchFailed(inProgressBatch, err.toString).ignore
                )

            // Step 5: Save transactions to repository
            _ <- transactionRepository
                .saveAll(transactions)
                .mapError(err =>
                    TransactionStorageError(s"Failed to save transactions: $err", None)
                )
                .tapError(err =>
                    // On failure, mark the batch as failed
                    markBatchFailed(inProgressBatch, err.toString).ignore
                )

            // Step 6: Mark the batch as completed
            completedBatch <- ZIO
                .fromEither(inProgressBatch.markCompleted(transactions.size))
                .mapError(msg => ImportBatchError(s"Failed to mark batch as completed: $msg", None))

            // Step 7: Save the updated batch
            _ <- importBatchRepository
                .save(completedBatch)
                .mapError(err => ImportBatchError(s"Failed to save completed batch: $err", None))
        yield completedBatch

    override def getImportStatus(
        batchId: ImportBatchId
    ): ZIO[Any, TransactionImportError, ImportBatch] =
        importBatchRepository
            .findById(batchId)
            .mapError(err => ImportBatchError(s"Failed to get import batch: $err", None))
            .flatMap {
                case Some(batch) => ZIO.succeed(batch)
                case None =>
                    ZIO.fail(ImportBatchError(s"Import batch not found with ID: $batchId", None))
            }

    override def getMostRecentImport(
        accountId: AccountId
    ): ZIO[Any, TransactionImportError, Option[ImportBatch]] =
        importBatchRepository
            .findMostRecentByAccountId(accountId)
            .mapError(err => ImportBatchError(s"Failed to get recent import batch: $err", None))

    /** Helper method to fetch transactions from the bank API.
      *
      * @param accountId
      *   The account ID to fetch transactions for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that returns a list of transactions or an error
      */
    private def fetchBankTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate,
        importBatchId: ImportBatchId // Not used, but kept for backward compatibility
    ): ZIO[Any, TransactionImportError, List[Transaction]] =
        bankTransactionService
            .fetchTransactions(accountId, startDate, endDate)
            .mapError(err =>
                BankApiError(
                    s"Failed to fetch transactions from bank: ${err.getMessage}",
                    Some(err)
                )
            )
            .flatMap { transactions =>
                if transactions.isEmpty then
                    ZIO.fail(NoTransactionsFound(startDate, endDate))
                else
                    ZIO.succeed(transactions)
            }

    /** Helper method to mark an import batch as failed.
      *
      * @param batch
      *   The batch to mark as failed
      * @param errorMessage
      *   The error message to record
      * @return
      *   A ZIO effect that returns the updated batch or an error
      */
    private def markBatchFailed(
        batch: ImportBatch,
        errorMessage: String
    ): ZIO[Any, TransactionImportError, ImportBatch] =
        for
            failedBatch <- ZIO
                .fromEither(batch.markFailed(errorMessage))
                .mapError(msg => ImportBatchError(s"Failed to mark batch as failed: $msg", None))
            _ <- importBatchRepository
                .save(failedBatch)
                .mapError(err =>
                    ImportBatchError(s"Failed to save failed batch status: $err", None)
                )
        yield failedBatch
end TransactionImportServiceLive

/** Companion object for TransactionImportService.
  */
object TransactionImportService:
    /** Validates a date range for a specific account based on bank-specific business rules.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires TransactionImportService and completes successfully if the date
      *   range is valid, or fails with an InvalidDateRange error if invalid
      */
    def validateDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionImportService, TransactionImportError, Unit] =
        ZIO.serviceWithZIO(_.validateDateRange(accountId, startDate, endDate))

    /** Validates a date range without account context.
      *
      * @deprecated
      *   Use validateDateRange with accountId parameter instead
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires TransactionImportService and completes successfully if the date
      *   range is valid, or fails with an InvalidDateRange error if invalid
      */
    @deprecated("Use validateDateRange with accountId parameter instead", "2025.05")
    def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionImportService, TransactionImportError, Unit] =
        ZIO.serviceWithZIO(_.validateDateRange(startDate, endDate))

    /** Imports transactions for the specified date range from the bank.
      *
      * @param accountId
      *   The account ID to import transactions for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires TransactionImportService and returns the completed import batch
      *   or an error
      */
    def importTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionImportService, TransactionImportError, ImportBatch] =
        ZIO.serviceWithZIO(_.importTransactions(accountId, startDate, endDate))

    /** Gets the current status of an import batch.
      *
      * @param batchId
      *   The ID of the import batch to check
      * @return
      *   A ZIO effect that requires TransactionImportService and returns the import batch or an
      *   error if not found
      */
    def getImportStatus(
        batchId: ImportBatchId
    ): ZIO[TransactionImportService, TransactionImportError, ImportBatch] =
        ZIO.serviceWithZIO(_.getImportStatus(batchId))

    /** Gets the most recent import batch for an account.
      *
      * @param accountId
      *   The account ID to get the latest import for
      * @return
      *   A ZIO effect that requires TransactionImportService and returns an Option containing the
      *   most recent import batch if any exists, or None if no imports have been done
      */
    def getMostRecentImport(
        accountId: AccountId
    ): ZIO[TransactionImportService, TransactionImportError, Option[ImportBatch]] =
        ZIO.serviceWithZIO(_.getMostRecentImport(accountId))

    /** Creates a live implementation of TransactionImportService.
      *
      * @return
      *   A ZLayer that requires TransactionRepository, ImportBatchRepository, and
      *   BankTransactionService, and provides a TransactionImportService
      */
    val live: ZLayer[
        TransactionRepository & ImportBatchRepository & BankTransactionService,
        Nothing,
        TransactionImportService
    ] =
        ZLayer {
            for
                transactionRepository <- ZIO.service[TransactionRepository]
                importBatchRepository <- ZIO.service[ImportBatchRepository]
                bankTransactionService <- ZIO.service[BankTransactionService]
            yield TransactionImportServiceLive(
                transactionRepository,
                importBatchRepository,
                bankTransactionService
            )
        }
end TransactionImportService
