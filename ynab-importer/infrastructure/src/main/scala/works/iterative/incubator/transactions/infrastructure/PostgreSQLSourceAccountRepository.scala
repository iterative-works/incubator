package works.iterative.incubator
package transactions
package infrastructure

import zio.*
import service.SourceAccountRepository
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*

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
            sourceAccountRepo.findAll(spec).map(_.toModel)
        .orDie
    end find

    override def save(key: Long, value: SourceAccount): UIO[Unit] =
        xa.transact:
            if sourceAccountRepo.existsById(key) then
                sourceAccountRepo.update(SourceAccountDTO(value.id, value.accountId, value.bankId))
            else
                sourceAccountRepo.insert(SourceAccountDTO(value.id, value.accountId, value.bankId))
        .orDie

    override def load(id: Long): UIO[Option[SourceAccount]] =
        xa.connect:
            sourceAccountRepo.findById(id).map(dto =>
                SourceAccount(dto.id, dto.accountId, dto.bankId)
            )
        .orDie
end PostgreSQLSourceAccountRepository

object PostgreSQLSourceAccountRepository:
    import io.scalaland.chimney.dsl.*

    @SqlName("source_account")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class SourceAccountDTO(
        id: Long,
        accountId: String,
        bankId: String
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
