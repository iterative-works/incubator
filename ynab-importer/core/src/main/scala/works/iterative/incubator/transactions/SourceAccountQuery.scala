package works.iterative.incubator.transactions

/** Query parameters for filtering source accounts
  *
  * This class provides a flexible way to query source accounts based on various criteria.
  */
case class SourceAccountQuery(
    id: Option[Long] = None,
    accountId: Option[String] = None,
    bankId: Option[String] = None,
    name: Option[String] = None,
    currency: Option[String] = None,
    hasYnabAccount: Option[Boolean] = None,
    active: Option[Boolean] = None
)
