package works.iterative.incubator.transactions.infrastructure.persistence

import zio.*
import works.iterative.incubator.transactions.domain.repository.TransactionProcessingStateRepository
import works.iterative.incubator.transactions.domain.model.{TransactionId, TransactionProcessingState, TransactionStatus}
import works.iterative.incubator.transactions.domain.query.TransactionProcessingStateQuery
import works.iterative.incubator.transactions.infrastructure.DbCodecs.given
import works.iterative.incubator.transactions.infrastructure.{PostgreSQLDataSource, PostgreSQLTransactor}
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import java.time.Instant

/** PostgreSQL implementation of TransactionProcessingStateRepository
  *
  * This repository handles the mutable processing state of transactions, including status,
  * categorization, and YNAB integration information.
  *
  * Classification: Infrastructure Repository Implementation
  */
class PostgreSQLTransactionProcessingStateRepository(xa: Transactor)
    extends TransactionProcessingStateRepository:
    import PostgreSQLTransactionProcessingStateRepository.{
        processingStateRepo,
        ProcessingStateDTO,
        TransactionStatusDTO
    }

    override def find(query: TransactionProcessingStateQuery)
        : UIO[Seq[TransactionProcessingState]] =
        xa.transact:
            val spec = Spec[ProcessingStateDTO]
                // Handle transaction ID filter
                .where(
                    query.transactionId.map(tid =>
                        sql"source_account_id = ${tid.sourceAccountId} AND transaction_id = ${tid.transactionId}"
                    ).getOrElse(sql"")
                )
                // Handle source account filter
                .where(
                    query.sourceAccountId.map(id =>
                        sql"source_account_id = ${id}"
                    ).getOrElse(sql"")
                )
                // Handle status filter
                .where(
                    query.status.map(status =>
                        sql"status = ${TransactionStatusDTO.fromDomain(status)}"
                    ).getOrElse(sql"")
                )
                // Handle YNAB ID presence filter
                .where(
                    query.hasYnabId.map(hasId =>
                        if hasId then sql"ynab_transaction_id IS NOT NULL"
                        else sql"ynab_transaction_id IS NULL"
                    ).getOrElse(sql"")
                )
                // Handle processing time range filters
                .where(
                    query.processedAfter.map(time =>
                        sql"processed_at > ${java.sql.Timestamp.from(time)}"
                    ).getOrElse(sql"")
                )
                .where(
                    query.processedBefore.map(time =>
                        sql"processed_at < ${java.sql.Timestamp.from(time)}"
                    ).getOrElse(sql"")
                )
                // Handle submission time range filters
                .where(
                    query.submittedAfter.map(time =>
                        sql"submitted_at > ${java.sql.Timestamp.from(time)}"
                    ).getOrElse(sql"")
                )
                .where(
                    query.submittedBefore.map(time =>
                        sql"submitted_at < ${java.sql.Timestamp.from(time)}"
                    ).getOrElse(sql"")
                )

            // Convert DTOs to domain models
            processingStateRepo.findAll(spec).map(_.toDomain)
        .orDie
    end find

    override def save(key: TransactionId, value: TransactionProcessingState): UIO[Unit] =
        xa.transact:
            val dto = ProcessingStateDTO.fromDomain(value)
            if findById(key).isDefined then
                processingStateRepo.update(dto)
            else
                processingStateRepo.insert(dto)
        .orDie

    /** Find a processing state by transaction ID */
    private def findById(tid: TransactionId)(using DbCon) =
        processingStateRepo.findAll(
            Spec[ProcessingStateDTO]
                .where(
                    sql"source_account_id = ${tid.sourceAccountId} AND transaction_id = ${tid.transactionId}"
                )
                .limit(1)
        ).headOption

    override def load(id: TransactionId): UIO[Option[TransactionProcessingState]] =
        xa.connect:
            findById(id).map(_.toDomain)
        .orDie

    // Using default implementations from trait for findBySourceAccount and findByStatus

    override def findReadyToSubmit(): UIO[Seq[TransactionProcessingState]] =
        xa.connect:
            // Using Spec to build the query instead of raw SQL
            val spec = Spec[ProcessingStateDTO]
                .where(sql"status = ${TransactionStatusDTO.Categorized}")
                .where(sql"ynab_account_id IS NOT NULL")
                .where(sql"(suggested_payee_name IS NOT NULL OR override_payee_name IS NOT NULL)")
                .where(sql"ynab_transaction_id IS NULL")

            // Execute the query and convert results
            processingStateRepo.findAll(spec).map(_.toDomain)
        .orDie
end PostgreSQLTransactionProcessingStateRepository

object PostgreSQLTransactionProcessingStateRepository:

    @SqlName("transaction_status")
    @Table(PostgresDbType, SqlNameMapper.CamelToUpperSnakeCase)
    enum TransactionStatusDTO derives DbCodec:
        case Imported, Categorized, Submitted

        def toDomain: TransactionStatus = this match
            case Imported    => TransactionStatus.Imported
            case Categorized => TransactionStatus.Categorized
            case Submitted   => TransactionStatus.Submitted
    end TransactionStatusDTO

    object TransactionStatusDTO:
        def fromDomain(status: TransactionStatus): TransactionStatusDTO = status match
            case TransactionStatus.Imported    => Imported
            case TransactionStatus.Categorized => Categorized
            case TransactionStatus.Submitted   => Submitted
    end TransactionStatusDTO

    @SqlName("transaction_processing_state")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class ProcessingStateDTO(
        // Reference to transaction
        sourceAccountId: Long,
        transactionId: String,

        // Processing state
        status: TransactionStatusDTO,

        // AI computed/processed fields for YNAB
        suggestedPayeeName: Option[String],
        suggestedCategory: Option[String],
        suggestedMemo: Option[String],

        // User overrides
        overridePayeeName: Option[String],
        overrideCategory: Option[String],
        overrideMemo: Option[String],

        // YNAB integration fields
        ynabTransactionId: Option[String],
        ynabAccountId: Option[String],

        // Processing timestamps
        processedAt: Option[Instant],
        submittedAt: Option[Instant]
    ) derives DbCodec:
        def toDomain: TransactionProcessingState =
            TransactionProcessingState(
                transactionId = TransactionId(sourceAccountId, transactionId),
                status = status.toDomain,
                suggestedPayeeName = suggestedPayeeName,
                suggestedCategory = suggestedCategory,
                suggestedMemo = suggestedMemo,
                overridePayeeName = overridePayeeName,
                overrideCategory = overrideCategory,
                overrideMemo = overrideMemo,
                ynabTransactionId = ynabTransactionId,
                ynabAccountId = ynabAccountId,
                processedAt = processedAt,
                submittedAt = submittedAt
            )
    end ProcessingStateDTO

    object ProcessingStateDTO:
        def fromDomain(state: TransactionProcessingState): ProcessingStateDTO =
            ProcessingStateDTO(
                sourceAccountId = state.transactionId.sourceAccountId,
                transactionId = state.transactionId.transactionId,
                status = TransactionStatusDTO.fromDomain(state.status),
                suggestedPayeeName = state.suggestedPayeeName,
                suggestedCategory = state.suggestedCategory,
                suggestedMemo = state.suggestedMemo,
                overridePayeeName = state.overridePayeeName,
                overrideCategory = state.overrideCategory,
                overrideMemo = state.overrideMemo,
                ynabTransactionId = state.ynabTransactionId,
                ynabAccountId = state.ynabAccountId,
                processedAt = state.processedAt,
                submittedAt = state.submittedAt
            )
    end ProcessingStateDTO

    // Repository for transaction processing states
    val processingStateRepo = Repo[ProcessingStateDTO, ProcessingStateDTO, Null]

    // ZIO layer for the repository
    val layer: ZLayer[PostgreSQLTransactor, Nothing, TransactionProcessingStateRepository] =
        ZLayer.fromFunction { (ts: PostgreSQLTransactor) =>
            PostgreSQLTransactionProcessingStateRepository(ts.transactor)
        }

    // Full ZIO layer including data source and transactor
    val fullLayer: ZLayer[Scope, Throwable, TransactionProcessingStateRepository] =
        PostgreSQLDataSource.managedLayer >>>
            PostgreSQLTransactor.managedLayer >>>
            layer
end PostgreSQLTransactionProcessingStateRepository