package works.iterative.incubator.transactions.infrastructure.persistence

import zio.*
import works.iterative.incubator.transactions.domain.repository.SourceAccountRepository
import works.iterative.incubator.transactions.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery
import works.iterative.incubator.infrastructure.DbCodecs.given
import works.iterative.sqldb.{PostgreSQLDataSource, PostgreSQLTransactor}
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import java.time.Instant

/** PostgreSQL implementation of SourceAccountRepository
  *
  * This repository manages source account information in a PostgreSQL database.
  *
  * Classification: Infrastructure Repository Implementation
  */
class PostgreSQLSourceAccountRepository(xa: Transactor) extends SourceAccountRepository:
    import PostgreSQLSourceAccountRepository.{
        sourceAccountRepo,
        SourceAccountDTO,
        CreateSourceAccountDTO
    }

    override def find(filter: SourceAccountQuery): UIO[Seq[SourceAccount]] =
        xa.transact:
            val spec = Spec[SourceAccountDTO]
                .where(
                    filter.id.map(id => sql"id = ${id}").getOrElse(sql"")
                )
                .where(
                    filter.accountId.map(accountId => sql"account_id = ${accountId}").getOrElse(
                        sql""
                    )
                )
                .where(
                    filter.bankId.map(bankId => sql"bank_id = ${bankId}").getOrElse(sql"")
                )
                // Add new filters for enhanced fields
                .where(
                    filter.name.map(name => sql"name LIKE ${s"%$name%"}").getOrElse(sql"")
                )
                .where(
                    filter.currency.map(currency => sql"currency = ${currency}").getOrElse(sql"")
                )
                .where(
                    filter.active.map(active => sql"active = ${active}").getOrElse(sql"")
                )
            sourceAccountRepo.findAll(spec).map(_.toModel)
        .orDie
    end find

    override def save(key: Long, value: SourceAccount): UIO[Unit] =
        xa.transact:
            val dto = SourceAccountDTO.fromModel(value)
            if sourceAccountRepo.existsById(key) then
                sourceAccountRepo.update(dto)
            else
                // Convert to CreateSourceAccountDTO for insertion
                val createDto = CreateSourceAccountDTO.fromModel(
                    CreateSourceAccount(
                        accountId = value.accountId,
                        bankId = value.bankId,
                        name = value.name,
                        currency = value.currency,
                        active = value.active
                    )
                )
                sourceAccountRepo.insert(createDto)
            end if
        .orDie

    override def load(id: Long): UIO[Option[SourceAccount]] =
        xa.connect:
            sourceAccountRepo.findById(id).map(_.toModel)
        .orDie

    override def create(value: CreateSourceAccount): UIO[Long] =
        xa.transact:
            val dto = CreateSourceAccountDTO.fromModel(value)
            sourceAccountRepo.insertReturning(dto).id
        .orDie

    /** Find all active source accounts */
    def findActive(): UIO[Seq[SourceAccount]] =
        find(SourceAccountQuery(active = Some(true)))

    /** Update the last sync time for a source account */
    def updateLastSyncTime(id: Long, syncTime: Instant): UIO[Unit] =
        // Load the account, modify it, then save it through the standard save method
        for
            accountOpt <- load(id)
            _ <- ZIO.foreach(accountOpt)(account =>
                save(id, account.copy(lastSyncTime = Some(syncTime)))
            )
        yield ()

    /** Find source accounts that need to be synced
      *
      * @param cutoffTime
      *   Only return accounts that haven't been synced since this time
      * @return
      *   Active accounts that need syncing
      */
    def findAccountsNeedingSync(cutoffTime: Instant): UIO[Seq[SourceAccount]] =
        // Using custom query built with Spec
        xa.connect:
            val spec = Spec[SourceAccountDTO]
                .where(sql"active = true")
                .where(
                    sql"last_sync_time IS NULL OR last_sync_time < ${java.sql.Timestamp.from(cutoffTime)}"
                )
            sourceAccountRepo.findAll(spec).map(_.toModel)
        .orDie
end PostgreSQLSourceAccountRepository

object PostgreSQLSourceAccountRepository:
    import io.scalaland.chimney.dsl.*

    @SqlName("source_account")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class SourceAccountDTO(
        id: Long,
        accountId: String,
        bankId: String,
        name: String,
        currency: String,
        active: Boolean = true,
        lastSyncTime: Option[Instant] = None
    ) derives DbCodec:
        def toModel: SourceAccount = this.into[SourceAccount].transform
    end SourceAccountDTO

    object SourceAccountDTO:
        def fromModel(model: SourceAccount): SourceAccountDTO =
            model.into[SourceAccountDTO].transform
    end SourceAccountDTO

    /** DTO for creating new source accounts
      *
      * Used with Magnum's insertReturning to generate a proper ID
      */
    @SqlName("source_account")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class CreateSourceAccountDTO(
        accountId: String,
        bankId: String,
        name: String,
        currency: String,
        active: Boolean = true,
        lastSyncTime: Option[Instant] = None
    ) derives DbCodec

    object CreateSourceAccountDTO:
        def fromModel(model: CreateSourceAccount): CreateSourceAccountDTO =
            model.into[CreateSourceAccountDTO]
                .withFieldConst(_.lastSyncTime, None)
                .transform
    end CreateSourceAccountDTO

    // Updated repository definition with create pattern support
    val sourceAccountRepo = Repo[CreateSourceAccountDTO, SourceAccountDTO, Long]

    val layer: ZLayer[PostgreSQLTransactor, Nothing, SourceAccountRepository] =
        ZLayer.fromFunction { (ts: PostgreSQLTransactor) =>
            PostgreSQLSourceAccountRepository(ts.transactor)
        }

    val fullLayer: ZLayer[Scope, Throwable, SourceAccountRepository] =
        PostgreSQLDataSource.managedLayer >>>
            PostgreSQLTransactor.managedLayer >>>
            layer
end PostgreSQLSourceAccountRepository
