package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.ImportButtonViewModel
import scalatags.Text.all.*
import java.time.format.DateTimeFormatter

/** Component for triggering transaction import with loading state. Uses HTMX to perform the import
  * operation and display loading spinner.
  */
object ImportButton:
    /** Renders an import button component.
      *
      * @param viewModel
      *   The view model containing button state
      * @return
      *   A Scalatags fragment representing the import button
      */
    def render(viewModel: ImportButtonViewModel): Frag =
        // Format dates for request parameters
        val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
        val startDateParam = viewModel.startDate.format(dateFormat)
        val endDateParam = viewModel.endDate.format(dateFormat)

        // Button styling classes based on state
        val baseClasses =
            "px-4 py-2 rounded-md font-medium focus:outline-none focus:ring-2 focus:ring-offset-2"
        val enabledClasses = "bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500"
        val disabledClasses = "bg-gray-300 text-gray-500 cursor-not-allowed"

        val buttonClasses = if viewModel.isDisabled then
            s"$baseClasses $disabledClasses"
        else
            s"$baseClasses $enabledClasses"

        // Loading spinner (visible when button has htmx-request class)
        val loadingSpinner =
            span(
                cls := "inline-block animate-spin ml-2 htmx-indicator"
            )(
                // SVG spinner icon
                raw("""<svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    |  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    |  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    |</svg>""".stripMargin)
            )

        // Main button element with conditional disabled attribute
        val buttonAttrs = Seq(
            cls := buttonClasses,
            attr("aria-disabled") := (if viewModel.isDisabled then "true" else "false"),
            attr(
                "hx-post"
            ) := s"/transactions/import?startDate=$startDateParam&endDate=$endDateParam",
            attr("hx-target") := "#results-panel-container",
            attr("hx-swap") := "innerHTML"
        )

        // Add disabled attribute only if disabled
        val finalAttrs = if viewModel.isDisabled then
            buttonAttrs :+ (attr("disabled") := "disabled")
        else
            buttonAttrs

        button(finalAttrs*)(
            viewModel.buttonText,
            loadingSpinner
        )
    end render
end ImportButton
