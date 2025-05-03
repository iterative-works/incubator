package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.StatusIndicatorViewModel
import works.iterative.incubator.budget.ui.transaction_import.models.ImportStatus
import scalatags.Text.all.*

/** Component for displaying the current status of transaction import operations. Shows appropriate
  * styling and icons based on the current status.
  */
object StatusIndicator:
    /** Renders a status indicator component.
      *
      * @param viewModel
      *   The view model containing status information
      * @return
      *   A Scalatags fragment representing the status indicator
      */
    def render(viewModel: StatusIndicatorViewModel): Frag =
        // Don't render if indicator is not visible
        if !viewModel.isVisible then
            frag()
        else
            // Extract the status as a string to use in data attribute
            val statusStr = viewModel.status.toString
            
            div(
                id := "status-indicator",
                cls := s"rounded-md px-4 py-3 flex items-center ${statusContainerClass(viewModel)}",
                // Add data attribute for current status to allow checking for changes
                attr("data-status") := statusStr,
                attr("aria-live") := "polite",
                attr("role") := "status"
            )(
                // Add all possible icons but only display the current one
                div(cls := "icon-container relative")(
                    // Not Started icon - only visible when in this state
                    div(
                        cls := s"absolute inset-0 ${if viewModel.status == ImportStatus.NotStarted then "opacity-100" else "opacity-0"}",
                        attr("data-icon") := "not-started"
                    )(
                        raw(s"""<svg class="h-5 w-5 mr-1.5 flex-shrink-0 text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                          |  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                          |</svg>""".stripMargin)
                    ),
                    
                    // In Progress icon (spinner) - only visible when in this state
                    div(
                        cls := s"absolute inset-0 transition-opacity duration-300 ${if viewModel.status == ImportStatus.InProgress then "opacity-100" else "opacity-0"}",
                        attr("data-icon") := "in-progress"
                    )(
                        raw(s"""<svg class="h-5 w-5 mr-1.5 flex-shrink-0 text-blue-600 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                          |  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                          |  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          |</svg>""".stripMargin)
                    ),
                    
                    // Completed icon - only visible when in this state
                    div(
                        cls := s"absolute inset-0 transition-opacity duration-300 ${if viewModel.status == ImportStatus.Completed then "opacity-100" else "opacity-0"}",
                        attr("data-icon") := "completed"
                    )(
                        raw(s"""<svg class="h-5 w-5 mr-1.5 flex-shrink-0 text-green-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                          |  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                          |</svg>""".stripMargin)
                    ),
                    
                    // Error icon - only visible when in this state
                    div(
                        cls := s"absolute inset-0 transition-opacity duration-300 ${if viewModel.status == ImportStatus.Error then "opacity-100" else "opacity-0"}",
                        attr("data-icon") := "error"
                    )(
                        raw(s"""<svg class="h-5 w-5 mr-1.5 flex-shrink-0 text-red-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                          |  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                          |</svg>""".stripMargin)
                    )
                ),
                span(cls := "ml-2 text-sm")(
                    viewModel.getStatusText
                )
            )
    end render

    /** Determines the appropriate CSS classes for the container based on status.
      *
      * @param viewModel
      *   The view model containing status information
      * @return
      *   String containing Tailwind CSS classes
      */
    private def statusContainerClass(viewModel: StatusIndicatorViewModel): String =
        viewModel.status match
            case ImportStatus.NotStarted => "bg-gray-100 text-gray-800"
            case ImportStatus.InProgress => "bg-blue-100 text-blue-800 animate-pulse"
            case ImportStatus.Completed  => "bg-green-100 text-green-800"
            case ImportStatus.Error      => "bg-red-100 text-red-800"
    end statusContainerClass

    /** Renders the appropriate icon for the current status.
      *
      * @param viewModel
      *   The view model containing status information
      * @return
      *   A Scalatags fragment representing the icon
      */
    private def renderIcon(viewModel: StatusIndicatorViewModel): Frag =
        val svgBaseClasses = "h-5 w-5 mr-1.5 flex-shrink-0"

        viewModel.status match
            case ImportStatus.NotStarted =>
                // Info icon
                raw(s"""<svg class="$svgBaseClasses text-gray-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    |  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                    |</svg>""".stripMargin)

            case ImportStatus.InProgress =>
                // Loading spinner icon (animated)
                raw(s"""<svg class="$svgBaseClasses text-blue-600 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    |  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    |  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    |</svg>""".stripMargin)

            case ImportStatus.Completed =>
                // Success (checkmark) icon
                raw(s"""<svg class="$svgBaseClasses text-green-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    |  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    |</svg>""".stripMargin)

            case ImportStatus.Error =>
                // Error (x) icon
                raw(s"""<svg class="$svgBaseClasses text-red-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    |  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                    |</svg>""".stripMargin)
        end match
    end renderIcon
end StatusIndicator
