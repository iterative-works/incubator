package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.TransactionRepository
import java.time.LocalDate
import zio.*

/** In-memory implementation of TransactionRepository for testing and development.
  *
  * Stores transactions in memory without persistence.
  * Uses ZIO Ref for thread-safe concurrent access.
  *
  * Category: Repository Implementation
  * Layer: Infrastructure
  */
final case class InMemoryTransactionRepository(
    txRef: Ref[Map[TransactionId, Transaction]]
) extends TransactionRepository:

  /** Saves a transaction.
    * Uses atomic update to ensure thread-safety.
    *
    * @param transaction The transaction to save
    * @return A ZIO effect that completes with Unit or fails with an error
    */
  override def save(transaction: Transaction): ZIO[Any, String, Unit] =
    txRef.update { transactions =>
      transactions + (transaction.id -> transaction)
    }

  /** Saves multiple transactions in a single atomic operation.
    * This ensures that either all transactions are saved or none are (atomicity).
    *
    * @param transactions The list of transactions to save
    * @return A ZIO effect that completes with Unit or fails with an error
    */
  override def saveAll(transactions: List[Transaction]): ZIO[Any, String, Unit] =
    txRef.update { txMap =>
      txMap ++ transactions.map(tx => tx.id -> tx)
    }

  /** Finds a transaction by ID.
    *
    * @param id The ID of the transaction to find
    * @return A ZIO effect that completes with the found transaction or None if not found
    */
  override def findById(id: TransactionId): ZIO[Any, String, Option[Transaction]] =
    txRef.get.map(_.get(id))

  /** Finds transactions for an account within a date range.
    *
    * @param accountId The account ID to find transactions for
    * @param startDate The start date of the range
    * @param endDate The end date of the range
    * @return A ZIO effect that completes with a list of found transactions
    */
  override def findByAccountAndDateRange(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, String, List[Transaction]] =
    txRef.get.map { transactions =>
      transactions.values
        .filter { tx =>
          tx.id.sourceAccount == accountId &&
          !tx.date.isBefore(startDate) &&
          !tx.date.isAfter(endDate)
        }
        .toList
        .sortBy(_.date)
    }

  /** Finds transactions for a specific import batch.
    *
    * @param importBatchId The import batch ID to find transactions for
    * @return A ZIO effect that completes with a list of found transactions
    */
  override def findByImportBatch(importBatchId: ImportBatchId): ZIO[Any, String, List[Transaction]] =
    txRef.get.map { transactions =>
      transactions.values
        .filter(_.importBatchId == importBatchId)
        .toList
    }

  /** Updates the status of all transactions in a specific import batch.
    * This operation is atomic - either all transactions are updated or none are.
    *
    * @param importBatchId The import batch ID to update transactions for
    * @param status The new status to set
    * @return A ZIO effect that completes with the number of transactions updated
    */
  override def updateStatusByImportBatch(
      importBatchId: ImportBatchId,
      status: TransactionStatus
  ): ZIO[Any, String, Int] =
    for {
      // Get current state and identify transactions to update
      currentState <- txRef.get
      toUpdate = currentState.values.filter(_.importBatchId == importBatchId).toList

      // Update the transactions map atomically
      _ <- txRef.update { transactions =>
        transactions ++ toUpdate.map { tx =>
          val updated = tx.updateStatus(status)
          updated.id -> updated
        }
      }
    } yield toUpdate.size

  /** Counts transactions with a specific status.
    *
    * @param status The status to count transactions for
    * @return A ZIO effect that completes with the count
    */
  override def countByStatus(status: TransactionStatus): ZIO[Any, String, Int] =
    txRef.get.map { transactions =>
      transactions.values.count(_.status == status)
    }

/** Companion object for InMemoryTransactionRepository.
  */
object InMemoryTransactionRepository:
  /** Creates an in-memory implementation of TransactionRepository.
    *
    * Initializes a thread-safe Ref containing an empty Map for storing transactions.
    *
    * @return
    *   A ZLayer that provides a TransactionRepository
    */
  val layer: ULayer[TransactionRepository] =
    ZLayer.scoped {
      for {
        txRef <- Ref.make(Map.empty[TransactionId, Transaction])
        repo = InMemoryTransactionRepository(txRef)
      } yield repo
    }