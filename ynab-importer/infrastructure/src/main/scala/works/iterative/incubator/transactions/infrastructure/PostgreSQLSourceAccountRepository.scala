package works.iterative.incubator
package transactions
package infrastructure

import zio.*
import service.SourceAccountRepository
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import java.time.Instant
import DbCodecs.given

/** PostgreSQL implementation of SourceAccountRepository
  *
  * This repository manages source account information in a PostgreSQL database.
  */
class PostgreSQLSourceAccountRepository(xa: Transactor) extends SourceAccountRepository:
    import PostgreSQLSourceAccountRepository.{sourceAccountRepo, SourceAccountDTO}

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
                sourceAccountRepo.insert(dto)
        .orDie

    override def load(id: Long): UIO[Option[SourceAccount]] =
        xa.connect:
            sourceAccountRepo.findById(id).map(_.toModel)
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
        ynabAccountId: Option[String] = None,
        active: Boolean = true,
        lastSyncTime: Option[Instant] = None
    ) derives DbCodec:
        def toModel: SourceAccount = this.into[SourceAccount].transform
    end SourceAccountDTO

    object SourceAccountDTO:
        def fromModel(model: SourceAccount): SourceAccountDTO =
            model.into[SourceAccountDTO].transform
    end SourceAccountDTO

    val sourceAccountRepo = Repo[SourceAccountDTO, SourceAccountDTO, Long]

    val layer: ZLayer[PostgreSQLTransactor, Nothing, SourceAccountRepository] =
        ZLayer.fromFunction { (ts: PostgreSQLTransactor) =>
            PostgreSQLSourceAccountRepository(ts.transactor)
        }

    val fullLayer: ZLayer[Scope, Throwable, SourceAccountRepository] =
        PostgreSQLDataSource.managedLayer >>>
            PostgreSQLTransactor.managedLayer >>>
            layer
end PostgreSQLSourceAccountRepository
