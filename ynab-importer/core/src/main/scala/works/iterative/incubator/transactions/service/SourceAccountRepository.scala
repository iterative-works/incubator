package works.iterative.incubator.transactions
package service

import works.iterative.core.service.Repository

trait SourceAccountRepository extends Repository[Long, SourceAccount, SourceAccountQuery]
