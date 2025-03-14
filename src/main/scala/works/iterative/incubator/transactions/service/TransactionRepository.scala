package works.iterative.incubator.transactions
package service

import works.iterative.core.service.Repository

trait TransactionRepository extends Repository[TransactionId, Transaction, TransactionQuery]
