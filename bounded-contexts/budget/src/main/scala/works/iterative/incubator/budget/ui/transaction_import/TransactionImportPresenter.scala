package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import zio.*

/** Service interface for handling transaction imports from Fio Bank. This service provides methods
  * for the import workflow:
  *   1. Initial view model for the import page 2. Form validation and import processing 3. Import
  *      status tracking
  *
  * Category: Presenter Layer: UI/Presentation
  */
trait TransactionImportPresenter:
    /** Get the initial form view model for the import page.
      *
      * @return
      *   A ZIO effect that returns the TransactionImportFormViewModel or an error string
      */
    def getImportViewModel(): ZIO[Any, String, TransactionImportFormViewModel]

    /** Validate and process a transaction import command.
      *
      * @param command
      *   The command to validate and process
      * @return
      *   A ZIO effect that returns Either validation errors or import results
      */
    def validateAndProcess(
        command: TransactionImportCommand
    ): ZIO[Any, String, Either[ValidationErrors, ImportResults]]

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
    def getImportViewModel()
        : ZIO[TransactionImportPresenter, String, TransactionImportFormViewModel] =
        ZIO.serviceWithZIO(_.getImportViewModel())

    /** Accessor method for form validation and processing.
      *
      * @param command
      *   The command to validate and process
      * @return
      *   A ZIO effect that returns Either validation errors or import results
      */
    def validateAndProcess(
        command: TransactionImportCommand
    ): ZIO[TransactionImportPresenter, String, Either[ValidationErrors, ImportResults]] =
        ZIO.serviceWithZIO(_.validateAndProcess(command))

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
