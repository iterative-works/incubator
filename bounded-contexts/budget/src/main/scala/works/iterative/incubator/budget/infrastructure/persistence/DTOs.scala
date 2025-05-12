package works.iterative.incubator.budget.infrastructure.persistence

import java.time.{Instant, LocalDate}
import com.augustnagro.magnum.*
import java.sql.Timestamp

/** Data Transfer Objects (DTOs) for database persistence.
  *
  * These classes map directly to database tables and are used to translate between domain entities
  * and database records.
  */

given DbCodec[LocalDate] = DbCodec.SqlTimestampCodec.biMap(
    ts => if ts == null then null else ts.toLocalDateTime.toLocalDate(),
    ld => Timestamp.valueOf(ld.atStartOfDay())
)

given DbCodec[Instant] = DbCodec.SqlTimestampCodec.biMap(
    ts => if ts == null then null else ts.toInstant,
    i => Timestamp.from(i)
)

/** DTO for the transactions table */
@SqlName("transactions")
@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class TransactionDTO(
    id: String,
    sourceAccountId: String,
    transactionDate: LocalDate,
    amountValue: BigDecimal,
    amountCurrency: String,
    description: String,
    counterparty: Option[String],
    counterAccount: Option[String],
    reference: Option[String],
    importBatchId: String,
    status: String,
    createdAt: Instant,
    updatedAt: Instant
) derives DbCodec

/** DTO for the import_batches table */
@SqlName("import_batches")
@Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
case class ImportBatchDTO(
    id: String,
    accountId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    status: String,
    transactionCount: Int,
    errorMessage: Option[String],
    startTime: Instant,
    endTime: Option[Instant],
    createdAt: Instant,
    updatedAt: Instant
) derives DbCodec
