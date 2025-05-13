package works.iterative.incubator.budget.infrastructure.persistence

import works.iterative.incubator.budget.domain.model.AccountId
import works.iterative.incubator.budget.infrastructure.adapter.fio.{
    FioAccount,
    FioAccountRepository
}
import com.augustnagro.magnum.*
import com.augustnagro.magnum.magzio.Transactor
import zio.*
import works.iterative.sqldb.PostgreSQLTransactor
import java.time.Instant

/** PostgreSQL implementation of the FioAccountRepository interface.
  *
  * This repository handles the persistence and retrieval of FioAccount entities using a PostgreSQL
  * database.
  */
class PostgreSQLFioAccountRepository(xa: Transactor) extends FioAccountRepository:
    import PostgreSQLFioAccountRepository.repo

    /** Saves a FioAccount to the repository.
      */
    override def save(account: FioAccount): ZIO[Any, String, Unit] =
        xa.transact {
            // Convert to DTO
            val dto = FioAccountMapper.toDTO(account)

            // Check if the record already exists
            val existingAccount = repo.findById(dto.id)

            // If it exists, update it; otherwise, insert it
            if existingAccount.isDefined then
                repo.update(dto)
            else
                repo.insert(dto)
        }.mapError(e => s"Failed to save FioAccount: ${e.getMessage}")

    /** Finds a FioAccount by its ID.
      */
    override def findById(id: Long): ZIO[Any, String, Option[FioAccount]] =
        xa.connect {
            repo.findById(id) match
                case Some(dto) =>
                    FioAccountMapper.toDomain(dto) match
                        case Right(account) => Some(account)
                        case Left(error) =>
                            throw new RuntimeException(s"Failed to map FioAccount: $error")
                case None => None
        }.mapError(e => s"Failed to find FioAccount: ${e.getMessage}")

    /** Finds a FioAccount by source account ID.
      */
    override def findBySourceAccountId(
        sourceAccountId: AccountId
    ): ZIO[Any, String, Option[FioAccount]] =
        xa.connect {
            val spec = Spec[FioAccountDTO]
                .where(sql"source_account_id = ${sourceAccountId.toString}")

            val dtoOption = repo.findAll(spec).headOption

            dtoOption match
                case Some(dto) =>
                    FioAccountMapper.toDomain(dto) match
                        case Right(account) => Some(account)
                        case Left(error) =>
                            throw new RuntimeException(s"Failed to map FioAccount: $error")
                case None => None
            end match
        }.mapError(e => s"Failed to find FioAccount by source account ID: ${e.getMessage}")

    /** Generates a new ID for a FioAccount.
      */
    override def nextId(): ZIO[Any, String, Long] =
        xa.transact {
            // Get all existing accounts
            val accounts = repo.findAll(Spec[FioAccountDTO])

            // Find the maximum ID currently in use, or use 0 if no accounts exist
            val maxId = if accounts.isEmpty then 0L else accounts.map(_.id).max

            // Return the next ID (max + 1)
            maxId + 1L
        }.mapError(e => s"Failed to generate next sequence number: ${e.getMessage}")
end PostgreSQLFioAccountRepository

object PostgreSQLFioAccountRepository:
    /** Magnum repository for FioAccount entities */
    val repo = Repo[FioAccountDTO, FioAccountDTO, Long]

    /** ZLayer that provides a PostgreSQLFioAccountRepository implementation requiring a
      * PostgreSQLTransactor as a dependency.
      */
    val layer: ZLayer[PostgreSQLTransactor, Nothing, FioAccountRepository] =
        ZLayer.fromFunction { (transactor: PostgreSQLTransactor) =>
            new PostgreSQLFioAccountRepository(transactor.transactor)
        }
end PostgreSQLFioAccountRepository
