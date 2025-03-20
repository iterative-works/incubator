package works.iterative.incubator
package transactions
package infrastructure

import zio.*
import service.TransactionRepository
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import java.time.LocalDate
import java.time.Instant

class PostgreSQLTransactionRepository(xa: Transactor) extends TransactionRepository:
    import PostgreSQLTransactionRepository.{transactionRepo, TransactionDTO}

    override def find(filter: TransactionQuery): UIO[Seq[Transaction]] =
        xa.transact:
            val spec = Spec[TransactionDTO]
                .where(
                    filter.id.map(tid =>
                        sql"source_account = ${tid.sourceAccount} AND source_bank = ${tid.sourceBank} AND transaction_id = ${tid.id}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.status.map(status => sql"status = ${status}").getOrElse(sql"")
                )
                .where(
                    filter.amount.map(amount => sql"amount = ${amount}").getOrElse(sql"")
                )
                .where(
                    filter.currency.map(currency => sql"currency = ${currency}").getOrElse(sql"")
                )
                .where(
                    filter.createdBefore.map(createdBefore =>
                        sql"created_at < ${java.sql.Timestamp.from(createdBefore)}"
                    ).getOrElse(sql"")
                )
                .where(
                    filter.createdAfter.map(createdAfter =>
                        sql"created_at > ${java.sql.Timestamp.from(createdAfter)}"
                    ).getOrElse(sql"")
                )
            transactionRepo.findAll(spec).map(_.toTransaction)
        .orDie
    end find

    override def save(key: TransactionId, value: Transaction): UIO[Unit] =
        xa.transact:
            if findById(key).isDefined then
                transactionRepo.update(PostgreSQLTransactionRepository.Transaction.fromModel(value))
            else
                transactionRepo.insert(PostgreSQLTransactionRepository.Transaction.fromModel(value))
        .orDie

    private def findById(tid: TransactionId)(using DbCon) =
        transactionRepo.findAll(
            Spec[TransactionDTO]
                .where(
                    sql"source_account = ${tid.sourceAccount} AND source_bank = ${tid.sourceBank} AND transaction_id = ${tid.id}"
                )
                .limit(1)
        ).headOption

    override def load(id: TransactionId): UIO[Option[Transaction]] =
        xa.connect:
            findById(id).map(_.toTransaction)
        .orDie
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

    @SqlName("transaction_status")
    @Table(PostgresDbType, SqlNameMapper.CamelToUpperSnakeCase)
    enum TransactionStatusDTO derives DbCodec:
        case Imported, Categorized, Submitted
    end TransactionStatusDTO

    @SqlName("transaction")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class TransactionDTO(
        // Source data from FIO
        sourceAccount: String,
        sourceBank: String,
        transactionId: String, // Unique ID from FIO (column_22)
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
        status: TransactionStatusDTO, // Imported, Categorized, Submitted

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
    ) derives DbCodec:
        def toTransaction: Transaction = this.into[Transaction]
            .withFieldComputed(
                _.id,
                t => TransactionId(t.sourceAccount, t.sourceBank, t.transactionId)
            )
            .transform
    end TransactionDTO

    object Transaction:
        def fromModel(model: Transaction): TransactionDTO =
            model.into[TransactionDTO]
                .withFieldComputed(_.sourceAccount, _.id.sourceAccount)
                .withFieldComputed(_.sourceBank, _.id.sourceBank)
                .withFieldComputed(_.transactionId, _.id.id)
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
