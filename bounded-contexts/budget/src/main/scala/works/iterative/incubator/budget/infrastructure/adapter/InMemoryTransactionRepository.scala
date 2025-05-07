package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.TransactionRepository
import java.time.LocalDate
import zio.*
import scala.collection.mutable

/** In-memory implementation of TransactionRepository for testing and development.
  *
  * Stores transactions in memory without persistence.
  *
  * Category: Repository Implementation
  * Layer: Infrastructure
  */
final case class InMemoryTransactionRepository() extends TransactionRepository:
  private val transactions = mutable.Map[TransactionId, Transaction]()

  override def save(transaction: Transaction): ZIO[Any, String, Unit] =
    ZIO.succeed {
      transactions.put(transaction.id, transaction)
      ()
    }

  override def saveAll(transactions: List[Transaction]): ZIO[Any, String, Unit] =
    ZIO.succeed {
      transactions.foreach(tx => this.transactions.put(tx.id, tx))
      ()
    }

  override def findById(id: TransactionId): ZIO[Any, String, Option[Transaction]] =
    ZIO.succeed(transactions.get(id))

  override def findByAccountAndDateRange(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, String, List[Transaction]] =
    ZIO.succeed {
      transactions.values.filter { tx =>
        tx.accountId == accountId &&
        !tx.date.isBefore(startDate) &&
        !tx.date.isAfter(endDate)
      }.toList.sortBy(_.date)
    }

  override def findByImportBatch(importBatchId: ImportBatchId): ZIO[Any, String, List[Transaction]] =
    ZIO.succeed {
      transactions.values.filter(_.importBatchId == importBatchId).toList
    }

  override def updateStatusByImportBatch(
      importBatchId: ImportBatchId,
      status: TransactionStatus
  ): ZIO[Any, String, Int] =
    ZIO.succeed {
      val toUpdate = transactions.values.filter(_.importBatchId == importBatchId).toList
      toUpdate.foreach { tx =>
        val updated = tx.updateStatus(status)
        transactions.put(tx.id, updated)
      }
      toUpdate.size
    }

  override def countByStatus(status: TransactionStatus): ZIO[Any, String, Int] =
    ZIO.succeed {
      transactions.values.count(_.status == status)
    }

/** Companion object for InMemoryTransactionRepository.
  */
object InMemoryTransactionRepository:
  /** Creates an in-memory implementation of TransactionRepository.
    *
    * @return
    *   A ZLayer that provides a TransactionRepository
    */
  val layer: ULayer[TransactionRepository] =
    ZLayer.succeed(InMemoryTransactionRepository())