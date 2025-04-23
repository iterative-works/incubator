package works.iterative.incubator.budget.domain.repository

import works.iterative.core.service.Repository
import works.iterative.incubator.budget.domain.model.Category
import works.iterative.incubator.budget.domain.query.CategoryQuery

/** Repository interface for categories
  *
  * Provides operations for Category entities.
  *
  * Classification: Domain Repository Interface
  */
trait CategoryRepository extends Repository[String, Category, CategoryQuery]