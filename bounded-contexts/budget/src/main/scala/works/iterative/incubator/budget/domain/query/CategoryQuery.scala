package works.iterative.incubator.budget.domain.query

/** Query parameters for filtering categories
  *
  * This class provides a flexible way to query categories based on various criteria.
  *
  * Classification: Domain Query Object
  */
case class CategoryQuery(
    id: Option[String] = None,
    name: Option[String] = None,
    parentId: Option[String] = None,
    active: Option[Boolean] = None,
    isTopLevel: Option[Boolean] = None
)
