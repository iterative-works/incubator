package works.iterative.incubator.transactions.infrastructure.persistence

import zio.*
import works.iterative.incubator.transactions.domain.repository.TransactionRepository
import works.iterative.incubator.transactions.domain.model.{Transaction, TransactionId}
import works.iterative.incubator.transactions.domain.query.TransactionQuery
import works.iterative.incubator.transactions.infrastructure.{PostgreSQLDataSource, PostgreSQLTransactor}
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import java.time.LocalDate
import java.time.Instant

/** PostgreSQL implementation of TransactionRepository
  *
  * This repository implements storage and retrieval of immutable Transaction events in a PostgreSQL
  * database.
  *
  * Classification: Infrastructure Repository Implementation
  */
class PostgreSQLTransactionRepository(xa: Transactor) extends TransactionRepository:
    import PostgreSQLTransactionRepository.{transactionRepo, TransactionDTO}

    override def find(filter: TransactionQuery): UIO[Seq[Transaction]] =
        xa.transact:
            val spec = Spec[TransactionDTO]
                // Handle the ID filter
                .where(
                    filter.id.map(tid =>
                        sql"source_account_id = ${tid.sourceAccountId} AND transaction_id = ${tid.transactionId}"
                    ).getOrElse(sql"")
                )
                // Handle direct sourceAccountId filter
                .where(
                    filter.sourceAccountId.map(id =>
                        sql"source_account_id = ${id}"
                    ).getOrElse(sql"")
                )
                // Handle date range filters
                .where(
                    filter.dateFrom.map(date =>
                        sql"date >= ${java.sql.Date.valueOf(date)}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.dateTo.map(date =>
                        sql"date <= ${java.sql.Date.valueOf(date)}"
                    ).getOrElse(sql"")
                )
                // Handle amount range filters
                .where(
                    filter.amountMin.map(amount =>
                        sql"amount >= ${amount}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.amountMax.map(amount =>
                        sql"amount <= ${amount}"
                    ).getOrElse(sql"")
                )
                // Handle other simple filters
                .where(
                    filter.currency.map(currency =>
                        sql"currency = ${currency}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.counterAccount.map(account =>
                        sql"counter_account = ${account}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.variableSymbol.map(symbol =>
                        sql"variable_symbol = ${symbol}"
                    ).getOrElse(sql"")
                )
                // Handle import time range filters
                .where(
                    filter.importedBefore.map(time =>
                        sql"imported_at < ${java.sql.Timestamp.from(time)}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.importedAfter.map(time =>
                        sql"imported_at > ${java.sql.Timestamp.from(time)}"
                    ).getOrElse(sql"")
                )

            // Convert DTOs to domain models
            transactionRepo.findAll(spec).map(_.toTransaction)
        .orDie
    end find

    override def save(key: TransactionId, value: Transaction): UIO[Unit] =
        xa.transact:
            val dto = PostgreSQLTransactionRepository.Transaction.fromModel(value)
            if findById(key).isDefined then
                // Transactions are immutable, so we should not update them
                // but we'll allow it in the implementation for now
                transactionRepo.update(dto)
            else
                transactionRepo.insert(dto)
            end if
        .orDie

    /** Find a transaction by its ID */
    private def findById(tid: TransactionId)(using DbCon) =
        transactionRepo.findAll(
            Spec[TransactionDTO]
                .where(
                    sql"source_account_id = ${tid.sourceAccountId} AND transaction_id = ${tid.transactionId}"
                )
                .limit(1)
        ).headOption

    override def load(id: TransactionId): UIO[Option[Transaction]] =
        xa.connect:
            findById(id).map(_.toTransaction)
        .orDie

    /** Find all transactions for a specific source account */
    def findBySourceAccount(sourceAccountId: Long): UIO[Seq[Transaction]] =
        find(TransactionQuery(sourceAccountId = Some(sourceAccountId)))
end PostgreSQLTransactionRepository

object PostgreSQLTransactionRepository:
    import io.scalaland.chimney.dsl.*

    // Use ZoneId instead of ZoneOffset
    val pragueZone = java.time.ZoneId.of("Europe/Prague")

    given DbCodec[LocalDate] = DbCodec.SqlDateCodec.biMap(
        d => d.toLocalDate,
        d => java.sql.Date.valueOf(d)
    )

    given DbCodec[Instant] = DbCodec.SqlTimestampCodec.biMap(
        i => i.toInstant,
        i => java.sql.Timestamp.from(i)
    )

    @SqlName("transaction")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class TransactionDTO(
        // Core identity
        sourceAccountId: Long,
        transactionId: String,

        // Transaction details
        date: LocalDate,
        amount: BigDecimal,
        currency: String,

        // Counterparty information
        counterAccount: Option[String],
        counterBankCode: Option[String],
        counterBankName: Option[String],

        // Additional transaction details
        variableSymbol: Option[String],
        constantSymbol: Option[String],
        specificSymbol: Option[String],
        userIdentification: Option[String],
        message: Option[String],
        transactionType: String,
        comment: Option[String],

        // When this transaction was imported
        importedAt: Instant
    ) derives DbCodec:
        def toTransaction: Transaction = this.into[Transaction]
            .withFieldComputed(
                _.id,
                dto => TransactionId(dto.sourceAccountId, dto.transactionId)
            )
            .transform
    end TransactionDTO

    object Transaction:
        def fromModel(model: Transaction): TransactionDTO =
            model.into[TransactionDTO]
                .withFieldComputed(_.sourceAccountId, _.id.sourceAccountId)
                .withFieldComputed(_.transactionId, _.id.transactionId)
                .transform
    end Transaction

    val transactionRepo = Repo[TransactionDTO, TransactionDTO, Null]

    val layer: ZLayer[PostgreSQLTransactor, Nothing, TransactionRepository] =
        ZLayer.fromFunction { (ts: PostgreSQLTransactor) =>
            PostgreSQLTransactionRepository(ts.transactor)
        }

    val fullLayer: ZLayer[Scope, Throwable, TransactionRepository] =
        PostgreSQLDataSource.managedLayer >>>
            PostgreSQLTransactor.managedLayer >>>
            layer
end PostgreSQLTransactionRepository
