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
            // Start polling the status endpoint when import begins
            attr("hx-on::before-request") := """
              // Create a custom status updater function
              window.updateImportStatus = async function() {
                try {
                  // Fetch the status data
                  const response = await fetch('/transactions/import/status');
                  const html = await response.text();
                  
                  // Extract the status from the HTML
                  const parser = new DOMParser();
                  const doc = parser.parseFromString(html, 'text/html');
                  const newStatus = doc.querySelector('#status-indicator')?.getAttribute('data-status');
                  const statusText = doc.querySelector('#status-indicator span')?.innerText;
                  
                  if (newStatus && statusText) {
                    // Get the current status element
                    const statusContainer = document.querySelector('#status-indicator-container');
                    if (!statusContainer) return;
                    
                    // If there's no status indicator yet, replace the entire container
                    if (!document.querySelector('#status-indicator')) {
                      statusContainer.innerHTML = html;
                      return;
                    }
                    
                    // Update just the status text without touching the spinner
                    const statusElement = document.querySelector('#status-indicator');
                    const textElement = statusElement.querySelector('span');
                    if (textElement) textElement.innerText = statusText;
                    
                    // Update the status data attribute (which controls background color via CSS)
                    statusElement.setAttribute('data-status', newStatus);
                    
                    // Update the container class based on status
                    if (newStatus === 'InProgress') {
                      statusElement.className = statusElement.className.replace(/bg-[a-z]+-100/g, 'bg-blue-100');
                      statusElement.className = statusElement.className.replace(/text-[a-z]+-800/g, 'text-blue-800');
                      statusElement.className += ' animate-pulse';
                    } else if (newStatus === 'Completed') {
                      statusElement.className = statusElement.className.replace(/bg-[a-z]+-100/g, 'bg-green-100');
                      statusElement.className = statusElement.className.replace(/text-[a-z]+-800/g, 'text-green-800');
                      statusElement.className = statusElement.className.replace(/animate-pulse/g, '');
                    } else if (newStatus === 'Error') {
                      statusElement.className = statusElement.className.replace(/bg-[a-z]+-100/g, 'bg-red-100');
                      statusElement.className = statusElement.className.replace(/text-[a-z]+-800/g, 'text-red-800');
                      statusElement.className = statusElement.className.replace(/animate-pulse/g, '');
                    }
                    
                    // Show the appropriate icon
                    document.querySelectorAll('[data-icon]').forEach(icon => {
                      if (icon.getAttribute('data-icon') === 'in-progress' && newStatus === 'InProgress') {
                        icon.classList.remove('opacity-0');
                        icon.classList.add('opacity-100');
                      } else if (icon.getAttribute('data-icon') === 'completed' && newStatus === 'Completed') {
                        icon.classList.remove('opacity-0');
                        icon.classList.add('opacity-100');
                      } else if (icon.getAttribute('data-icon') === 'error' && newStatus === 'Error') {
                        icon.classList.remove('opacity-0');
                        icon.classList.add('opacity-100');
                      } else if (icon.getAttribute('data-icon') === 'not-started' && newStatus === 'NotStarted') {
                        icon.classList.remove('opacity-0');
                        icon.classList.add('opacity-100');
                      } else {
                        icon.classList.remove('opacity-100');
                        icon.classList.add('opacity-0');
                      }
                    });
                  }
                } catch (error) {
                  console.error("Error updating status:", error);
                }
              };
              
              // Create a status poller that updates every 500ms
              window.statusPoller = setInterval(window.updateImportStatus, 500);
              
              // Initial status update
              window.updateImportStatus();
            """,
            // Stop polling when import completes or fails
            attr("hx-on::after-request") := """
              // Stop polling the status endpoint
              clearInterval(window.statusPoller);
              // Update status one final time to ensure latest state is shown
              window.updateImportStatus();
            """,
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
