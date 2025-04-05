package works.iterative.incubator.transactions.domain.repository

import works.iterative.core.service.{Repository, CreateRepository}
import works.iterative.incubator.transactions.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery

/** Repository interface for source accounts
  *
  * Provides CRUD operations and creation operations for SourceAccount entities.
  *
  * Classification: Domain Repository Interface
  */
trait SourceAccountRepository extends Repository[Long, SourceAccount, SourceAccountQuery]
    with CreateRepository[Long, CreateSourceAccount]
