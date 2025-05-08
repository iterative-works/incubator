package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.ImportButtonViewModel
import scalatags.Text.all.*

/** Component for triggering transaction import with loading state.
  *
  * Category: View
  * Layer: UI/Presentation
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

        // Button content based on state
        val buttonContent = if viewModel.isLoading then
            // If already in loading state, show spinner
            frag(
                span(
                    cls := "inline-block animate-spin mr-2"
                )(
                    // SVG spinner icon
                    spinnerSvg
                ),
                "Importing..."
            )
        else 
            // Normal state with HTMX loading indicator
            frag(
                // This spinner only shows during HTMX requests
                span(
                    cls := "inline-block animate-spin mr-2 htmx-indicator",
                    style := "display: none;" // Initially hidden, htmx will show it during requests
                )(
                    // SVG spinner icon
                    spinnerSvg
                ),
                // Button text changes during HTMX requests
                span(
                    cls := "htmx-indicator",
                    style := "display: none;" // Initially hidden, htmx will show it during requests
                )(
                    "Importing..."
                ),
                // Normal text (hidden during HTMX requests)
                span(
                    cls := "htmx-indicator-inverse" // Will be hidden when htmx-indicator is shown
                )(
                    "Import Transactions"
                )
            )

        // Main button element with conditional disabled attribute
        val buttonAttrs = Seq(
            `type` := "submit",
            cls := buttonClasses,
            attr("aria-disabled") := (if viewModel.isDisabled then "true" else "false")
        )

        // Add disabled attribute only if initially disabled
        val finalAttrs = if viewModel.isDisabled then
            buttonAttrs :+ (disabled := "disabled")
        else
            buttonAttrs

        button(finalAttrs*)(
            // Use the dynamic button content we created
            buttonContent,
            // Keep the placeholder for layout balance
            placeholderSpinner
        )
    end render
end ImportButton
