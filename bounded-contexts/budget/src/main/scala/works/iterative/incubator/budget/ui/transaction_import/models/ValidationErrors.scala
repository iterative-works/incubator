package works.iterative.incubator.budget.ui.transaction_import.models

/** Represents validation errors for a form or command.
  *
  * @param errors
  *   Map of field names to error messages
  * @param globalErrors
  *   List of errors that apply to the entire form/command rather than specific fields
  *
  * Category: Domain Model Layer: Domain
  */
case class ValidationErrors(
    errors: Map[String, String] = Map.empty,
    globalErrors: List[String] = List.empty
):
    /** Determines if there are any validation errors.
      *
      * @return
      *   true if there are field or global errors, false otherwise
      */
    def hasErrors: Boolean = errors.nonEmpty || globalErrors.nonEmpty

    /** Add a field error to the validation errors.
      *
      * @param field
      *   The name of the field with the error
      * @param errorMessage
      *   The error message
      * @return
      *   A new ValidationErrors instance with the added error
      */
    def addFieldError(field: String, errorMessage: String): ValidationErrors =
        this.copy(
            errors = errors + (field -> errorMessage)
        )

    /** Add a global error to the validation errors.
      *
      * @param errorMessage
      *   The error message
      * @return
      *   A new ValidationErrors instance with the added global error
      */
    def addGlobalError(errorMessage: String): ValidationErrors =
        this.copy(
            globalErrors = globalErrors :+ errorMessage
        )

    /** Get the error message for a specific field.
      *
      * @param field
      *   The name of the field
      * @return
      *   The error message, if any
      */
    def getFieldError(field: String): Option[String] =
        errors.get(field)
end ValidationErrors

/** Companion object for ValidationErrors */
object ValidationErrors:
    /** Create an empty ValidationErrors instance */
    val empty: ValidationErrors = ValidationErrors()

    /** Create a ValidationErrors instance with a single field error */
    def forField(field: String, errorMessage: String): ValidationErrors =
        ValidationErrors(errors = Map(field -> errorMessage))

    /** Create a ValidationErrors instance with a single global error */
    def forGlobal(errorMessage: String): ValidationErrors =
        ValidationErrors(globalErrors = List(errorMessage))
end ValidationErrors
