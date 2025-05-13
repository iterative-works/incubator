package works.iterative.incubator.budget.infrastructure.persistence

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.TransactionRepository
import java.time.{Instant, LocalDate}
import zio.*
import com.augustnagro.magnum.*
import com.augustnagro.magnum.magzio.Transactor
import works.iterative.sqldb.PostgreSQLTransactor

/** PostgreSQL implementation of the TransactionRepository interface.
  *
  * This repository handles the persistence and retrieval of Transaction entities using a PostgreSQL
  * database.
  */
class PostgreSQLTransactionRepository(xa: Transactor) extends TransactionRepository:
    import PostgreSQLTransactionRepository.repo

    /** Saves a transaction to the repository.
      */
    override def save(transaction: Transaction): ZIO[Any, String, Unit] =
        xa.transact {
            repo.insert(TransactionMapper.toDTO(transaction))
        }.mapError(e => s"Failed to save transaction: ${e.getMessage}")

    /** Saves multiple transactions in a batch.
      */
    override def saveAll(transactions: List[Transaction]): ZIO[Any, String, Unit] =
        if transactions.isEmpty then
            ZIO.unit
        else
            xa.transact {
                repo.insertAll(transactions.map(TransactionMapper.toDTO))
            }.mapError(e => s"Failed to save transactions in batch: ${e.getMessage}")

    /** Finds a transaction by its ID.
      */
    override def findById(id: TransactionId): ZIO[Any, String, Option[Transaction]] =
        xa.connect {
            repo.findById(id.toString) match
                case Some(dto) =>
                    TransactionMapper.toDomain(dto) match
                        case Right(transaction) => Some(transaction)
                        case Left(error) =>
                            throw new RuntimeException(s"Failed to map transaction: $error")
                case None => None
        }.mapError(e => s"Failed to find transaction: ${e.getMessage}")

    /** Finds all transactions for a given account and date range.
      */
    override def findByAccountAndDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, List[Transaction]] =
        xa.connect {
            val spec = Spec[TransactionDTO]
                .where(sql"source_account_id = ${accountId.toString}")
                .where(sql"transaction_date >= ${startDate}")
                .where(sql"transaction_date <= ${endDate}")
                .orderBy("transaction_date", SortOrder.Desc)

            val dtos = repo.findAll(spec)
            val results = dtos.flatMap { dto =>
                TransactionMapper.toDomain(dto) match
                    case Right(transaction) => Some(transaction)
                    case Left(error) =>
                        throw new RuntimeException(s"Failed to map transaction: $error")
            }

            results.toList
        }.mapError(e => s"Failed to find transactions by account and date range: ${e.getMessage}")

    /** Finds all transactions associated with a specific import batch.
      */
    override def findByImportBatch(importBatchId: ImportBatchId)
        : ZIO[Any, String, List[Transaction]] =
        xa.connect {
            val spec = Spec[TransactionDTO]
                .where(sql"import_batch_id = ${importBatchId.toString}")
                .orderBy("transaction_date", SortOrder.Desc)

            val dtos = repo.findAll(spec)
            val results = dtos.flatMap { dto =>
                TransactionMapper.toDomain(dto) match
                    case Right(transaction) => Some(transaction)
                    case Left(error) =>
                        throw new RuntimeException(s"Failed to map transaction: $error")
            }

            results.toList
        }.mapError(e => s"Failed to find transactions by import batch: ${e.getMessage}")

    /** Updates the status of all transactions in a batch.
      */
    override def updateStatusByImportBatch(
        importBatchId: ImportBatchId,
        status: TransactionStatus
    ): ZIO[Any, String, Int] =
        xa.transact {
            // First find all affected transactions
            val spec = Spec[TransactionDTO].where(sql"import_batch_id = ${importBatchId.toString}")
            val transactions = repo.findAll(spec)

            // Update each transaction with the new status
            val now = Instant.now()
            val updatedTransactions = transactions.map { dto =>
                dto.copy(
                    status = status.toString,
                    updatedAt = now
                )
            }

            // Batch update the transactions
            if updatedTransactions.nonEmpty then
                val _ = repo.updateAll(updatedTransactions)
                updatedTransactions.size // Use size as batch update count
            else
                0
            end if
        }.mapError(e => s"Failed to update transaction statuses: ${e.getMessage}")

    /** Counts transactions by status.
      */
    override def countByStatus(status: TransactionStatus): ZIO[Any, String, Int] =
        xa.connect {
            val spec = Spec[TransactionDTO].where(sql"status = ${status.toString}")
            repo.findAll(spec).size
        }.mapError(e => s"Failed to count transactions by status: ${e.getMessage}")
end PostgreSQLTransactionRepository

object PostgreSQLTransactionRepository:
    /** Magnum repository for Transaction entities
      */
    val repo = Repo[TransactionDTO, TransactionDTO, String]

    /** ZLayer that provides a PostgreSQLTransactionRepository implementation requiring a
      * PostgreSQLTransactor as a dependency.
      */
    val layer: ZLayer[PostgreSQLTransactor, Nothing, TransactionRepository] =
        ZLayer.fromFunction { (transactor: PostgreSQLTransactor) =>
            new PostgreSQLTransactionRepository(transactor.transactor)
        }
end PostgreSQLTransactionRepository
