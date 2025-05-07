package works.iterative.incubator.budget.domain.service

import works.iterative.incubator.budget.domain.model.{AccountId, Transaction}
import java.time.LocalDate
import zio.*

/** Service interface for importing transactions from Fio Bank API.
  *
  * Category: Service Interface
  * Layer: Domain
  */
trait FioBankService:
  /** Fetches transactions from Fio Bank API for a specified date range.
    *
    * @param accountId
    *   The ID of the source account to fetch transactions for
    * @param startDate
    *   The start date of the range
    * @param endDate
    *   The end date of the range
    * @return
    *   A ZIO effect that returns a list of transactions or a service-specific error
    */
  def fetchTransactions(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, Throwable, List[Transaction]]

/** Companion object providing ZIO accessor methods.
  */
object FioBankService:
  /** Accesses the service to fetch transactions.
    *
    * @param accountId
    *   The ID of the source account to fetch transactions for
    * @param startDate
    *   The start date of the range
    * @param endDate
    *   The end date of the range
    * @return
    *   A ZIO effect that requires FioBankService and returns a list of transactions or a
    *   service-specific error
    */
  def fetchTransactions(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[FioBankService, Throwable, List[Transaction]] =
    ZIO.serviceWithZIO(_.fetchTransactions(accountId, startDate, endDate))