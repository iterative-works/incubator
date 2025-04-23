package works.iterative.incubator.budget.domain.query

/** Query parameters for filtering source accounts
  *
  * This class provides a flexible way to query source accounts based on various criteria.
  *
  * Classification: Domain Query Object
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