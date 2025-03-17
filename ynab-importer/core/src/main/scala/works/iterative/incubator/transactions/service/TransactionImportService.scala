package works.iterative.incubator.transactions
package service

import zio.*
import java.time.LocalDate

trait TransactionImportService:
    def importTransactions(from: LocalDate, to: LocalDate): Task[Int]
    def importNewTransactions(lastId: Option[Long]): Task[Int]
