package works.iterative.incubator.budget.domain.repository

import works.iterative.incubator.budget.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.budget.domain.query.SourceAccountQuery
import works.iterative.core.service.RepositoryWithCreate

/** Repository interface for source accounts
  *
  * Provides CRUD operations and creation operations for SourceAccount entities.
  *
  * Classification: Domain Repository Interface
  */
trait SourceAccountRepository
    extends RepositoryWithCreate[Long, SourceAccount, SourceAccountQuery, CreateSourceAccount]
