package works.iterative.incubator.budget.infrastructure.persistence

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import java.time.{Instant, LocalDate}
import zio.*
import com.augustnagro.magnum.*
import com.augustnagro.magnum.magzio.Transactor
import works.iterative.sqldb.PostgreSQLTransactor

/** PostgreSQL implementation of the ImportBatchRepository interface.
  *
  * This repository handles the persistence and retrieval of ImportBatch entities using a PostgreSQL
  * database.
  */
class PostgreSQLImportBatchRepository(xa: Transactor) extends ImportBatchRepository:
    import PostgreSQLImportBatchRepository.repo

    /** Saves an import batch to the repository.
      */
    override def save(importBatch: ImportBatch): ZIO[Any, String, Unit] =
        xa.transact {
            repo.insert(ImportBatchMapper.toDTO(importBatch))
        }.mapError(e => s"Failed to save import batch: ${e.getMessage}")

    /** Finds an import batch by its ID.
      */
    override def findById(id: ImportBatchId): ZIO[Any, String, Option[ImportBatch]] =
        xa.connect {
            repo.findById(id.toString) match
                case Some(dto) =>
                    ImportBatchMapper.toDomain(dto) match
                        case Right(batch) => Some(batch)
                        case Left(error) =>
                            throw new RuntimeException(s"Failed to map import batch: $error")
                case None => None
        }.mapError(e => s"Failed to find import batch: ${e.getMessage}")

    /** Finds all import batches for a specific account.
      */
    override def findByAccountId(accountId: AccountId): ZIO[Any, String, List[ImportBatch]] =
        xa.connect {
            val spec = Spec[ImportBatchDTO]
                .where(sql"account_id = ${accountId.toString}")
                .orderBy("start_time", SortOrder.Desc)

            val dtos = repo.findAll(spec)
            val results = dtos.flatMap { dto =>
                ImportBatchMapper.toDomain(dto) match
                    case Right(batch) => Some(batch)
                    case Left(error) =>
                        throw new RuntimeException(s"Failed to map import batch: $error")
            }

            results.toList
        }.mapError(e => s"Failed to find import batches by account: ${e.getMessage}")

    /** Finds the most recent import batch for an account.
      */
    override def findMostRecentByAccountId(accountId: AccountId)
        : ZIO[Any, String, Option[ImportBatch]] =
        xa.connect {
            val spec = Spec[ImportBatchDTO]
                .where(sql"account_id = ${accountId.toString}")
                .orderBy("start_time", SortOrder.Desc)
                .limit(1)

            val batchOption = repo.findAll(spec).headOption

            batchOption match
                case Some(dto) =>
                    ImportBatchMapper.toDomain(dto) match
                        case Right(batch) => Some(batch)
                        case Left(error) =>
                            throw new RuntimeException(s"Failed to map import batch: $error")
                case None => None
            end match
        }.mapError(e => s"Failed to find most recent import batch: ${e.getMessage}")

    /** Finds all import batches with a specific status.
      */
    override def findByStatus(status: ImportStatus): ZIO[Any, String, List[ImportBatch]] =
        xa.connect {
            val spec = Spec[ImportBatchDTO]
                .where(sql"status = ${status.toString}")
                .orderBy("start_time", SortOrder.Desc)

            val dtos = repo.findAll(spec)
            val results = dtos.flatMap { dto =>
                ImportBatchMapper.toDomain(dto) match
                    case Right(batch) => Some(batch)
                    case Left(error) =>
                        throw new RuntimeException(s"Failed to map import batch: $error")
            }

            results.toList
        }.mapError(e => s"Failed to find import batches by status: ${e.getMessage}")

    /** Finds import batches for a specific date range.
      */
    override def findByDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, List[ImportBatch]] =
        xa.connect {
            val spec = Spec[ImportBatchDTO]
                .where(sql"account_id = ${accountId.toString}")
                .where(sql"(start_date <= ${endDate} AND end_date >= ${startDate})")
                .orderBy("start_time", SortOrder.Desc)

            val dtos = repo.findAll(spec)
            val results = dtos.flatMap { dto =>
                ImportBatchMapper.toDomain(dto) match
                    case Right(batch) => Some(batch)
                    case Left(error) =>
                        throw new RuntimeException(s"Failed to map import batch: $error")
            }

            results.toList
        }.mapError(e => s"Failed to find import batches by date range: ${e.getMessage}")

    /** Generates the next sequence number for import batches for a given account.
      */
    override def nextSequenceNumber(accountId: AccountId): ZIO[Any, String, Long] =
        xa.transact {
            // Find the highest sequence number for this account
            val accountPrefix = accountId.toString.take(8)

            // SQL for batch IDs in the format 'accountprefix-123'
            val batchesForAccount = repo.findAll(
                Spec[ImportBatchDTO].where(sql"id LIKE ${accountPrefix + "-%"}")
            )

            if batchesForAccount.isEmpty then
                1L // Start with sequence number 1 if no previous batches
            else
                // Extract sequence numbers from batch IDs
                val seqNums = batchesForAccount.flatMap { dto =>
                    val parts = dto.id.split("-", 2)
                    if parts.length == 2 then
                        try Some(parts(1).toLong)
                        catch
                            case _: NumberFormatException => None
                    else None
                    end if
                }

                // Return the next sequence number (max + 1)
                if seqNums.isEmpty then 1L else seqNums.max + 1
            end if
        }.mapError(e => s"Failed to generate next sequence number: ${e.getMessage}")
end PostgreSQLImportBatchRepository

object PostgreSQLImportBatchRepository:
    /** Magnum repository for ImportBatch entities
      */
    val repo = Repo[ImportBatchDTO, ImportBatchDTO, String]

    /** ZLayer that provides a PostgreSQLImportBatchRepository implementation requiring a
      * PostgreSQLTransactor as a dependency.
      */
    val layer: ZLayer[PostgreSQLTransactor, Nothing, ImportBatchRepository] =
        ZLayer.fromFunction { (transactor: PostgreSQLTransactor) =>
            new PostgreSQLImportBatchRepository(transactor.transactor)
        }
end PostgreSQLImportBatchRepository
