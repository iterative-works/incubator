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
            val result1 = assert(rendered)(containsString("<input type=\"date\" id=\"startDate\""))
            val result2 = assert(rendered)(containsString("<input type=\"date\" id=\"endDate\""))
            val result3 = assert(rendered)(containsString("style=\"display: none\"")) // Error message should be hidden
            result1 && result2 && result3
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
        test("does not contain HTMX attributes for validation after refactoring") {
            // Given a valid view model
            val viewModel = DateRangeSelectorViewModel(
                LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(1)
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should not have the HTMX attributes for validation anymore
            // since we've moved validation to the form level
            assert(rendered)(not(containsString("hx-post=")))
        }
    )
end DateRangeSelectorSpec
