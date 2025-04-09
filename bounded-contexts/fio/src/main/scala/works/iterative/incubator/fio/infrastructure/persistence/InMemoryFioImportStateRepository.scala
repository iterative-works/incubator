package works.iterative.incubator.fio.infrastructure.persistence

import zio.*
import works.iterative.incubator.fio.domain.model.*

/**
 * In-memory implementation of FioImportStateRepository
 * Useful for testing and development
 *
 * Classification: Infrastructure Repository Implementation
 */
class InMemoryFioImportStateRepository extends FioImportStateRepository:
    // State storage using ZIO Ref
    private val storage: Ref[Map[Long, FioImportState]] =
        Unsafe.unsafe { implicit unsafe =>
            Ref.unsafe.make(Map.empty[Long, FioImportState])
        }
    
    override def getImportState(sourceAccountId: Long): Task[Option[FioImportState]] =
        storage.get.map(_.get(sourceAccountId))
        
    override def updateImportState(state: FioImportState): Task[Unit] =
        storage.update(_ + (state.sourceAccountId -> state))

object InMemoryFioImportStateRepository:
    val layer: ZLayer[Any, Nothing, FioImportStateRepository] =
        ZLayer.succeed(new InMemoryFioImportStateRepository())