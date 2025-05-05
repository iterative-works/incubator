package works.iterative.incubator.budget.ui.transaction_import.components

import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.ui.transaction_import.models.DateRangeSelectorViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import works.iterative.incubator.budget.ui.transaction_import.components.DateRangeSelector.render

/** Tests for the DateRangeSelector component.
  */
object DateRangeSelectorSpec extends ZIOSpecDefault:
    def spec = suite("DateRangeSelector")(
        test("renders correctly with valid date range") {
            // Given a valid date range
            val startDate = LocalDate.now().minusDays(30)
            val endDate = LocalDate.now().minusDays(1)
            val viewModel = DateRangeSelectorViewModel(startDate, endDate)

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should contain expected elements
            val result1 = assert(rendered)(containsString("<input type=\"date\" id=\"start-date\""))
            val result2 = assert(rendered)(containsString("<input type=\"date\" id=\"end-date\""))
            val result3 = assert(rendered)(containsString("hx-post=\"/transactions/import/validate-dates\""))
            val result4 =
                assert(rendered)(
                    containsString("style=\"display: none\"")
                ) // Error message should be hidden
            result1 && result2 && result3 && result4
        },
        test("renders correctly with error when start date is after end date") {
            // Given an invalid date range where start date is after end date
            val startDate = LocalDate.now().minusDays(1)
            val endDate = LocalDate.now().minusDays(30)
            val errorMessage = "Start date must be before end date"
            val viewModel = DateRangeSelectorViewModel(startDate, endDate, Some(errorMessage))

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should show the error message
            val result1 = assert(rendered)(containsString("Start date must be before end date"))
            val result2 =
                assert(rendered)(
                    containsString("style=\"display: block\"")
                ) // Error message should be visible
            val result3 =
                assert(rendered)(containsString("border-red-500")) // Should have error border
            result1 && result2 && result3
        },
        test("renders correctly with error when date range exceeds 90 days") {
            // Given an invalid date range exceeding 90 days
            val startDate = LocalDate.now().minusDays(100)
            val endDate = LocalDate.now().minusDays(1)
            val errorMessage = "Date range cannot exceed 90 days"
            val viewModel = DateRangeSelectorViewModel(startDate, endDate, Some(errorMessage))

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should show the error message
            val result1 = assert(rendered)(containsString("Date range cannot exceed 90 days"))
            val result2 =
                assert(rendered)(
                    containsString("style=\"display: block\"")
                ) // Error message should be visible
            result1 && result2
        },
        test("sets the max date attribute correctly") {
            // Given a view model with today as max date
            val today = LocalDate.now()
            val viewModel = DateRangeSelectorViewModel(
                today.minusDays(30),
                today.minusDays(1)
            )

            // When rendering the component
            val rendered = render(viewModel).render
            val expectedMaxDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Then it should have the correct max date attribute
            assert(rendered)(containsString(s"max=\"$expectedMaxDate\""))
        },
        test("renders HTMX attributes for validation") {
            // Given a valid view model
            val viewModel = DateRangeSelectorViewModel(
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(1)
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should have the HTMX attributes for validation
            val result1 = assert(rendered)(containsString("hx-post=\"/transactions/import/validate-dates\""))
            val result2 = assert(rendered)(containsString("hx-trigger=\"change\""))
            val result3 = assert(rendered)(containsString("hx-target=\"#date-range-selector\""))
            val result4 = assert(rendered)(containsString("hx-swap=\"outerHTML\""))
            result1 && result2 && result3 && result4
        }
    )
end DateRangeSelectorSpec
