package works.iterative.incubator.transactions

import java.time.Instant

case class TransactionQuery(
    id: Option[TransactionId] = None,
    status: Option[TransactionStatus] = None,
    amount: Option[Double] = None,
    currency: Option[String] = None,
    createdAfter: Option[Instant] = None,
    createdBefore: Option[Instant] = None
)
