package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import java.time.LocalDate
import zio.*

/** In-memory implementation of ImportBatchRepository for testing and development.
  *
  * Stores import batches in memory without persistence.
  * Uses ZIO Ref for thread-safe concurrent access.
  *
  * Category: Repository Implementation
  * Layer: Infrastructure
  */
final case class InMemoryImportBatchRepository(
    batchesRef: Ref[Map[ImportBatchId, ImportBatch]]
) extends ImportBatchRepository:

  /** Saves an import batch.
    * Uses atomic update to ensure thread-safety.
    *
    * @param importBatch The import batch to save
    * @return A ZIO effect that completes with Unit or fails with an error
    */
  override def save(importBatch: ImportBatch): ZIO[Any, String, Unit] =
    batchesRef.update { batches =>
      batches + (importBatch.id -> importBatch)
    }

  /** Finds an import batch by ID.
    *
    * @param id The ID of the import batch to find
    * @return A ZIO effect that completes with the found batch or None if not found
    */
  override def findById(id: ImportBatchId): ZIO[Any, String, Option[ImportBatch]] =
    batchesRef.get.map(_.get(id))

  /** Finds all import batches for an account.
    *
    * @param accountId The account ID to find batches for
    * @return A ZIO effect that completes with a list of found batches
    */
  override def findByAccountId(accountId: AccountId): ZIO[Any, String, List[ImportBatch]] =
    batchesRef.get.map { batches =>
      batches.values
        .filter(_.accountId == accountId)
        .toList
        .sortBy(_.createdAt)
    }

  /** Finds the most recent import batch for an account.
    *
    * @param accountId The account ID to find the most recent batch for
    * @return A ZIO effect that completes with the most recent batch or None
    */
  override def findMostRecentByAccountId(accountId: AccountId): ZIO[Any, String, Option[ImportBatch]] =
    batchesRef.get.map { batches =>
      batches.values
        .filter(_.accountId == accountId)
        .toList
        .sortBy(_.createdAt)(Ordering[java.time.Instant].reverse)
        .headOption
    }

  /** Finds all import batches with a specific status.
    *
    * @param status The status to filter by
    * @return A ZIO effect that completes with a list of found batches
    */
  override def findByStatus(status: ImportStatus): ZIO[Any, String, List[ImportBatch]] =
    batchesRef.get.map { batches =>
      batches.values
        .filter(_.status == status)
        .toList
        .sortBy(_.createdAt)
    }

  /** Finds all import batches for an account within a date range.
    *
    * @param accountId The account ID to find batches for
    * @param startDate The start date of the range
    * @param endDate The end date of the range
    * @return A ZIO effect that completes with a list of found batches
    */
  override def findByDateRange(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, String, List[ImportBatch]] =
    batchesRef.get.map { batches =>
      batches.values
        .filter { batch =>
          batch.accountId == accountId &&
          !batch.endDate.isBefore(startDate) &&
          !batch.startDate.isAfter(endDate)
        }
        .toList
        .sortBy(_.createdAt)
    }

/** Companion object for InMemoryImportBatchRepository.
  */
object InMemoryImportBatchRepository:
  /** Creates an in-memory implementation of ImportBatchRepository.
    *
    * Initializes a thread-safe Ref containing an empty Map for storing batches.
    *
    * @return
    *   A ZLayer that provides an ImportBatchRepository
    */
  val layer: ULayer[ImportBatchRepository] =
    ZLayer.scoped {
      for {
        batchesRef <- Ref.make(Map.empty[ImportBatchId, ImportBatch])
        repo = InMemoryImportBatchRepository(batchesRef)
      } yield repo
    }