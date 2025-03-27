package works.iterative.incubator.transactions
package service

import works.iterative.core.service.{Repository, CreateRepository}

trait SourceAccountRepository extends Repository[Long, SourceAccount, SourceAccountQuery]
    with CreateRepository[Long, CreateSourceAccount]
