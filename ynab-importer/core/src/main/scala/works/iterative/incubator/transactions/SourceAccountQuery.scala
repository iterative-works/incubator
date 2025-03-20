package works.iterative.incubator.transactions

case class SourceAccountQuery(
    id: Option[Long] = None,
    accountId: Option[String] = None,
    bankId: Option[String] = None
)
