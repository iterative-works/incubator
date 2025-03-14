package works.iterative.incubator
package transactions
package infrastructure

import zio.*
import service.TransactionRepository
import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import java.time.LocalDate
import java.time.Instant

class PostgreSQLTransactionRepository(xa: Transactor) extends TransactionRepository:
    import PostgreSQLTransactionRepository.transactionRepo

    override def find(filter: TransactionQuery): UIO[Seq[Transaction]] =
        xa.transact:
            transactionRepo.findAll.map(_.toTransaction)
        .orDie

    override def save(key: TransactionId, value: Transaction): UIO[Unit] = ???
    override def load(id: TransactionId): UIO[Option[Transaction]] = ???
    override def loadAll(ids: Seq[TransactionId]): UIO[Seq[Transaction]] = ???
end PostgreSQLTransactionRepository

object PostgreSQLTransactionRepository:
    import io.scalaland.chimney.dsl.*

    // Use ZoneId instead of ZoneOffset
    val pragueZone = java.time.ZoneId.of("Europe/Prague")

    given DbCodec[LocalDate] = DbCodec.OffsetDateTimeCodec.biMap(
        d => d.toLocalDate,
        d => d.atStartOfDay().atZone(pragueZone).toOffsetDateTime()
    )

    given DbCodec[Instant] = DbCodec.OffsetDateTimeCodec.biMap(
        i => i.toInstant,
        i => i.atZone(pragueZone).toOffsetDateTime()
    )

    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class Transaction(
        // Source data from FIO
        id: TransactionId, // Unique ID from FIO (column_22)
        date: java.time.LocalDate, // Transaction date
        amount: BigDecimal, // Transaction amount
        currency: String, // Currency code (e.g., CZK)
        counterAccount: Option[String], // Counter account number
        counterBankCode: Option[String], // Counter bank code
        counterBankName: Option[String], // Name of the counter bank
        variableSymbol: Option[String], // Variable symbol
        constantSymbol: Option[String], // Constant symbol
        specificSymbol: Option[String], // Specific symbol
        userIdentification: Option[String], // User identification
        message: Option[String], // Message for recipient
        transactionType: String, // Transaction type
        comment: Option[String], // Comment

        // Processing state
        status: TransactionStatus, // Imported, Categorized, Submitted

        // AI computed/processed fields for YNAB
        suggestedPayeeName: Option[String], // AI suggested payee name
        suggestedCategory: Option[String], // AI suggested category
        suggestedMemo: Option[String], // AI cleaned/processed memo

        // User overrides (if user wants to adjust AI suggestions)
        overridePayeeName: Option[String], // User override for payee
        overrideCategory: Option[String], // User override for category
        overrideMemo: Option[String], // User override for memo

        // YNAB integration fields
        ynabTransactionId: Option[String], // ID assigned by YNAB after submission
        ynabAccountId: Option[String], // YNAB account ID where transaction was submitted

        // Metadata
        importedAt: java.time.Instant, // When this transaction was imported
        processedAt: Option[java.time.Instant], // When AI processed this
        submittedAt: Option[java.time.Instant] // When submitted to YNAB
    ):
        def toTransaction: transactions.Transaction = this.transformInto[transactions.Transaction]
    end Transaction

    val transactionRepo = Repo[Transaction, Transaction, TransactionId]

    val layer: ZLayer[Scope, Throwable, TransactionRepository] =
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
            yield PostgreSQLTransactionRepository(transactor)
        }
    end layer
end PostgreSQLTransactionRepository
