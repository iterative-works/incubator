package works.iterative.incubator.transactions.application.service

import zio.*
import java.time.LocalDate

/** Service responsible for importing transactions from external sources
  *
  * This interface defines the contract for services that import transactions from external sources
  * like bank APIs.
  *
  * Classification: Application Service
  */
trait TransactionImportService:
    /** Import transactions from a specific date range
      *
      * @param from
      *   Start date of the range
      * @param to
      *   End date of the range
      * @return
      *   Number of transactions imported
      */
    def importTransactions(from: LocalDate, to: LocalDate): Task[Int]

    /** Import new transactions since a specific transaction ID
      *
      * @param lastId
      *   Optional ID of the last imported transaction
      * @return
      *   Number of transactions imported
      */
    def importNewTransactions(lastId: Option[Long]): Task[Int]
end TransactionImportService
