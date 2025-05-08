package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate
import scala.util.Try

/** View model for the transaction import form.
  * Combines all form fields, validation state, and import results into a single cohesive model.
  *
  * @param accounts
  *   List of available accounts to choose from
  * @param selectedAccountId
  *   Currently selected account ID
  * @param startDate
  *   The start date for the transaction import range
  * @param endDate
  *   The end date for the transaction import range
  * @param fieldErrors
  *   Map of field names to error messages for form validation
  * @param globalError
  *   Optional error message that applies to the entire form
  * @param isSubmitting
  *   Whether the form is currently being submitted
  * @param importStatus
  *   The current status of the import operation
  * @param importResults
  *   Optional results of the import operation
  *
  * Category: View Model
  * Layer: UI/Presentation
  */
case class TransactionImportFormViewModel(
    // Form data
    accounts: List[AccountOption] = AccountSelectorViewModel.defaultAccounts,
    selectedAccountId: Option[String] = None,
    startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    endDate: LocalDate = LocalDate.now(),
    
    // Validation errors
    fieldErrors: Map[String, String] = Map.empty,
    globalError: Option[String] = None,
    
    // State
    isSubmitting: Boolean = false,
    importStatus: ImportStatus = ImportStatus.NotStarted,
    importResults: Option[ImportResults] = None
):
    /** Indicates whether there are any validation errors.
      *
      * @return
      *   true if there are field or global errors
      */
    def hasErrors: Boolean = fieldErrors.nonEmpty || globalError.isDefined
    
    /** Updates the view model with validation errors.
      *
      * @param errors
      *   The validation errors to apply
      * @return
      *   A new view model with the validation errors applied
      */
    def withValidationErrors(errors: ValidationErrors): TransactionImportFormViewModel =
        this.copy(
            fieldErrors = errors.errors,
            globalError = errors.globalErrors.headOption,
            isSubmitting = false,
            importStatus = ImportStatus.NotStarted // Reset status to not started when validation fails
        )
        
    /** Updates the view model with import results.
      *
      * @param results
      *   The import results to apply
      * @return
      *   A new view model with the import results and completed status
      */
    def withImportResults(results: ImportResults): TransactionImportFormViewModel =
        this.copy(
            importResults = Some(results),
            importStatus = ImportStatus.Completed,
            isSubmitting = false
        )
        
    /** Updates the view model with an error message.
      *
      * @param error
      *   The error message
      * @return
      *   A new view model with the error message and error status
      */
    def withError(error: String): TransactionImportFormViewModel =
        this.copy(
            globalError = Some(error),
            importStatus = ImportStatus.Error,
            isSubmitting = false
        )
        
    /** Updates the view model to indicate that form submission is in progress.
      *
      * @return
      *   A new view model with submission in progress and in-progress status
      */
    def submitting: TransactionImportFormViewModel =
        this.copy(
            isSubmitting = true,
            importStatus = ImportStatus.InProgress
        )
        
    /** Converts the view model to a domain command for validation and processing.
      *
      * @return
      *   A TransactionImportCommand with values from this view model
      */
    def toCommand: TransactionImportCommand =
        TransactionImportCommand(
            accountId = selectedAccountId.getOrElse(""),
            startDate = startDate.toString,
            endDate = endDate.toString
        )
end TransactionImportFormViewModel

/** Companion object for TransactionImportFormViewModel */
object TransactionImportFormViewModel:
    /** Creates a view model from form data.
      *
      * @param formData
      *   Map of form field names to values
      * @return
      *   A new view model with values from the form data
      */
    def fromFormData(formData: Map[String, String]): TransactionImportFormViewModel =
        val startDate = Try(LocalDate.parse(formData.getOrElse("startDate", "")))
            .getOrElse(LocalDate.now().withDayOfMonth(1))
        val endDate = Try(LocalDate.parse(formData.getOrElse("endDate", "")))
            .getOrElse(LocalDate.now())
            
        TransactionImportFormViewModel(
            selectedAccountId = Option(formData.getOrElse("accountId", "")).filter(_.nonEmpty),
            startDate = startDate,
            endDate = endDate
        )
    
    /** Creates a view model with default values.
      *
      * @return
      *   A new view model with default values
      */
    val default: TransactionImportFormViewModel = TransactionImportFormViewModel()
end TransactionImportFormViewModel