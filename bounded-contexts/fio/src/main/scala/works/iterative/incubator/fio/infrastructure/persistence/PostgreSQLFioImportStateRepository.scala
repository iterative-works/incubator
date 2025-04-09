package works.iterative.incubator.fio.infrastructure.persistence

import zio.*
import java.time.Instant
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.transactions.infrastructure.config.PostgreSQLTransactor

// PostgreSQL implementation is a dummy for now since we need doobie and
// cannot compile with it currently

/**
 * PostgreSQL implementation of FioImportStateRepository (simplified for now)
 * 
 * This is a simplified temporary version until we fix the doobie dependencies
 *
 * Classification: Infrastructure Repository Implementation
 */
class PostgreSQLFioImportStateRepository extends FioImportStateRepository:
    // State storage using ZIO Ref (temporary in-memory storage)
    private val storage: Ref[Map[Long, FioImportState]] =
        Unsafe.unsafe { implicit unsafe =>
            Ref.unsafe.make(Map.empty[Long, FioImportState])
        }
    
    override def getImportState(sourceAccountId: Long): Task[Option[FioImportState]] =
        storage.get.map(_.get(sourceAccountId))
        
    override def updateImportState(state: FioImportState): Task[Unit] =
        storage.update(_ + (state.sourceAccountId -> state))

object PostgreSQLFioImportStateRepository:
    val layer: ZLayer[Any, Nothing, FioImportStateRepository] =
        ZLayer.succeed(new PostgreSQLFioImportStateRepository())