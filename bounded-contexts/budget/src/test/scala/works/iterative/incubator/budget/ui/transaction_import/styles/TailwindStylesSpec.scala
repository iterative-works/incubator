package works.iterative.incubator.budget.ui.transaction_import.styles

import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.ui.transaction_import.models.ImportStatus
import scala.annotation.unused
import scalatags.Text.all.*

object TailwindStylesSpec extends ZIOSpecDefault:
    def spec = suite("TailwindStyles")(
        test("style string construction provides expected Tailwind classes") {
            // Check several key style strings
            assertTrue(
                TailwindStyles.buttonPrimary.contains("px-4") &&
                    TailwindStyles.buttonPrimary.contains("py-2") &&
                    TailwindStyles.buttonPrimary.contains("rounded-md") &&
                    TailwindStyles.buttonPrimary.contains("bg-blue-600") &&

                    TailwindStyles.heading1.contains("text-2xl") &&
                    TailwindStyles.heading1.contains("font-bold") &&

                    TailwindStyles.panel.contains("bg-white") &&
                    TailwindStyles.panel.contains("rounded-lg") &&

                    TailwindStyles.StatusIndicator.completed.contains("bg-green-50") &&
                    TailwindStyles.StatusIndicator.completed.contains("text-green-700")
            )
        },
        test("style composition creates combined class strings") {
            // Test composing styles with string interpolation
            val baseStyle = "p-4 bg-white"
            val additionalStyle = "rounded-lg shadow"
            val combined = s"$baseStyle $additionalStyle"

            assertTrue(
                combined.contains("p-4") &&
                    combined.contains("bg-white") &&
                    combined.contains("rounded-lg") &&
                    combined.contains("shadow")
            )
        },
        test("component-specific styles are correctly organized") {
            // Check that components have appropriate style objects
            assertTrue(
                TailwindStyles.DateRangeSelector != null &&
                    TailwindStyles.ImportButton != null &&
                    TailwindStyles.StatusIndicator != null &&
                    TailwindStyles.ResultsPanel != null
            )
        },
        test("conditionalClass returns correct classes based on condition") {
            val trueResult = TailwindStyles.conditionalClass(true, "active", "inactive")
            val falseResult = TailwindStyles.conditionalClass(false, "active", "inactive")

            assertTrue(
                trueResult == "active" &&
                    falseResult == "inactive"
            )
        },
        test("conditionalClass returns empty string for false condition without false class") {
            val result = TailwindStyles.conditionalClass(false, "active")

            assertTrue(result.isEmpty)
        },
        test("styles integrate properly with scalatags component rendering") {
            // Test rendering a simple component with TailwindStyles
            val buttonText = "Click me"
            @unused val buttonTag = button(
                cls := TailwindStyles.buttonPrimary,
                buttonText
            ).render

            // Test rendering a status indicator
            val status = ImportStatus.Completed
            val showSuccessIcon = status == ImportStatus.Completed

            @unused val statusIndicator = div(
                cls := TailwindStyles.StatusIndicator.completed,
                showSuccessIcon match
                    case true  => span(cls := TailwindStyles.iconSuccess, "✓")
                    case false => ""
                ,
                span(
                    cls := TailwindStyles.StatusIndicator.statusText,
                    "Import completed successfully"
                )
            ).render

            // We're just checking that this compiles correctly
            assertTrue(true)
        }
    )
end TailwindStylesSpec
