package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.ResultsPanelViewModel
import scalatags.Text.all.*
import java.time.format.DateTimeFormatter

/** Component for displaying transaction import results. Shows appropriate success or error
  * messages, transaction counts, and action buttons.
  */
object ResultsPanel:
    /** Renders a results panel component.
      *
      * @param viewModel
      *   The view model containing import results information
      * @return
      *   A Scalatags fragment representing the results panel
      */
    def render(viewModel: ResultsPanelViewModel): Frag =
        // Don't render if panel is not visible
        if !viewModel.isVisible then
            frag()
        else
            val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            val formattedStart = viewModel.startDate.format(dateFormat)
            val formattedEnd = viewModel.endDate.format(dateFormat)
            val dateRange = s"$formattedStart to $formattedEnd"

            div(
                id := "import-results",
                cls := "rounded-lg shadow-md mt-6",
                attr("aria-live") := "polite",
                attr("role") := "region",
                attr("aria-labelledby") := "results-heading"
            )(
                // Results header with background color based on success/error
                div(
                    cls := s"px-4 py-3 rounded-t-lg ${headerClass(viewModel)}",
                    id := "results-heading"
                )(
                    headerContent(viewModel)
                ),
                // Results content
                div(
                    cls := "px-4 py-3 bg-white rounded-b-lg"
                )(
                    // Date range information
                    div(cls := "text-sm text-gray-600 mb-2")(
                        s"Date range: $dateRange"
                    ),

                    // Success summary section
                    if viewModel.showSuccessSummary then
                        div(cls := "mt-3")(
                            transactionSummary(viewModel),
                            // Completion time if available
                            viewModel.completionTimeSeconds.map { seconds =>
                                div(cls := "text-xs text-gray-500 mt-1")(
                                    s"Completed in $seconds seconds"
                                )
                            }.getOrElse(frag()),
                            // View transactions button
                            div(cls := "mt-4")(
                                a(
                                    href := "/transactions",
                                    cls := "inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                                )(
                                    "View Transactions"
                                )
                            )
                        )
                    else frag(),

                    // Error message section
                    if viewModel.showErrorMessage then
                        div(cls := "mt-3")(
                            // Error message
                            div(cls := "text-sm text-red-600 mb-2")(
                                viewModel.errorMessage.getOrElse("An unknown error occurred")
                            ),
                            // Error code for support
                            div(cls := "text-xs text-gray-500 mb-3")(
                                s"Error code: ${viewModel.errorCode}"
                            ),
                            // Retry button
                            if viewModel.showRetryButton then
                                button(
                                    cls := "inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500",
                                    attr(
                                        "hx-post"
                                    ) := s"/import-transactions?startDate=${viewModel.startDate}&endDate=${viewModel.endDate}",
                                    attr("hx-target") := "#import-results",
                                    attr("hx-swap") := "outerHTML"
                                )(
                                    "Retry Import"
                                )
                            else frag()
                        )
                    else frag()
                )
            )
    end render

    /** Determines the appropriate CSS classes for the header based on result status.
      *
      * @param viewModel
      *   The view model containing result information
      * @return
      *   String containing Tailwind CSS classes
      */
    private def headerClass(viewModel: ResultsPanelViewModel): String =
        if viewModel.showSuccessSummary then "bg-green-100 text-green-800"
        else if viewModel.showErrorMessage then "bg-red-100 text-red-800"
        else "bg-gray-100 text-gray-700"
    end headerClass

    /** Creates the header content based on result status.
      *
      * @param viewModel
      *   The view model containing result information
      * @return
      *   A Scalatags fragment for the header
      */
    private def headerContent(viewModel: ResultsPanelViewModel): Frag =
        val svgBaseClasses = "h-5 w-5 mr-2 flex-shrink-0"

        div(cls := "flex items-center")(
            // Icon - success or error
            if viewModel.showSuccessSummary then
                // Success (checkmark) icon
                raw(s"""<svg class="$svgBaseClasses text-green-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    |  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
                    |</svg>""".stripMargin)
            else if viewModel.showErrorMessage then
                // Error (x) icon
                raw(s"""<svg class="$svgBaseClasses text-red-600" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                    |  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                    |</svg>""".stripMargin)
            else frag(),

            // Header text
            h3(cls := "text-lg font-medium")(
                if viewModel.showSuccessSummary then
                    "Import Successful"
                else if viewModel.showErrorMessage then
                    "Import Failed"
                else
                    "Import Results"
            )
        )
    end headerContent

    /** Creates transaction summary text based on transaction count.
      *
      * @param viewModel
      *   The view model containing result information
      * @return
      *   A Scalatags fragment for the transaction summary
      */
    private def transactionSummary(viewModel: ResultsPanelViewModel): Frag =
        val count = viewModel.transactionCount

        if count == 0 then
            div(cls := "text-sm")(
                "No transactions found for the selected date range"
            )
        else if count == 1 then
            div(cls := "text-sm")(
                "1 transaction successfully imported"
            )
        else
            div(cls := "text-sm")(
                s"$count transactions successfully imported"
            )
        end if
    end transactionSummary
end ResultsPanel
