package works.iterative.incubator.fio.application.port

import zio.Task
import works.iterative.incubator.transactions.domain.model.Transaction

/** Port interface for interacting with the Transaction Management Context
  *
  * This port defines the interface that the Fio Bank Context needs from
  * the Transaction Management Context. It allows the Fio Bank Context to
  * save transactions without directly depending on the Transaction Management
  * Context's implementation details.
  *
  * Classification: Application Port
  */
trait TransactionPort:
    /** Save a transaction 
      *
      * @param transaction Transaction to save
      * @return Unit
      */
    def saveTransaction(transaction: Transaction): Task[Unit]
    
    /** Get source account ID by bank account identifier
      *
      * @param accountId Bank account identifier
      * @param bankId Bank identifier
      * @return Source account ID
      */
    def getSourceAccountId(accountId: String, bankId: String): Task[Long]