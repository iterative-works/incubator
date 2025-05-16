package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.AccountId
import zio.*
import java.util.concurrent.atomic.AtomicLong

/** In-memory implementation of FioAccountRepository for testing and development.
  *
  * Stores FioAccount entities in memory without persistence. Uses ZIO Ref for thread-safe
  * concurrent access.
  *
  * Category: Repository Implementation Layer: Infrastructure
  */
final case class InMemoryFioAccountRepository(
    accountsRef: Ref[Map[Long, FioAccount]],
    idCounter: AtomicLong = new AtomicLong(1)
) extends FioAccountRepository:

    /** Saves a FioAccount. Uses atomic update to ensure thread-safety.
      *
      * @param account
      *   The FioAccount to save
      * @return
      *   A ZIO effect that completes with Unit or fails with an error
      */
    override def save(account: FioAccount): ZIO[Any, String, Unit] =
        accountsRef.update { accounts =>
            accounts + (account.id -> account)
        }

    /** Finds a FioAccount by ID.
      *
      * @param id
      *   The ID of the FioAccount to find
      * @return
      *   A ZIO effect that completes with the found account or None if not found
      */
    override def findById(id: Long): ZIO[Any, String, Option[FioAccount]] =
        accountsRef.get.map(_.get(id))

    /** Finds a FioAccount by source account ID.
      *
      * @param sourceAccountId
      *   The source account ID to look for
      * @return
      *   A ZIO effect that completes with the found account or None if not found
      */
    override def findBySourceAccountId(
        sourceAccountId: AccountId
    ): ZIO[Any, String, Option[FioAccount]] =
        accountsRef.get.map { accounts =>
            accounts.values.find(_.sourceAccountId == sourceAccountId)
        }

    /** Generates a new ID for a FioAccount.
      *
      * @return
      *   A ZIO effect that returns a new ID
      */
    override def nextId(): ZIO[Any, String, Long] =
        ZIO.succeed(idCounter.getAndIncrement())

    override def getAll(): ZIO[Any, String, List[FioAccount]] =
        accountsRef.get.map(_.values.toList)
end InMemoryFioAccountRepository

/** Companion object for InMemoryFioAccountRepository.
  */
object InMemoryFioAccountRepository:
    /** Creates an in-memory implementation of FioAccountRepository.
      *
      * Initializes a thread-safe Ref containing an empty Map for storing accounts.
      *
      * @return
      *   A ZLayer that provides a FioAccountRepository
      */
    val layer: ULayer[FioAccountRepository] =
        ZLayer.scoped {
            for
                accountsRef <- Ref.make(Map.empty[Long, FioAccount])
                repo = InMemoryFioAccountRepository(accountsRef)
            yield repo
        }
end InMemoryFioAccountRepository
