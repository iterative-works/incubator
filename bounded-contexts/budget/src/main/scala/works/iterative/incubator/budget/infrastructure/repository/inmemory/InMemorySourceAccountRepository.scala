package works.iterative.incubator.budget.infrastructure.repository.inmemory

import zio.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import works.iterative.incubator.budget.domain.repository.SourceAccountRepository
import works.iterative.incubator.budget.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.budget.domain.query.SourceAccountQuery

/** In-memory implementation of SourceAccountRepository for testing
  *
  * This repository holds source account data in memory, without persistence to a database.
  * It's useful for testing, development, and UI-first implementation.
  *
  * Classification: Infrastructure Repository Implementation (Test Double)
  */
class InMemorySourceAccountRepository extends SourceAccountRepository:
    private val idCounter = new AtomicLong(1)

    private val storage: Ref[Map[Long, SourceAccount]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[Long, SourceAccount])

    /** Find source accounts matching the given query
      */
    override def find(query: SourceAccountQuery): UIO[Seq[SourceAccount]] =
        storage.get.map { accounts =>
            accounts.values
                .filter { account =>
                    // Filter by ID if provided
                    query.id.forall(_ == account.id) &&
                    // Filter by account ID if provided
                    query.accountId.forall(id => account.accountId.contains(id)) &&
                    // Filter by bank ID if provided
                    query.bankId.forall(id => account.bankId.contains(id)) &&
                    // Filter by name if provided
                    query.name.forall(name => account.name.contains(name)) &&
                    // Filter by currency if provided
                    query.currency.forall(_ == account.currency) &&
                    // Filter by active flag if provided
                    query.active.forall(_ == account.active)
                    // Note: hasYnabAccount filter would be implemented here if we had that field
                }
                .toSeq
        }

    /** Save a source account
      */
    override def save(key: Long, value: SourceAccount): UIO[Unit] =
        storage.update(_ + (key -> value))

    /** Load a source account by ID
      */
    override def load(id: Long): UIO[Option[SourceAccount]] =
        storage.get.map(_.get(id))

    /** Create a new source account
      */
    override def create(command: CreateSourceAccount): UIO[Long] =
        for
            accounts <- storage.get
            // Check if account with same accountId and bankId already exists
            _ <- ZIO.when(
                accounts.values.exists(a =>
                    a.accountId == command.accountId && a.bankId == command.bankId
                )
            )(ZIO.fail(new RuntimeException("Source account with the same account ID and bank ID already exists")).orDie)
            id = idCounter.getAndIncrement()
            account = SourceAccount(
                id = id,
                accountId = command.accountId,
                bankId = command.bankId,
                name = command.name,
                currency = command.currency,
                active = command.active,
                lastSyncTime = None
            )
            _ <- storage.update(_ + (id -> account))
        yield id

    /** Find all active source accounts
      */
    def findActive(): UIO[Seq[SourceAccount]] =
        find(SourceAccountQuery(active = Some(true)))

    /** Update the last sync time for a source account
      */
    def updateLastSyncTime(id: Long, syncTime: Instant): UIO[Unit] =
        storage.update { accounts =>
            accounts.get(id) match
                case Some(account) =>
                    accounts + (id -> account.copy(lastSyncTime = Some(syncTime)))
                case None =>
                    accounts
        }

    /** Find source accounts that need to be synced
      *
      * @param cutoffTime
      *   Only return accounts that haven't been synced since this time
      * @return
      *   Active accounts that need syncing
      */
    def findAccountsNeedingSync(cutoffTime: Instant): UIO[Seq[SourceAccount]] =
        storage.get.map { accounts =>
            accounts.values
                .filter(account =>
                    account.active &&
                    (account.lastSyncTime.isEmpty || account.lastSyncTime.exists(_.isBefore(cutoffTime)))
                )
                .toSeq
        }

    /** Add a batch of source accounts (for testing)
      */
    def addBatch(accounts: Seq[SourceAccount]): UIO[Unit] = for
        _ <- ZIO.foreach(accounts)(account => storage.update(_ + (account.id -> account)))
        _ <- ZIO.succeed {
            // Update the ID counter to be higher than any existing ID
            val maxId = accounts.map(_.id).maxOption.getOrElse(0L)
            if maxId >= idCounter.get() then idCounter.set(maxId + 1)
        }
    yield ()

    /** Reset the repository (for testing)
      */
    def reset(): UIO[Unit] =
        storage.set(Map.empty) *> ZIO.succeed(idCounter.set(1))
end InMemorySourceAccountRepository

object InMemorySourceAccountRepository:
    /** Create a new in-memory repository
      */
    def make(): UIO[InMemorySourceAccountRepository] =
        ZIO.succeed(new InMemorySourceAccountRepository())

    /** ZIO layer for the repository
      */
    val layer: ULayer[SourceAccountRepository] =
        ZLayer.succeed(new InMemorySourceAccountRepository())

    /** ZIO layer with access to the concrete implementation
      */
    val testLayer: ULayer[InMemorySourceAccountRepository] =
        ZLayer.succeed(new InMemorySourceAccountRepository())
end InMemorySourceAccountRepository