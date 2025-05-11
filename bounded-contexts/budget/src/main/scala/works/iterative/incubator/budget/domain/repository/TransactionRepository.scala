package works.iterative.incubator.budget.domain.repository

import works.iterative.incubator.budget.domain.model.{
    ImportBatchId,
    Transaction,
    TransactionId,
    AccountId,
    TransactionStatus
}
import java.time.LocalDate
import zio.*

/** Repository interface for Transaction entities.
  *
  * Category: Repository Interface Layer: Domain
  */
trait TransactionRepository:
    /** Saves a transaction to the repository.
      *
      * @param transaction
      *   The transaction to save
      * @return
      *   A ZIO effect that returns Unit or an error string
      */
    def save(transaction: Transaction): ZIO[Any, String, Unit]

    /** Saves multiple transactions in a batch.
      *
      * @param transactions
      *   The list of transactions to save
      * @return
      *   A ZIO effect that returns Unit or an error string
      */
    def saveAll(transactions: List[Transaction]): ZIO[Any, String, Unit]

    /** Finds a transaction by its ID.
      *
      * @param id
      *   The transaction ID to look for
      * @return
      *   A ZIO effect that returns an Option containing the transaction if found, or an error
      *   string
      */
    def findById(id: TransactionId): ZIO[Any, String, Option[Transaction]]

    /** Finds all transactions for a given account and date range.
      *
      * @param accountId
      *   The account ID to filter by
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that returns a list of transactions or an error string
      */
    def findByAccountAndDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, List[Transaction]]

    /** Finds all transactions associated with a specific import batch.
      *
      * @param importBatchId
      *   The import batch ID to filter by
      * @return
      *   A ZIO effect that returns a list of transactions or an error string
      */
    def findByImportBatch(importBatchId: ImportBatchId): ZIO[Any, String, List[Transaction]]

    /** Updates the status of all transactions in a batch.
      *
      * @param importBatchId
      *   The import batch ID
      * @param status
      *   The new status to set
      * @return
      *   A ZIO effect that returns the count of updated transactions or an error string
      */
    def updateStatusByImportBatch(
        importBatchId: ImportBatchId,
        status: TransactionStatus
    ): ZIO[Any, String, Int]

    /** Counts transactions by status.
      *
      * @param status
      *   The status to count
      * @return
      *   A ZIO effect that returns the count or an error string
      */
    def countByStatus(status: TransactionStatus): ZIO[Any, String, Int]
end TransactionRepository

/** Companion object for TransactionRepository.
  */
object TransactionRepository:
    /** Accesses the repository to save a transaction.
      *
      * @param transaction
      *   The transaction to save
      * @return
      *   A ZIO effect that requires TransactionRepository and returns Unit or an error string
      */
    def save(transaction: Transaction): ZIO[TransactionRepository, String, Unit] =
        ZIO.serviceWithZIO(_.save(transaction))

    /** Accesses the repository to save multiple transactions.
      *
      * @param transactions
      *   The list of transactions to save
      * @return
      *   A ZIO effect that requires TransactionRepository and returns Unit or an error string
      */
    def saveAll(transactions: List[Transaction]): ZIO[TransactionRepository, String, Unit] =
        ZIO.serviceWithZIO(_.saveAll(transactions))

    /** Accesses the repository to find a transaction by ID.
      *
      * @param id
      *   The transaction ID to look for
      * @return
      *   A ZIO effect that requires TransactionRepository and returns an Option containing the
      *   transaction if found, or an error string
      */
    def findById(id: TransactionId): ZIO[TransactionRepository, String, Option[Transaction]] =
        ZIO.serviceWithZIO(_.findById(id))

    /** Accesses the repository to find transactions by account and date range.
      *
      * @param accountId
      *   The account ID to filter by
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires TransactionRepository and returns a list of transactions or an
      *   error string
      */
    def findByAccountAndDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionRepository, String, List[Transaction]] =
        ZIO.serviceWithZIO(_.findByAccountAndDateRange(accountId, startDate, endDate))

    /** Accesses the repository to find transactions by import batch.
      *
      * @param importBatchId
      *   The import batch ID to filter by
      * @return
      *   A ZIO effect that requires TransactionRepository and returns a list of transactions or an
      *   error string
      */
    def findByImportBatch(importBatchId: ImportBatchId)
        : ZIO[TransactionRepository, String, List[Transaction]] =
        ZIO.serviceWithZIO(_.findByImportBatch(importBatchId))

    /** Accesses the repository to update transaction statuses by import batch.
      *
      * @param importBatchId
      *   The import batch ID
      * @param status
      *   The new status to set
      * @return
      *   A ZIO effect that requires TransactionRepository and returns the count of updated
      *   transactions or an error string
      */
    def updateStatusByImportBatch(
        importBatchId: ImportBatchId,
        status: TransactionStatus
    ): ZIO[TransactionRepository, String, Int] =
        ZIO.serviceWithZIO(_.updateStatusByImportBatch(importBatchId, status))

    /** Accesses the repository to count transactions by status.
      *
      * @param status
      *   The status to count
      * @return
      *   A ZIO effect that requires TransactionRepository and returns the count or an error string
      */
    def countByStatus(status: TransactionStatus): ZIO[TransactionRepository, String, Int] =
        ZIO.serviceWithZIO(_.countByStatus(status))
end TransactionRepository
