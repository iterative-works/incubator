package works.iterative.incubator.fio.application.service

import zio.Task
import java.time.LocalDate
import works.iterative.incubator.transactions.domain.model.Transaction

/** Service interface for importing transactions from Fio Bank API
  *
  * This service is responsible for fetching transactions from Fio Bank API
  * and converting them to the application's domain model.
  *
  * Classification: Application Service
  */
trait FioImportService:
    /** Import transactions from Fio Bank API for a given date range
      *
      * @param from Start date for transaction import (inclusive)
      * @param to End date for transaction import (inclusive)
      * @return Number of transactions imported
      */
    def importTransactions(from: LocalDate, to: LocalDate): Task[Int]
    
    /** Import new transactions from Fio Bank API since the last import
      *
      * This method uses Fio Bank's "last" endpoint which returns all transactions
      * that have not been fetched yet.
      *
      * @return Number of transactions imported
      */
    def importNewTransactions(): Task[Int]
    
    /** Get the available Fio Bank accounts
      *
      * @return List of source account IDs that have Fio Bank configuration
      */
    def getFioSourceAccounts(): Task[List[Long]]