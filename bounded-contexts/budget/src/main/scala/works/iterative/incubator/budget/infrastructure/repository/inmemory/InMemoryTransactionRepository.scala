package works.iterative.incubator.budget.infrastructure.repository.inmemory

import zio.*
import works.iterative.incubator.budget.domain.repository.TransactionRepository
import works.iterative.incubator.budget.domain.model.{Transaction, TransactionId}
import works.iterative.incubator.budget.domain.query.TransactionQuery

/** In-memory implementation of TransactionRepository for testing
  *
  * This repository holds transaction data in memory, without persistence to a database. It's useful
  * for testing, development, and UI-first implementation.
  *
  * Classification: Infrastructure Repository Implementation (Test Double)
  */
class InMemoryTransactionRepository extends TransactionRepository:
    private val storage: Ref[Map[TransactionId, Transaction]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[TransactionId, Transaction])

    /** Find transactions matching the given query
      */
    override def find(query: TransactionQuery): UIO[Seq[Transaction]] =
        storage.get.map { transactions =>
            transactions.values
                .filter { tx =>
                    // Filter by ID if provided
                    query.id.forall(_ == tx.id) &&
                    // Filter by source account ID if provided
                    query.sourceAccountId.forall(_ == tx.id.sourceAccountId) &&
                    // Filter by date range if provided
                    query.dateFrom.forall(from => !tx.date.isBefore(from)) &&
                    query.dateTo.forall(to => !tx.date.isAfter(to)) &&
                    // Filter by amount range if provided
                    query.amountMin.forall(min => tx.amount >= min) &&
                    query.amountMax.forall(max => tx.amount <= max) &&
                    // Filter by currency if provided
                    query.currency.forall(_ == tx.currency) &&
                    // Filter by counter account if provided
                    query.counterAccount.forall(ca => tx.counterAccount.exists(_.contains(ca))) &&
                    // Filter by variable symbol if provided
                    query.variableSymbol.forall(vs => tx.variableSymbol.exists(_.contains(vs))) &&
                    // Filter by imported time if provided
                    query.importedAfter.forall(after => tx.importedAt.isAfter(after)) &&
                    query.importedBefore.forall(before => tx.importedAt.isBefore(before))
                }
                .toSeq
        }

    /** Save a transaction
      */
    override def save(key: TransactionId, value: Transaction): UIO[Unit] =
        storage.update(_ + (key -> value))

    /** Load a transaction by ID
      */
    override def load(id: TransactionId): UIO[Option[Transaction]] =
        storage.get.map(_.get(id))

    /** Add a batch of transactions (for testing)
      */
    def addBatch(transactions: Seq[Transaction]): UIO[Unit] =
        storage.update(existing => existing ++ transactions.map(tx => tx.id -> tx))

    /** Reset the repository (for testing)
      */
    def reset(): UIO[Unit] =
        storage.set(Map.empty)
end InMemoryTransactionRepository

object InMemoryTransactionRepository:
    /** Create a new in-memory repository
      */
    def make(): UIO[InMemoryTransactionRepository] =
        ZIO.succeed(new InMemoryTransactionRepository())

    /** ZIO layer for the repository
      */
    val layer: ULayer[TransactionRepository] =
        ZLayer.succeed(new InMemoryTransactionRepository())

    /** ZIO layer with access to the concrete implementation
      */
    val testLayer: ULayer[InMemoryTransactionRepository] =
        ZLayer.succeed(new InMemoryTransactionRepository())
end InMemoryTransactionRepository
