package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import java.time.LocalDate
import zio.*

/** In-memory implementation of ImportBatchRepository for testing and development.
  *
  * Stores import batches in memory without persistence. Uses ZIO Ref for thread-safe concurrent
  * access.
  *
  * Category: Repository Implementation Layer: Infrastructure
  */
final case class InMemoryImportBatchRepository(
    batchesRef: Ref[Map[ImportBatchId, ImportBatch]],
    sequenceCountersRef: Ref[Map[String, Long]]
) extends ImportBatchRepository:

    /** Saves an import batch. Uses atomic update to ensure thread-safety.
      *
      * @param importBatch
      *   The import batch to save
      * @return
      *   A ZIO effect that completes with Unit or fails with an error
      */
    override def save(importBatch: ImportBatch): ZIO[Any, String, Unit] =
        batchesRef.update { batches =>
            batches + (importBatch.id -> importBatch)
        }

    /** Finds an import batch by ID.
      *
      * @param id
      *   The ID of the import batch to find
      * @return
      *   A ZIO effect that completes with the found batch or None if not found
      */
    override def findById(id: ImportBatchId): ZIO[Any, String, Option[ImportBatch]] =
        batchesRef.get.map(_.get(id))

    /** Finds all import batches for an account.
      *
      * @param accountId
      *   The account ID to find batches for
      * @return
      *   A ZIO effect that completes with a list of found batches
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
      * @param accountId
      *   The account ID to find the most recent batch for
      * @return
      *   A ZIO effect that completes with the most recent batch or None
      */
    override def findMostRecentByAccountId(accountId: AccountId)
        : ZIO[Any, String, Option[ImportBatch]] =
        batchesRef.get.map { batches =>
            batches.values
                .filter(_.accountId == accountId)
                .toList
                .sortBy(_.createdAt)(Ordering[java.time.Instant].reverse)
                .headOption
        }

    /** Finds all import batches with a specific status.
      *
      * @param status
      *   The status to filter by
      * @return
      *   A ZIO effect that completes with a list of found batches
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
      * @param accountId
      *   The account ID to find batches for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes with a list of found batches
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

    /** Generates the next sequence number for import batches for a given account. This method is
      * thread-safe and ensures that sequence numbers are unique per account.
      *
      * @param accountId
      *   The account ID to generate the sequence number for
      * @return
      *   A ZIO effect that returns the next sequence number
      */
    override def nextSequenceNumber(accountId: AccountId): ZIO[Any, String, Long] =
        sequenceCountersRef.modify { counters =>
            val accountKey = accountId.toString
            val currentSeq = counters.getOrElse(accountKey, 0L)
            val nextSeq = currentSeq + 1L
            val updatedCounters = counters + (accountKey -> nextSeq)
            (nextSeq, updatedCounters)
        }
end InMemoryImportBatchRepository

/** Companion object for InMemoryImportBatchRepository.
  */
object InMemoryImportBatchRepository:
    /** Creates an in-memory implementation of ImportBatchRepository.
      *
      * Initializes thread-safe Refs containing empty Maps for storing batches and sequence
      * counters.
      *
      * @return
      *   A ZLayer that provides an ImportBatchRepository
      */
    val layer: ULayer[ImportBatchRepository] =
        ZLayer.scoped {
            for
                batchesRef <- Ref.make(Map.empty[ImportBatchId, ImportBatch])
                sequenceCountersRef <- Ref.make(Map.empty[String, Long])
                repo = InMemoryImportBatchRepository(batchesRef, sequenceCountersRef)
            yield repo
        }
end InMemoryImportBatchRepository
