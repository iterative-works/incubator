package works.iterative.incubator.transactions

import java.time.Instant

/** Query parameters for filtering transaction processing states
  *
  * This class provides a flexible way to query transaction processing states based on various
  * criteria.
  */
case class TransactionProcessingStateQuery(
    transactionId: Option[TransactionId] = None,
    sourceAccountId: Option[Long] = None,
    status: Option[TransactionStatus] = None,
    hasYnabId: Option[Boolean] = None,
    processedAfter: Option[Instant] = None,
    processedBefore: Option[Instant] = None,
    submittedAfter: Option[Instant] = None,
    submittedBefore: Option[Instant] = None
)
