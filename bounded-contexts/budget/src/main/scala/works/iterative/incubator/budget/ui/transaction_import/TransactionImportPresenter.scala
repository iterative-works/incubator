package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import works.iterative.incubator.budget.domain.model.AccountId
import java.time.LocalDate
import zio.*

/** Service interface for handling transaction imports from Fio Bank. This service provides methods
  * for the import workflow:
  *   1. Initial view model for the import page 2. Account and date validation 3. Transaction import
  *      execution 4. Import status tracking
  *
  * Category: Presenter
  * Layer: UI/Presentation
  */
trait TransactionImportPresenter:
    /** Get the initial view model for the import page.
      *
      * @return
      *   A ZIO effect that returns the ImportPageViewModel or an error string
      */
    def getImportViewModel(): ZIO[Any, String, ImportPageViewModel]

    /** Validate a date range based on business rules. Valid ranges are:
      *   - Start date is not after end date
      *   - Neither date is in the future
      *   - Range is not more than 90 days (Fio Bank API limitation)
      *
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect with Either an error message (Left) or success (Right)
      */
    def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, Either[String, Unit]]
    
    /** Validate an account ID.
      *
      * @param accountIdStr
      *   The account ID string to validate
      * @return
      *   A ZIO effect with Either an error message (Left) or the parsed AccountId (Right)
      */
    def validateAccountId(
        accountIdStr: String
    ): ZIO[Any, String, Either[String, AccountId]]

    /** Import transactions for the specified account and date range.
      *
      * @param accountId
      *   The account to import transactions from
      * @param startDate
      *   The start date for imported transactions
      * @param endDate
      *   The end date for imported transactions
      * @return
      *   A ZIO effect that returns ImportResults or an error string
      */
    def importTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults]

    /** Get the current status of the import operation.
      *
      * @return
      *   A ZIO effect that returns the current ImportStatus or an error string
      */
    def getImportStatus(): ZIO[Any, String, ImportStatus]
    
    /** Get the list of available accounts.
      *
      * @return
      *   A ZIO effect that returns a list of AccountOption or an error string
      */
    def getAccounts(): ZIO[Any, String, List[AccountOption]]
end TransactionImportPresenter

/** Provides access to TransactionImportPresenter.
  */
object TransactionImportPresenter:
    /** Accessor method for the service.
      *
      * @return
      *   A ZIO effect that accesses the TransactionImportPresenter
      */
    def getImportViewModel(): ZIO[TransactionImportPresenter, String, ImportPageViewModel] =
        ZIO.serviceWithZIO(_.getImportViewModel())

    /** Accessor method for date range validation.
      *
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect with Either an error message (Left) or success (Right)
      */
    def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionImportPresenter, String, Either[String, Unit]] =
        ZIO.serviceWithZIO(_.validateDateRange(startDate, endDate))
        
    /** Accessor method for account ID validation.
      *
      * @param accountIdStr
      *   The account ID string to validate
      * @return
      *   A ZIO effect with Either an error message (Left) or the parsed AccountId (Right)
      */
    def validateAccountId(
        accountIdStr: String
    ): ZIO[TransactionImportPresenter, String, Either[String, AccountId]] =
        ZIO.serviceWithZIO(_.validateAccountId(accountIdStr))

    /** Accessor method for transaction import.
      *
      * @param accountId
      *   The account to import transactions from
      * @param startDate
      *   The start date for imported transactions
      * @param endDate
      *   The end date for imported transactions
      * @return
      *   A ZIO effect that returns ImportResults or an error string
      */
    def importTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionImportPresenter, String, ImportResults] =
        ZIO.serviceWithZIO(_.importTransactions(accountId, startDate, endDate))

    /** Accessor method for import status.
      *
      * @return
      *   A ZIO effect that returns the current ImportStatus or an error string
      */
    def getImportStatus(): ZIO[TransactionImportPresenter, String, ImportStatus] =
        ZIO.serviceWithZIO(_.getImportStatus())
        
    /** Accessor method for getting available accounts.
      *
      * @return
      *   A ZIO effect that returns a list of AccountOption or an error string
      */
    def getAccounts(): ZIO[TransactionImportPresenter, String, List[AccountOption]] =
        ZIO.serviceWithZIO(_.getAccounts())
end TransactionImportPresenter