package works.iterative.incubator.budget.infrastructure.repository.inmemory

import zio.*
import works.iterative.incubator.budget.domain.repository.TransactionProcessingStateRepository
import works.iterative.incubator.budget.domain.model.{
    TransactionId,
    TransactionProcessingState,
    TransactionStatus
}
import works.iterative.incubator.budget.domain.query.TransactionProcessingStateQuery

/** In-memory implementation of TransactionProcessingStateRepository for testing
  *
  * This repository holds transaction processing state data in memory, without persistence to a
  * database. It's useful for testing, development, and UI-first implementation.
  *
  * Classification: Infrastructure Repository Implementation (Test Double)
  */
class InMemoryTransactionProcessingStateRepository extends TransactionProcessingStateRepository:
    private val storage: Ref[Map[TransactionId, TransactionProcessingState]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[TransactionId, TransactionProcessingState])

    /** Find processing states matching the given query
      */
    override def find(query: TransactionProcessingStateQuery)
        : UIO[Seq[TransactionProcessingState]] =
        storage.get.map { states =>
            states.values
                .filter { state =>
                    // Filter by transaction ID if provided
                    query.transactionId.forall(_ == state.transactionId) &&
                    // Filter by source account ID if provided
                    query.sourceAccountId.forall(_ == state.transactionId.sourceAccountId) &&
                    // Filter by status if provided
                    query.status.forall(_ == state.status) &&
                    // Filter by whether it has a YNAB ID
                    query.hasYnabId.forall(has =>
                        if has then state.ynabTransactionId.isDefined
                        else state.ynabTransactionId.isEmpty
                    ) &&
                    // Filter by processed time if provided
                    query.processedAfter.forall(after =>
                        state.processedAt.exists(_.isAfter(after))
                    ) &&
                    query.processedBefore.forall(before =>
                        state.processedAt.exists(_.isBefore(before))
                    ) &&
                    // Filter by submitted time if provided
                    query.submittedAfter.forall(after =>
                        state.submittedAt.exists(_.isAfter(after))
                    ) &&
                    query.submittedBefore.forall(before =>
                        state.submittedAt.exists(_.isBefore(before))
                    )
                }
                .toSeq
        }

    /** Save a processing state
      */
    override def save(key: TransactionId, value: TransactionProcessingState): UIO[Unit] =
        storage.update(_ + (key -> value))

    /** Load a processing state by transaction ID
      */
    override def load(id: TransactionId): UIO[Option[TransactionProcessingState]] =
        storage.get.map(_.get(id))

    /** Find all ready-to-submit processing states (categorized but not submitted)
      */
    override def findReadyToSubmit(): UIO[Seq[TransactionProcessingState]] =
        storage.get.map { states =>
            states.values
                .filter(state =>
                    state.status == TransactionStatus.Categorized &&
                        state.ynabTransactionId.isEmpty &&
                        state.isReadyForSubmission
                )
                .toSeq
        }

    /** Add a batch of processing states (for testing)
      */
    def addBatch(states: Seq[TransactionProcessingState]): UIO[Unit] =
        storage.update(existing => existing ++ states.map(state => state.transactionId -> state))

    /** Reset the repository (for testing)
      */
    def reset(): UIO[Unit] =
        storage.set(Map.empty)
end InMemoryTransactionProcessingStateRepository

object InMemoryTransactionProcessingStateRepository:
    /** Create a new in-memory repository
      */
    def make(): UIO[InMemoryTransactionProcessingStateRepository] =
        ZIO.succeed(new InMemoryTransactionProcessingStateRepository())

    /** ZIO layer for the repository
      */
    val layer: ULayer[TransactionProcessingStateRepository] =
        ZLayer.succeed(new InMemoryTransactionProcessingStateRepository())

    /** ZIO layer with access to the concrete implementation
      */
    val testLayer: ULayer[InMemoryTransactionProcessingStateRepository] =
        ZLayer.succeed(new InMemoryTransactionProcessingStateRepository())
end InMemoryTransactionProcessingStateRepository
