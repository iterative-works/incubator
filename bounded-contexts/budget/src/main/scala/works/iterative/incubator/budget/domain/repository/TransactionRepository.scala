package works.iterative.incubator.budget.domain.repository

import works.iterative.core.service.Repository
import works.iterative.incubator.budget.domain.model.{Transaction, TransactionId}
import works.iterative.incubator.budget.domain.query.TransactionQuery

/** Repository interface for transactions
  *
  * Provides basic CRUD operations for Transaction entities.
  *
  * Classification: Domain Repository Interface
  */
trait TransactionRepository extends Repository[TransactionId, Transaction, TransactionQuery]
