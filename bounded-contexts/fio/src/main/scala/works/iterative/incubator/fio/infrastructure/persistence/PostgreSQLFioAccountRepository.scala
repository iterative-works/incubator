package works.iterative.incubator.fio.infrastructure.persistence

import zio.*
import java.time.Instant
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.transactions.infrastructure.config.PostgreSQLTransactor
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import io.scalaland.chimney.dsl.*
import works.iterative.incubator.infrastructure.DbCodecs.given
import works.iterative.incubator.transactions.infrastructure.config.PostgreSQLDataSource

/** PostgreSQL implementation of FioAccountRepository
  *
  * This repository is responsible for storing and retrieving Fio Bank account configurations,
  * including API tokens and synchronization states.
  *
  * Classification: Infrastructure Repository Implementation
  */
class PostgreSQLFioAccountRepository(xa: Transactor) extends FioAccountRepository:
    import PostgreSQLFioAccountRepository.{fioAccountRepo, FioAccountDTO, CreateFioAccountDTO}

    /** Create a new Fio Bank account
      */
    override def create(command: CreateFioAccount): Task[Long] =
        xa.transact {
            // Check if an account with this source_account_id already exists
            val spec =
                Spec[FioAccountDTO].where(sql"sourceAccountId === ${command.sourceAccountId}")

            fioAccountRepo.findAll(spec).headOption match
                case Some(_) =>
                    throw new RuntimeException(
                        s"Fio account for source account ID ${command.sourceAccountId} already exists"
                    )
                case None =>
                    val dto = CreateFioAccountDTO.fromModel(command)
                    fioAccountRepo.insertReturning(dto).id
            end match
        }.orDie

    /** Get a Fio Bank account by ID
      */
    override def getById(id: Long): Task[Option[FioAccount]] =
        xa.connect {
            fioAccountRepo.findById(id).map(_.toModel)
        }.orDie

    /** Get a Fio Bank account by source account ID
      */
    override def getBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]] =
        xa.connect {
            val spec = Spec[FioAccountDTO].where(sql"sourceAccountId = $sourceAccountId")
            fioAccountRepo.findAll(spec).headOption.map(_.toModel)
        }.orDie

    /** Get all Fio Bank accounts
      */
    override def getAll(): Task[List[FioAccount]] =
        xa.connect {
            fioAccountRepo.findAll.map(_.toModel).toList
        }.orDie

    /** Update a Fio Bank account
      */
    override def update(account: FioAccount): Task[Unit] =
        xa.transact {
            val dto = FioAccountDTO.fromModel(account)
            fioAccountRepo.update(dto)
        }.orDie

    /** Delete a Fio Bank account
      */
    override def delete(id: Long): Task[Unit] =
        xa.transact {
            // First check if the account exists
            fioAccountRepo.findById(id) match
                case Some(_) => fioAccountRepo.deleteById(id)
                case None    => ()
        }.orDie

    /** Update the last fetched transaction ID for an account
      */
    override def updateLastFetched(
        id: Long,
        lastFetchedId: Long,
        syncTime: Instant
    ): Task[Unit] =
        xa.transact {
            // First retrieve the account
            fioAccountRepo.findById(id) match
                case Some(account) =>
                    val updated = account.copy(
                        lastFetchedId = Some(lastFetchedId),
                        lastSyncTime = Some(syncTime)
                    )
                    fioAccountRepo.update(updated)
                case None => ()
        }.orDie
end PostgreSQLFioAccountRepository

object PostgreSQLFioAccountRepository:
    /** DTO for Fio account table
      */
    @SqlName("fio_account")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class FioAccountDTO(
        id: Long,
        sourceAccountId: Long,
        token: String,
        lastSyncTime: Option[Instant],
        lastFetchedId: Option[Long]
    ) derives DbCodec:
        def toModel: FioAccount = this.into[FioAccount].transform
    end FioAccountDTO

    object FioAccountDTO:
        def fromModel(model: FioAccount): FioAccountDTO =
            model.into[FioAccountDTO].transform
    end FioAccountDTO

    /** DTO for creating new Fio accounts
      */
    @SqlName("fio_account")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class CreateFioAccountDTO(
        sourceAccountId: Long,
        token: String,
        lastSyncTime: Option[Instant] = None,
        lastFetchedId: Option[Long] = None
    ) derives DbCodec

    object CreateFioAccountDTO:
        def fromModel(model: CreateFioAccount): CreateFioAccountDTO =
            model.into[CreateFioAccountDTO]
                .withFieldConst(_.lastSyncTime, None)
                .withFieldConst(_.lastFetchedId, None)
                .transform
    end CreateFioAccountDTO

    // Repository definition using Magnum's Repo pattern
    val fioAccountRepo = Repo[CreateFioAccountDTO, FioAccountDTO, Long]

    /** ZIO layer for the repository
      */
    val layer: ZLayer[PostgreSQLTransactor, Nothing, FioAccountRepository] =
        ZLayer.fromFunction { (ts: PostgreSQLTransactor) =>
            new PostgreSQLFioAccountRepository(ts.transactor)
        }

    /** Full layer with all dependencies
      */
    val fullLayer: ZLayer[Scope, Throwable, FioAccountRepository] =
        PostgreSQLDataSource.managedLayer >>> PostgreSQLTransactor.managedLayer >>> layer
end PostgreSQLFioAccountRepository
