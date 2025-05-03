package works.iterative.incubator.budget.ui.transaction_import.components

import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.ui.transaction_import.models.ResultsPanelViewModel
import works.iterative.incubator.budget.ui.transaction_import.models.ImportResults
import java.time.LocalDate
import java.time.Instant

/** Tests for the ResultsPanel component.
  */
object ResultsPanelSpec extends ZIOSpecDefault:
    def spec = suite("ResultsPanel")(
        test("does not render when isVisible is false") {
            // Given a view model with isVisible = false
            val viewModel = ResultsPanelViewModel(
                importResults = None,
                isVisible = false,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should be empty
            assert(rendered.trim)(equalTo(""))
        },
        test("renders a panel with date range information") {
            // Given a view model with visible results panel
            val startDate = LocalDate.of(2025, 3, 1)
            val endDate = LocalDate.of(2025, 3, 31)
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 15,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(3))
                )),
                isVisible = true,
                startDate = startDate,
                endDate = endDate
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should include the date range
            assert(rendered)(containsString("Date range: March 1, 2025 to March 31, 2025"))
        },
        test("renders correctly for successful import with transactions") {
            // Given a successful import with 15 transactions
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 15,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(3))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should show success message and transaction count
            val result1 = assert(rendered)(containsString("Import Successful"))
            val result2 = assert(rendered)(containsString("15 transactions successfully imported"))
            val result3 = assert(rendered)(containsString("Completed in 3 seconds"))
            val result4 = assert(rendered)(containsString("bg-green-100 text-green-800"))
            val result5 = assert(rendered)(containsString("View Transactions"))
            result1 && result2 && result3 && result4 && result5
        },
        test("renders correctly for successful import with single transaction") {
            // Given a successful import with 1 transaction
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 1,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(2))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should show singular transaction message
            assert(rendered)(containsString("1 transaction successfully imported"))
        },
        test("renders correctly for import with no transactions") {
            // Given a successful import but with zero transactions
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 0,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(1))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should show no transactions message
            val result1 = assert(rendered)(
                containsString("No transactions found for the selected date range")
            )
            val result2 = assert(rendered)(containsString("Import Successful"))
            val result3 = assert(rendered)(containsString("bg-green-100"))
            result1 && result2 && result3
        },
        test("renders correctly for failed import with error message") {
            // Given a failed import with error message
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 0,
                    errorMessage = Some("Unable to connect to Fio Bank. Please try again later."),
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(1))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should show error styling and message
            val result1 = assert(rendered)(containsString("Import Failed"))
            val result2 = assert(rendered)(
                containsString("Unable to connect to Fio Bank. Please try again later.")
            )
            val result3 = assert(rendered)(containsString("bg-red-100 text-red-800"))
            val result4 = assert(rendered)(containsString("Error code: IMPORT-"))
            val result5 = assert(rendered)(containsString("Retry Import"))
            result1 && result2 && result3 && result4 && result5
        },
        test("includes 'View Transactions' button only for successful imports") {
            // Given a successful and failed import
            val successViewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 5,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(2))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            val errorViewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 0,
                    errorMessage = Some("Connection error"),
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(1))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering both components
            val successRendered = ResultsPanel.render(successViewModel).render
            val errorRendered = ResultsPanel.render(errorViewModel).render

            // Then only success view should have View Transactions button
            val result1 = assert(successRendered)(containsString("View Transactions"))
            val result2 = assert(errorRendered)(not(containsString("View Transactions")))
            result1 && result2
        },
        test("includes 'Retry Import' button only for failed imports") {
            // Given a successful and failed import
            val successViewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 5,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(2))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            val errorViewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 0,
                    errorMessage = Some("Connection error"),
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(1))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering both components
            val successRendered = ResultsPanel.render(successViewModel).render
            val errorRendered = ResultsPanel.render(errorViewModel).render

            // Then only error view should have Retry Import button
            val result1 = assert(successRendered)(not(containsString("Retry Import")))
            val result2 = assert(errorRendered)(containsString("Retry Import"))
            result1 && result2
        },
        test("includes HTMX attributes on retry button") {
            // Given a failed import
            val startDate = LocalDate.of(2025, 3, 1)
            val endDate = LocalDate.of(2025, 3, 31)
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 0,
                    errorMessage = Some("Connection error"),
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(1))
                )),
                isVisible = true,
                startDate = startDate,
                endDate = endDate
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should have HTMX attributes for the retry action
            val result1 = assert(rendered)(containsString("hx-post"))
            val result2 = assert(rendered)(containsString("hx-target=\"#import-results\""))
            val result3 = assert(rendered)(containsString("hx-swap=\"outerHTML\""))
            val result4 = assert(rendered)(containsString(s"startDate=2025-03-01"))
            val result5 = assert(rendered)(containsString(s"endDate=2025-03-31"))
            result1 && result2 && result3 && result4 && result5
        },
        test("includes proper accessibility attributes") {
            // Given a visible results panel
            val viewModel = ResultsPanelViewModel(
                importResults = Some(ImportResults(
                    transactionCount = 5,
                    errorMessage = None,
                    startTime = Instant.now(),
                    endTime = Some(Instant.now().plusSeconds(2))
                )),
                isVisible = true,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 3, 31)
            )

            // When rendering the component
            val rendered = ResultsPanel.render(viewModel).render

            // Then it should include accessibility attributes
            val result1 = assert(rendered)(containsString("aria-live=\"polite\""))
            val result2 = assert(rendered)(containsString("role=\"region\""))
            val result3 = assert(rendered)(containsString("aria-labelledby=\"results-heading\""))
            result1 && result2 && result3
        }
    )
end ResultsPanelSpec
