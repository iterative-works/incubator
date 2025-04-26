package works.iterative.incubator.transactions.domain.repository

import zio.UIO
import works.iterative.incubator.transactions.domain.model.{
    TransactionId,
    TransactionProcessingState,
    TransactionStatus
}
import works.iterative.incubator.transactions.domain.query.TransactionProcessingStateQuery

/** Repository interface for transaction processing states
  *
  * This repository handles the mutable processing state of transactions.
  *
  * Classification: Domain Repository Interface
  */
trait TransactionProcessingStateRepository:
    /** Find processing states matching the given query
      *
      * @param query
      *   The query criteria
      * @return
      *   A sequence of processing states matching the criteria
      */
    def find(query: TransactionProcessingStateQuery): UIO[Seq[TransactionProcessingState]]

    /** Save a processing state
      *
      * @param key
      *   The transaction ID this state belongs to
      * @param value
      *   The processing state to save
      * @return
      *   Unit effect indicating success
      */
    def save(key: TransactionId, value: TransactionProcessingState): UIO[Unit]

    /** Load a processing state by transaction ID
      *
      * @param id
      *   The transaction ID to load the state for
      * @return
      *   An optional processing state if found
      */
    def load(id: TransactionId): UIO[Option[TransactionProcessingState]]

    /** Find all processing states for transactions from a specific source account
      *
      * @param sourceAccountId
      *   The source account ID
      * @return
      *   A sequence of processing states for transactions from that account
      */
    def findBySourceAccount(sourceAccountId: Long): UIO[Seq[TransactionProcessingState]] =
        find(TransactionProcessingStateQuery(sourceAccountId = Some(sourceAccountId)))

    /** Find all processing states with a specific status
      *
      * @param status
      *   The status to filter by
      * @return
      *   A sequence of processing states with that status
      */
    def findByStatus(status: TransactionStatus): UIO[Seq[TransactionProcessingState]] =
        find(TransactionProcessingStateQuery(status = Some(status)))

    /** Find all ready-to-submit processing states (categorized but not submitted)
      *
      * @return
      *   A sequence of processing states ready for YNAB submission
      */
    def findReadyToSubmit(): UIO[Seq[TransactionProcessingState]]
end TransactionProcessingStateRepository
