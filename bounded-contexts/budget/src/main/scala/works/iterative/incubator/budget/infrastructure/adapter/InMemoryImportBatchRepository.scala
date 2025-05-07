package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import java.time.LocalDate
import zio.*
import scala.collection.mutable

/** In-memory implementation of ImportBatchRepository for testing and development.
  *
  * Stores import batches in memory without persistence.
  *
  * Category: Repository Implementation
  * Layer: Infrastructure
  */
final case class InMemoryImportBatchRepository() extends ImportBatchRepository:
  private val batches = mutable.Map[ImportBatchId, ImportBatch]()

  override def save(importBatch: ImportBatch): ZIO[Any, String, Unit] =
    ZIO.succeed {
      batches.put(importBatch.id, importBatch)
      ()
    }

  override def findById(id: ImportBatchId): ZIO[Any, String, Option[ImportBatch]] =
    ZIO.succeed(batches.get(id))

  override def findByAccountId(accountId: AccountId): ZIO[Any, String, List[ImportBatch]] =
    ZIO.succeed {
      batches.values.filter(_.accountId == accountId).toList.sortBy(_.createdAt)
    }

  override def findMostRecentByAccountId(accountId: AccountId): ZIO[Any, String, Option[ImportBatch]] =
    ZIO.succeed {
      batches.values
        .filter(_.accountId == accountId)
        .toList
        .sortBy(_.createdAt)(Ordering[java.time.Instant].reverse)
        .headOption
    }

  override def findByStatus(status: ImportStatus): ZIO[Any, String, List[ImportBatch]] =
    ZIO.succeed {
      batches.values.filter(_.status == status).toList.sortBy(_.createdAt)
    }

  override def findByDateRange(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, String, List[ImportBatch]] =
    ZIO.succeed {
      batches.values.filter { batch =>
        batch.accountId == accountId &&
        !batch.endDate.isBefore(startDate) &&
        !batch.startDate.isAfter(endDate)
      }.toList.sortBy(_.createdAt)
    }

/** Companion object for InMemoryImportBatchRepository.
  */
object InMemoryImportBatchRepository:
  /** Creates an in-memory implementation of ImportBatchRepository.
    *
    * @return
    *   A ZLayer that provides an ImportBatchRepository
    */
  val layer: ULayer[ImportBatchRepository] =
    ZLayer.succeed(InMemoryImportBatchRepository())