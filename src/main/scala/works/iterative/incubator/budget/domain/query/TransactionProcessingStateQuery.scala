package works.iterative.incubator.budget.domain.query

import java.time.Instant
import works.iterative.incubator.budget.domain.model.{TransactionId, TransactionStatus}

/** Query parameters for filtering transaction processing states
  *
  * This class provides a flexible way to query transaction processing states based on various
  * criteria.
  *
  * Classification: Domain Query Object
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