package works.iterative.incubator.budget.ui.transaction_import.components

import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.ui.transaction_import.models.StatusIndicatorViewModel
import works.iterative.incubator.budget.ui.transaction_import.models.ImportStatus

/** Tests for the StatusIndicator component.
  */
object StatusIndicatorSpec extends ZIOSpecDefault:
    def spec = suite("StatusIndicator")(
        test("renders correctly for NotStarted status") {
            // Given a NotStarted status
            val viewModel = StatusIndicatorViewModel(
                status = ImportStatus.NotStarted
            )

            // When rendering the component
            val rendered = StatusIndicator.render(viewModel).render

            // Then it should contain expected elements and styling
            val result1 = assert(rendered)(containsString("Ready to import"))
            val result2 = assert(rendered)(containsString("bg-gray-100 text-gray-800"))
            val result3 = assert(rendered)(not(containsString("animate-")))
            result1 && result2 && result3
        },
        test("renders correctly for InProgress status") {
            // Given an InProgress status
            val viewModel = StatusIndicatorViewModel(
                status = ImportStatus.InProgress
            )

            // When rendering the component
            val rendered = StatusIndicator.render(viewModel).render

            // Then it should contain expected elements and styling
            val result1 = assert(rendered)(containsString("Importing transactions..."))
            val result2 = assert(rendered)(containsString("bg-blue-100 text-blue-800"))
            val result3 = assert(rendered)(containsString("animate-pulse"))
            val result4 = assert(rendered)(containsString("animate-spin"))
            result1 && result2 && result3 && result4
        },
        test("renders correctly for Completed status") {
            // Given a Completed status
            val viewModel = StatusIndicatorViewModel(
                status = ImportStatus.Completed
            )

            // When rendering the component
            val rendered = StatusIndicator.render(viewModel).render

            // Then it should contain expected elements and styling
            val result1 = assert(rendered)(containsString("Import completed successfully"))
            val result2 = assert(rendered)(containsString("bg-green-100 text-green-800"))
            val result3 = assert(rendered)(containsString("text-green-600"))
            result1 && result2 && result3
        },
        test("renders correctly for Error status") {
            // Given an Error status
            val viewModel = StatusIndicatorViewModel(
                status = ImportStatus.Error
            )

            // When rendering the component
            val rendered = StatusIndicator.render(viewModel).render

            // Then it should contain expected elements and styling
            val result1 = assert(rendered)(containsString("Import failed"))
            val result2 = assert(rendered)(containsString("bg-red-100 text-red-800"))
            val result3 = assert(rendered)(containsString("text-red-600"))
            result1 && result2 && result3
        },
        test("does not render when isVisible is false") {
            // Given a status indicator that should not be visible
            val viewModel = StatusIndicatorViewModel(
                status = ImportStatus.InProgress,
                isVisible = false
            )

            // When rendering the component
            val rendered = StatusIndicator.render(viewModel).render

            // Then it should be empty
            assert(rendered.trim)(equalTo(""))
        },
        test("includes appropriate ARIA attributes for accessibility") {
            // Given any visible status indicator
            val viewModel = StatusIndicatorViewModel(
                status = ImportStatus.InProgress
            )

            // When rendering the component
            val rendered = StatusIndicator.render(viewModel).render

            // Then it should have proper accessibility attributes
            val result1 = assert(rendered)(containsString("aria-live=\"polite\""))
            val result2 = assert(rendered)(containsString("role=\"status\""))
            result1 && result2
        },
        test("shows loading spinner only in InProgress status") {
            // Test each status to verify loading spinner only in InProgress
            val notStartedViewModel = StatusIndicatorViewModel(ImportStatus.NotStarted)
            val inProgressViewModel = StatusIndicatorViewModel(ImportStatus.InProgress)
            val completedViewModel = StatusIndicatorViewModel(ImportStatus.Completed)
            val errorViewModel = StatusIndicatorViewModel(ImportStatus.Error)

            val notStartedRendered = StatusIndicator.render(notStartedViewModel).render
            val inProgressRendered = StatusIndicator.render(inProgressViewModel).render
            val completedRendered = StatusIndicator.render(completedViewModel).render
            val errorRendered = StatusIndicator.render(errorViewModel).render

            val result1 = assert(inProgressRendered)(containsString("animate-spin"))
            val result2 = assert(notStartedRendered)(not(containsString("animate-spin")))
            val result3 = assert(completedRendered)(not(containsString("animate-spin")))
            val result4 = assert(errorRendered)(not(containsString("animate-spin")))
            result1 && result2 && result3 && result4
        },
        test("shows success icon only in Completed status") {
            // Test each status to verify success icon only in Completed
            val notStartedViewModel = StatusIndicatorViewModel(ImportStatus.NotStarted)
            val inProgressViewModel = StatusIndicatorViewModel(ImportStatus.InProgress)
            val completedViewModel = StatusIndicatorViewModel(ImportStatus.Completed)
            val errorViewModel = StatusIndicatorViewModel(ImportStatus.Error)

            val notStartedRendered = StatusIndicator.render(notStartedViewModel).render
            val inProgressRendered = StatusIndicator.render(inProgressViewModel).render
            val completedRendered = StatusIndicator.render(completedViewModel).render
            val errorRendered = StatusIndicator.render(errorViewModel).render

            // Look for checkmark icon in rendered output
            val result1 = assert(completedRendered)(containsString("text-green-600"))
            val result2 = assert(notStartedRendered)(not(containsString("text-green-600")))
            val result3 = assert(inProgressRendered)(not(containsString("text-green-600")))
            val result4 = assert(errorRendered)(not(containsString("text-green-600")))
            result1 && result2 && result3 && result4
        },
        test("shows error icon only in Error status") {
            // Test each status to verify error icon only in Error
            val notStartedViewModel = StatusIndicatorViewModel(ImportStatus.NotStarted)
            val inProgressViewModel = StatusIndicatorViewModel(ImportStatus.InProgress)
            val completedViewModel = StatusIndicatorViewModel(ImportStatus.Completed)
            val errorViewModel = StatusIndicatorViewModel(ImportStatus.Error)

            val notStartedRendered = StatusIndicator.render(notStartedViewModel).render
            val inProgressRendered = StatusIndicator.render(inProgressViewModel).render
            val completedRendered = StatusIndicator.render(completedViewModel).render
            val errorRendered = StatusIndicator.render(errorViewModel).render

            // Look for error icon in rendered output
            val result1 = assert(errorRendered)(containsString("text-red-600"))
            val result2 = assert(notStartedRendered)(not(containsString("text-red-600")))
            val result3 = assert(inProgressRendered)(not(containsString("text-red-600")))
            val result4 = assert(completedRendered)(not(containsString("text-red-600")))
            result1 && result2 && result3 && result4
        }
    )
end StatusIndicatorSpec
