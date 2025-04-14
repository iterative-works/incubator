package works.iterative.incubator.fio.infrastructure.persistence

import zio.*
import java.time.Instant
import works.iterative.incubator.fio.domain.model.*
import works.iterative.sqldb.{PostgreSQLDataSource, PostgreSQLTransactor}
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import io.scalaland.chimney.dsl.*
import works.iterative.incubator.infrastructure.DbCodecs.given

/** PostgreSQL implementation of FioImportStateRepository Stores import state in a database table
  * for persistence
  *
  * Classification: Infrastructure Repository Implementation
  */
class PostgreSQLFioImportStateRepository(xa: Transactor) extends FioImportStateRepository:
    import PostgreSQLFioImportStateRepository.{importStateRepo, FioImportStateDTO}

    override def getImportState(sourceAccountId: Long): Task[Option[FioImportState]] =
        xa.connect {
            importStateRepo.findAll(
                Spec[FioImportStateDTO]
                    .where(sql"source_account_id = $sourceAccountId")
                    .limit(1)
            ).headOption.map(_.toDomain)
        }.orDie

    override def updateImportState(state: FioImportState): Task[Unit] =
        xa.transact {
            val dto = FioImportStateDTO.fromDomain(state)

            // Check if exists
            val exists = importStateRepo.findAll(
                Spec[FioImportStateDTO]
                    .where(sql"source_account_id = ${state.sourceAccountId}")
                    .limit(1)
            ).nonEmpty

            if exists then importStateRepo.update(dto)
            else importStateRepo.insert(dto)
        }.orDie
end PostgreSQLFioImportStateRepository

object PostgreSQLFioImportStateRepository:
    // DTO for FioImportState
    @SqlName("fio_import_state")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class FioImportStateDTO(
        sourceAccountId: Long,
        lastTransactionId: Option[Long],
        lastImportTimestamp: Instant
    ) derives DbCodec:
        def toDomain: FioImportState = this.into[FioImportState].transform
    end FioImportStateDTO

    object FioImportStateDTO:
        def fromDomain(domain: FioImportState): FioImportStateDTO =
            domain.into[FioImportStateDTO].transform

    // Repository definition
    val importStateRepo = Repo[FioImportStateDTO, FioImportStateDTO, Null]

    val layer: ZLayer[PostgreSQLTransactor, Throwable, FioImportStateRepository] =
        ZLayer {
            for
                xa <- ZIO.service[PostgreSQLTransactor].map(_.transactor)
            yield PostgreSQLFioImportStateRepository(xa)
        }

    val fullLayer: ZLayer[Scope, Throwable, FioImportStateRepository] =
        PostgreSQLDataSource.managedLayer >>>
            PostgreSQLTransactor.managedLayer >>>
            layer
end PostgreSQLFioImportStateRepository
