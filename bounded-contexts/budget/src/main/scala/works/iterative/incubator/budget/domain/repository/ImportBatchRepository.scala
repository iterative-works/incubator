package works.iterative.incubator.budget.domain.repository

import works.iterative.incubator.budget.domain.model.{
    ImportBatch,
    ImportBatchId,
    AccountId,
    ImportStatus
}
import java.time.LocalDate
import zio.*

/** Repository interface for ImportBatch entities.
  *
  * Category: Repository Interface
  * Layer: Domain
  */
trait ImportBatchRepository:
    /** Saves an import batch to the repository.
      *
      * @param importBatch
      *   The import batch to save
      * @return
      *   A ZIO effect that returns Unit or an error string
      */
    def save(importBatch: ImportBatch): ZIO[Any, String, Unit]

    /** Finds an import batch by its ID.
      *
      * @param id
      *   The import batch ID to look for
      * @return
      *   A ZIO effect that returns an Option containing the import batch if found, or an error
      *   string
      */
    def findById(id: ImportBatchId): ZIO[Any, String, Option[ImportBatch]]

    /** Finds all import batches for a specific account.
      *
      * @param accountId
      *   The account ID to filter by
      * @return
      *   A ZIO effect that returns a list of import batches or an error string
      */
    def findByAccountId(accountId: AccountId): ZIO[Any, String, List[ImportBatch]]

    /** Finds the most recent import batch for an account.
      *
      * @param accountId
      *   The account ID to look for
      * @return
      *   A ZIO effect that returns an Option containing the most recent import batch if found, or
      *   an error string
      */
    def findMostRecentByAccountId(accountId: AccountId): ZIO[Any, String, Option[ImportBatch]]

    /** Finds all import batches with a specific status.
      *
      * @param status
      *   The status to filter by
      * @return
      *   A ZIO effect that returns a list of import batches or an error string
      */
    def findByStatus(status: ImportStatus): ZIO[Any, String, List[ImportBatch]]

    /** Finds import batches for a specific date range.
      *
      * @param accountId
      *   The account ID to filter by
      * @param startDate
      *   The start date that must be covered by the import
      * @param endDate
      *   The end date that must be covered by the import
      * @return
      *   A ZIO effect that returns a list of import batches or an error string
      */
    def findByDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, List[ImportBatch]]
end ImportBatchRepository

/** Companion object for ImportBatchRepository.
  */
object ImportBatchRepository:
    /** Accesses the repository to save an import batch.
      *
      * @param importBatch
      *   The import batch to save
      * @return
      *   A ZIO effect that requires ImportBatchRepository and returns Unit or an error string
      */
    def save(importBatch: ImportBatch): ZIO[ImportBatchRepository, String, Unit] =
        ZIO.serviceWithZIO(_.save(importBatch))

    /** Accesses the repository to find an import batch by ID.
      *
      * @param id
      *   The import batch ID to look for
      * @return
      *   A ZIO effect that requires ImportBatchRepository and returns an Option containing the
      *   import batch if found, or an error string
      */
    def findById(id: ImportBatchId): ZIO[ImportBatchRepository, String, Option[ImportBatch]] =
        ZIO.serviceWithZIO(_.findById(id))

    /** Accesses the repository to find import batches by account.
      *
      * @param accountId
      *   The account ID to filter by
      * @return
      *   A ZIO effect that requires ImportBatchRepository and returns a list of import batches or
      *   an error string
      */
    def findByAccountId(accountId: AccountId)
        : ZIO[ImportBatchRepository, String, List[ImportBatch]] =
        ZIO.serviceWithZIO(_.findByAccountId(accountId))

    /** Accesses the repository to find the most recent import batch for an account.
      *
      * @param accountId
      *   The account ID to look for
      * @return
      *   A ZIO effect that requires ImportBatchRepository and returns an Option containing the most
      *   recent import batch if found, or an error string
      */
    def findMostRecentByAccountId(accountId: AccountId)
        : ZIO[ImportBatchRepository, String, Option[ImportBatch]] =
        ZIO.serviceWithZIO(_.findMostRecentByAccountId(accountId))

    /** Accesses the repository to find import batches by status.
      *
      * @param status
      *   The status to filter by
      * @return
      *   A ZIO effect that requires ImportBatchRepository and returns a list of import batches or
      *   an error string
      */
    def findByStatus(status: ImportStatus): ZIO[ImportBatchRepository, String, List[ImportBatch]] =
        ZIO.serviceWithZIO(_.findByStatus(status))

    /** Accesses the repository to find import batches for a specific date range.
      *
      * @param accountId
      *   The account ID to filter by
      * @param startDate
      *   The start date that must be covered by the import
      * @param endDate
      *   The end date that must be covered by the import
      * @return
      *   A ZIO effect that requires ImportBatchRepository and returns a list of import batches or
      *   an error string
      */
    def findByDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[ImportBatchRepository, String, List[ImportBatch]] =
        ZIO.serviceWithZIO(_.findByDateRange(accountId, startDate, endDate))
end ImportBatchRepository
