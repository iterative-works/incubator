package works.iterative.incubator
package transactions
package infrastructure

import zio.*
import service.SourceAccountRepository
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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

    val layer: ZLayer[Scope, Throwable, SourceAccountRepository] =
        val dsLayer: ZLayer[Scope, Throwable, DataSource] = ZLayer.scoped:
            for
                _ <- ZIO.attempt(Class.forName("org.postgresql.Driver"))
                config <- ZIO.config[PostgreSQLConfig](PostgreSQLConfig.config)
                dataSource <- ZIO.attempt {
                    val conf = HikariConfig()
                    conf.setJdbcUrl(config.jdbcUrl)
                    conf.setUsername(config.username)
                    conf.setPassword(config.password)
                    conf.setMaximumPoolSize(10)
                    conf.setMinimumIdle(5)
                    conf.setConnectionTimeout(30000)
                    conf.setIdleTimeout(600000)
                    conf.setMaxLifetime(1800000)
                    conf.setInitializationFailTimeout(-1)
                    HikariDataSource(conf)
                }
            yield dataSource
        val tsLayer: ZLayer[Scope, Throwable, Transactor] =
            dsLayer.flatMap(env => Transactor.layer(env.get[DataSource]))

        tsLayer >>> ZLayer {
            for
                transactor <- ZIO.service[Transactor]
            yield PostgreSQLSourceAccountRepository(transactor)
        }
    end layer
end PostgreSQLSourceAccountRepository
