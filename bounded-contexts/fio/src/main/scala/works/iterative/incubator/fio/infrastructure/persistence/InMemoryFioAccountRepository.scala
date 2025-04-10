package works.iterative.incubator.fio.infrastructure.persistence

import zio.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import works.iterative.incubator.fio.domain.model.*

/** In-memory implementation of FioAccountRepository for testing
  *
  * Classification: Infrastructure Repository Implementation
  */
class InMemoryFioAccountRepository extends FioAccountRepository:
    private val idCounter = new AtomicLong(1)

    private val storage: Ref[Map[Long, FioAccount]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[Long, FioAccount])

    /** Create a new Fio Bank account
      */
    override def create(command: CreateFioAccount): Task[Long] =
        storage.get.flatMap { accounts =>
            // Check if account with this source ID already exists
            if accounts.values.exists(_.sourceAccountId == command.sourceAccountId) then
                ZIO.fail(new RuntimeException(
                    s"Fio account for source account ID ${command.sourceAccountId} already exists"
                ))
            else
                val id = idCounter.getAndIncrement()
                val account = FioAccount(
                    id = id,
                    sourceAccountId = command.sourceAccountId,
                    token = command.token,
                    lastSyncTime = None,
                    lastFetchedId = None
                )
                storage.update(_ + (id -> account)).as(id)
        }

    /** Get a Fio Bank account by ID
      */
    override def getById(id: Long): Task[Option[FioAccount]] =
        storage.get.map(_.get(id))

    /** Get a Fio Bank account by source account ID
      */
    override def getBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]] =
        storage.get.map(_.values.find(_.sourceAccountId == sourceAccountId))

    /** Get all Fio Bank accounts
      */
    override def getAll(): Task[List[FioAccount]] =
        storage.get.map(_.values.toList)

    /** Update a Fio Bank account
      */
    override def update(account: FioAccount): Task[Unit] =
        storage.update { accounts =>
            if accounts.contains(account.id) then
                accounts + (account.id -> account)
            else
                accounts
        }

    /** Delete a Fio Bank account
      */
    override def delete(id: Long): Task[Unit] =
        storage.update(_ - id)

    /** Update the last fetched transaction ID for an account
      */
    override def updateLastFetched(
        id: Long,
        lastFetchedId: Long,
        syncTime: Instant
    ): Task[Unit] =
        storage.update { accounts =>
            accounts.get(id) match
                case Some(account) =>
                    val updated = account.copy(
                        lastFetchedId = Some(lastFetchedId),
                        lastSyncTime = Some(syncTime)
                    )
                    accounts + (id -> updated)
                case None =>
                    accounts
        }

    /** Reset the repository (for testing)
      */
    def reset(): UIO[Unit] =
        storage.set(Map.empty) *> ZIO.succeed(idCounter.set(1))
end InMemoryFioAccountRepository

object InMemoryFioAccountRepository:
    /** Create a new in-memory repository
      */
    def make(): UIO[InMemoryFioAccountRepository] =
        ZIO.succeed(new InMemoryFioAccountRepository())

    /** ZIO layer for the repository
      */
    val layer: ULayer[FioAccountRepository] =
        ZLayer.succeed(new InMemoryFioAccountRepository())

    /** ZIO layer with access to the concrete implementation
      */
    val testLayer: ULayer[InMemoryFioAccountRepository] =
        ZLayer.succeed(new InMemoryFioAccountRepository())
end InMemoryFioAccountRepository
