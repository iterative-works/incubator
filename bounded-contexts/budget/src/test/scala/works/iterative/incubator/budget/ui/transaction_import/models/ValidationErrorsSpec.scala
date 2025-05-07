package works.iterative.incubator.budget.ui.transaction_import.models

import zio.test.*

/** Test suite for ValidationErrors.
  *
  * Tests the ValidationErrors model for proper error management and utility methods.
  */
class ValidationErrorsSpec extends ZIOSpecDefault:
    def spec = suite("ValidationErrors")(
        test("should detect when errors exist") {
            // Create ValidationErrors with field errors
            val fieldErrorsOnly = ValidationErrors(
                errors = Map("field1" -> "Error in field 1", "field2" -> "Error in field 2")
            )

            // Create ValidationErrors with global errors
            val globalErrorsOnly = ValidationErrors(
                globalErrors = List("Global error 1", "Global error 2")
            )

            // Create ValidationErrors with both field and global errors
            val bothErrorTypes = ValidationErrors(
                errors = Map("field1" -> "Error in field 1"),
                globalErrors = List("Global error 1")
            )

            // Create ValidationErrors with no errors
            val noErrors = ValidationErrors()

            assertTrue(
                fieldErrorsOnly.hasErrors,
                globalErrorsOnly.hasErrors,
                bothErrorTypes.hasErrors,
                !noErrors.hasErrors
            )
        },
        test("should add field errors correctly") {
            // Start with empty errors
            val initialErrors = ValidationErrors.empty

            // Add a field error
            val withOneFieldError = initialErrors.addFieldError("field1", "Error in field 1")

            // Add another field error
            val withTwoFieldErrors = withOneFieldError.addFieldError("field2", "Error in field 2")

            // Override an existing field error
            val withOverriddenError =
                withTwoFieldErrors.addFieldError("field1", "New error in field 1")

            assertTrue(
                withOneFieldError.errors.size == 1,
                withOneFieldError.errors.contains("field1"),
                withTwoFieldErrors.errors.size == 2,
                withTwoFieldErrors.errors.contains("field2"),
                withOverriddenError.errors("field1") == "New error in field 1"
            )
        },
        test("should add global errors correctly") {
            // Start with empty errors
            val initialErrors = ValidationErrors.empty

            // Add a global error
            val withOneGlobalError = initialErrors.addGlobalError("Global error 1")

            // Add another global error
            val withTwoGlobalErrors = withOneGlobalError.addGlobalError("Global error 2")

            assertTrue(
                withOneGlobalError.globalErrors.size == 1,
                withOneGlobalError.globalErrors.contains("Global error 1"),
                withTwoGlobalErrors.globalErrors.size == 2,
                withTwoGlobalErrors.globalErrors.contains("Global error 2")
            )
        },
        test("should get field error correctly") {
            // Create ValidationErrors with field errors
            val errors = ValidationErrors(
                errors = Map("field1" -> "Error in field 1", "field2" -> "Error in field 2")
            )

            // Get existing and non-existing field errors
            val field1Error = errors.getFieldError("field1")
            val field2Error = errors.getFieldError("field2")
            val field3Error = errors.getFieldError("field3")

            assertTrue(
                field1Error.contains("Error in field 1"),
                field2Error.contains("Error in field 2"),
                field3Error.isEmpty
            )
        },
        test("should create errors from static methods") {
            // Create errors using companion object methods
            val emptyErrors = ValidationErrors.empty
            val fieldErrors = ValidationErrors.forField("field1", "Error in field 1")
            val globalErrors = ValidationErrors.forGlobal("Global error 1")

            assertTrue(
                emptyErrors.errors.isEmpty && emptyErrors.globalErrors.isEmpty,
                fieldErrors.errors.size == 1 && fieldErrors.errors("field1") == "Error in field 1",
                globalErrors.globalErrors.size == 1 && globalErrors.globalErrors.head == "Global error 1"
            )
        }
    )
end ValidationErrorsSpec
