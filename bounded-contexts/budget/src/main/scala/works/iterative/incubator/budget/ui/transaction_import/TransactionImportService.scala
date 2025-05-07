package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.LocalDate
import zio.*

/** Service interface for handling transaction imports from Fio Bank. This service provides methods
  * for the import workflow:
  *   1. Initial view model for the import page 2. Date range validation 3. Transaction import
  *      execution 4. Import status tracking
  *
  * Category: Presenter
  * Layer: UI/Presentation
  */
trait TransactionImportService:
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

    /** Import transactions for the specified date range.
      *
      * @param startDate
      *   The start date for imported transactions
      * @param endDate
      *   The end date for imported transactions
      * @return
      *   A ZIO effect that returns ImportResults or an error string
      */
    def importTransactions(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, String, ImportResults]

    /** Get the current status of the import operation.
      *
      * @return
      *   A ZIO effect that returns the current ImportStatus or an error string
      */
    def getImportStatus(): ZIO[Any, String, ImportStatus]
end TransactionImportService

/** Provides access to TransactionImportService.
  */
object TransactionImportService:
    /** Accessor method for the service.
      *
      * @return
      *   A ZIO effect that accesses the TransactionImportService
      */
    def getImportViewModel(): ZIO[TransactionImportService, String, ImportPageViewModel] =
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
    ): ZIO[TransactionImportService, String, Either[String, Unit]] =
        ZIO.serviceWithZIO(_.validateDateRange(startDate, endDate))

    /** Accessor method for transaction import.
      *
      * @param startDate
      *   The start date for imported transactions
      * @param endDate
      *   The end date for imported transactions
      * @return
      *   A ZIO effect that returns ImportResults or an error string
      */
    def importTransactions(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[TransactionImportService, String, ImportResults] =
        ZIO.serviceWithZIO(_.importTransactions(startDate, endDate))

    /** Accessor method for import status.
      *
      * @return
      *   A ZIO effect that returns the current ImportStatus or an error string
      */
    def getImportStatus(): ZIO[TransactionImportService, String, ImportStatus] =
        ZIO.serviceWithZIO(_.getImportStatus())
end TransactionImportService
