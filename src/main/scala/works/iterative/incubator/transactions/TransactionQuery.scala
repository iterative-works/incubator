package works.iterative.incubator.transactions

import java.time.LocalDateTime

case class TransactionQuery(
    id: Option[TransactionId] = None,
    status: Option[String] = None,
    amount: Option[Double] = None,
    currency: Option[String] = None,
    createdAfter: Option[LocalDateTime] = None,
    createdBefore: Option[LocalDateTime] = None
)
