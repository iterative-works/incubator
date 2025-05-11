package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.ui.transaction_import.models.*
import works.iterative.incubator.budget.ui.transaction_import.components.*
import zio.test.*
import java.time.{Instant, LocalDate}
import java.time.temporal.ChronoUnit

/** Test suite for the transaction import form.
  *
  * Tests the integrated form validation approach, ensuring that error messages are properly
  * displayed and the form responds correctly to different validation states.
  */
class TransactionImportFormSpec extends ZIOSpecDefault:
    def spec = suite("TransactionImportForm")(
        test("should display validation errors for invalid input") {
            // Create view model with errors
            val viewModel = TransactionImportFormViewModel(
                fieldErrors = Map(
                    "accountId" -> "Account selection is required",
                    "dateRange" -> "Start date cannot be after end date"
                )
            )

            // Render form
            val html = TransactionImportForm.render(viewModel).render

            // Verify error messages are displayed
            assertTrue(
                html.contains("Account selection is required"),
                html.contains("Start date cannot be after end date")
            )
        },
        test("should disable submit button when errors exist") {
            // Create view model with errors
            val viewModel = TransactionImportFormViewModel(
                fieldErrors = Map("accountId" -> "Account selection is required")
            )

            // Render form
            val html = TransactionImportForm.render(viewModel).render

            // Verify button is disabled
            assertTrue(
                html.contains("disabled=\"disabled\"")
            )
        },
        test("should enable submit button when no errors exist and account is selected") {
            // Create view model with no errors and selected account
            val viewModel = TransactionImportFormViewModel(
                selectedAccountId = Some("0100-1234567890")
            )

            // Render form
            val html = TransactionImportForm.render(viewModel).render

            // Verify button is not disabled
            assertTrue(
                !html.contains("disabled=\"disabled\"")
            )
        },
        test("should show submitting state during form submission") {
            // Create view model with submission in progress
            val viewModel = TransactionImportFormViewModel(
                selectedAccountId = Some("0100-1234567890"),
                isSubmitting = true,
                importStatus = ImportStatus.InProgress
            )

            // Render form
            val html = TransactionImportForm.render(viewModel).render

            // Verify status indicator is shown and button text is updated
            assertTrue(
                html.contains("status-indicator-container"),
                html.contains("data-status=\"InProgress\""),
                html.contains("disabled=\"disabled\""),
                html.contains("Importing...")
            )
        },
        test("should convert ValidationErrors to field errors in view model") {
            // Create validation errors
            val validationErrors = ValidationErrors(
                errors = Map(
                    "accountId" -> "Account selection is required",
                    "dateRange" -> "Start date cannot be after end date"
                ),
                globalErrors = List("An unexpected error occurred")
            )

            // Create view model and apply validation errors
            val viewModel = TransactionImportFormViewModel().withValidationErrors(validationErrors)

            // Verify errors are properly converted
            assertTrue(
                viewModel.fieldErrors.contains("accountId"),
                viewModel.fieldErrors.contains("dateRange"),
                viewModel.globalError.exists(_ == "An unexpected error occurred")
            )
        },
        test("should convert import results to view model state") {
            // Create import results
            val now = Instant.now()
            val importResults = ImportResults(
                transactionCount = 42,
                errorMessage = None,
                startTime = now.minus(5, ChronoUnit.SECONDS),
                endTime = Some(now)
            )

            // Create view model and apply import results
            val viewModel = TransactionImportFormViewModel().withImportResults(importResults)

            // Verify results are properly converted
            assertTrue(
                viewModel.importStatus == ImportStatus.Completed,
                viewModel.importResults.exists(_.transactionCount == 42),
                viewModel.isSubmitting == false
            )
        },
        test("should convert error message to view model state") {
            // Create view model with error
            val viewModel = TransactionImportFormViewModel().withError("Connection failed")

            // Verify error is properly set
            assertTrue(
                viewModel.importStatus == ImportStatus.Error,
                viewModel.globalError.exists(_ == "Connection failed"),
                viewModel.isSubmitting == false
            )
        }
    )
end TransactionImportFormSpec
