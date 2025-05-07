package works.iterative.incubator.budget.ui.transaction_import.components

import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.ui.transaction_import.models.ImportButtonViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import works.iterative.incubator.budget.ui.transaction_import.components.ImportButton.render

/** Tests for the ImportButton component.
  */
object ImportButtonSpec extends ZIOSpecDefault:
    def spec = suite("ImportButton")(
        test("renders correctly in enabled state") {
            // Given an enabled button view model
            val startDate = LocalDate.of(2025, 3, 1)
            val endDate = LocalDate.of(2025, 3, 31)
            val viewModel = ImportButtonViewModel(
                isEnabled = true,
                isLoading = false,
                accountId = Some("0100-1234567890"),
                startDate = startDate,
                endDate = endDate
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should have the correct attributes and classes
            val result1 = assert(rendered)(containsString("Import Transactions"))
            val result2 = assert(rendered)(containsString("bg-blue-600"))
            val result3 = assert(rendered)(not(containsString("disabled=\"disabled\"")))
            val result4 = assert(rendered)(containsString("aria-disabled=\"false\""))
            result1 && result2 && result3 && result4
        },
        test("renders correctly in disabled state") {
            // Given a disabled button view model
            val startDate = LocalDate.of(2025, 3, 1)
            val endDate = LocalDate.of(2025, 3, 31)
            val viewModel = ImportButtonViewModel(
                isEnabled = false,
                isLoading = false,
                accountId = Some("0100-1234567890"),
                startDate = startDate,
                endDate = endDate
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should have the correct attributes and classes
            val result1 = assert(rendered)(containsString("Import Transactions"))
            val result2 = assert(rendered)(containsString("bg-gray-300"))
            val result3 = assert(rendered)(containsString("disabled=\"disabled\""))
            val result4 = assert(rendered)(containsString("aria-disabled=\"true\""))
            val result5 = assert(rendered)(containsString("cursor-not-allowed"))
            result1 && result2 && result3 && result4 && result5
        },
        test("renders correctly in loading state") {
            // Given a loading button view model
            val startDate = LocalDate.of(2025, 3, 1)
            val endDate = LocalDate.of(2025, 3, 31)
            val viewModel = ImportButtonViewModel(
                isEnabled = true,
                isLoading = true,
                accountId = Some("0100-1234567890"),
                startDate = startDate,
                endDate = endDate
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should show the loading state
            val result1 = assert(rendered)(containsString("Importing..."))
            val result2 = assert(rendered)(containsString("animate-spin"))
            val result3 = assert(rendered)(containsString("inline-block"))
            val result4 = assert(rendered)(containsString("disabled=\"disabled\""))
            val result5 = assert(rendered)(containsString("aria-disabled=\"true\""))
            result1 && result2 && result3 && result4 && result5
        },
        test("includes correct HTMX attributes") {
            // Given a view model with specific dates
            val startDate = LocalDate.of(2025, 3, 1)
            val endDate = LocalDate.of(2025, 3, 31)
            val viewModel = ImportButtonViewModel(
                isEnabled = true,
                isLoading = false,
                accountId = Some("0100-1234567890"),
                startDate = startDate,
                endDate = endDate
            )

            // Format dates for comparison
            val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
            val startDateFormatted = startDate.format(dateFormat)
            val endDateFormatted = endDate.format(dateFormat)

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should have the correct HTMX attributes
            val result1 = assert(rendered)(containsString(
                s"hx-post=\"/transactions/import?accountId=0100-1234567890&amp;startDate=$startDateFormatted&amp;endDate=$endDateFormatted\""
            ))
            val result2 = assert(rendered)(containsString("hx-target=\"#results-panel-container\""))
            val result3 = assert(rendered)(containsString("hx-swap=\"innerHTML\""))
            val result4 = assert(rendered)(containsString("hx-indicator=\"#loading-spinner\""))
            result1 && result2 && result3 && result4
        }
    )
end ImportButtonSpec
