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

        // Button styling classes - using Tailwind's disabled: variant for consistent styling
        val buttonClasses =
            "px-4 py-2 rounded-md font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 " +
                "flex items-center justify-center " +
                "bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500 " +
                "disabled:bg-gray-300 disabled:text-gray-500 disabled:cursor-not-allowed disabled:hover:bg-gray-300"

        // SVG spinner definition
        val spinnerSvg = raw("""<svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    |  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    |  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    |</svg>""".stripMargin)

        // Placeholder spinner (invisible, for balanced layout)
        val placeholderSpinner =
            span(
                cls := "invisible w-4 h-4 mr-2"
            )(
                // Empty placeholder with same dimensions as the spinner
                raw(
                    """<svg class="w-4 h-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"></svg>"""
                )
            )

        // Loading spinner (visible when button has htmx-request class)
        val loadingSpinner =
            span(
                id := "loading-spinner",
                cls := "inline-block animate-spin mr-2 htmx-indicator"
            )(
                // SVG spinner icon
                spinnerSvg
            )

        // Main button element with conditional disabled attribute
        val buttonAttrs = Seq(
            cls := buttonClasses,
            attr("aria-disabled") := (if viewModel.isDisabled then "true" else "false"),
            attr(
                "hx-post"
            ) := s"/transactions/import?startDate=$startDateParam&endDate=$endDateParam",
            attr("hx-target") := "#results-panel-container",
            attr("hx-swap") := "innerHTML",
            // Disable button during request
            attr("hx-disabled-elt") := "this",
            // Add htmx classes for styling during request
            attr("hx-indicator") := "#loading-spinner"
        )

        // Add disabled attribute only if initially disabled
        val finalAttrs = if viewModel.isDisabled then
            buttonAttrs :+ (attr("disabled") := "disabled")
        else
            buttonAttrs

        button(finalAttrs*)(
            loadingSpinner,
            viewModel.buttonText,
            placeholderSpinner
        )
    end render
end ImportButton
