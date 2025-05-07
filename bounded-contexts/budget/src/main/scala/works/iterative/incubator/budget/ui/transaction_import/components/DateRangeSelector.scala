package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.DateRangeSelectorViewModel
import scalatags.Text.all.*
import java.time.format.DateTimeFormatter

/** Component for selecting a date range for transaction imports.
  * Displays date inputs for start and end dates and error messages if any.
  */
object DateRangeSelector:
    /** Renders a date range selector component.
      *
      * @param viewModel
      *   The view model containing date range information
      * @return
      *   A Scalatags fragment representing the date range selector
      */
    def render(viewModel: DateRangeSelectorViewModel): Frag =
        val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
        val today = viewModel.maxDate.format(dateFormat)

        val startDateValue =
            if viewModel.startDate != null then viewModel.startDate.format(dateFormat) else ""
        val endDateValue =
            if viewModel.endDate != null then viewModel.endDate.format(dateFormat) else ""

        // Common input classes
        val inputClasses =
            "w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        val labelClasses = "block text-sm font-medium text-gray-700 mb-1"

        // Border styling based on validation state
        val startDateBorderClasses =
            if viewModel.hasError then s"$inputClasses border-red-500" else inputClasses
        val endDateBorderClasses =
            if viewModel.hasError then s"$inputClasses border-red-500" else inputClasses

        div(
            cls := "w-full",
            id := "date-range-selector"
        )(
            h3(cls := "text-lg font-medium mb-2")("Select Date Range for Import"),
            div(
                cls := "flex flex-col sm:flex-row items-center space-y-2 sm:space-y-0 sm:space-x-4 w-full"
            )(
                div(
                    cls := "flex-grow w-full",
                    label(`for` := "startDate", cls := s"$labelClasses")("From:"),
                    input(
                        `type` := "date",
                        id := "startDate",
                        name := "startDate",
                        cls := startDateBorderClasses,
                        value := startDateValue,
                        attr("max") := today,
                        // Add HTMX attributes for real-time form update
                        attr("hx-post") := "/transactions/import/submit",
                        attr("hx-trigger") := "change",
                        attr("hx-target") := "#transaction-import-container",
                        attr("hx-swap") := "outerHTML",
                        // Add HTMX indicator for which field triggered the change
                        attr("hx-include") := "[name='_triggeredBy']",
                        attr("hx-vals") := """{"_triggeredBy": "startDate"}"""
                    )
                ),
                div(
                    cls := "flex-grow w-full",
                    label(`for` := "endDate", cls := s"$labelClasses")(
                        "To:"
                    ),
                    input(
                        `type` := "date",
                        id := "endDate",
                        name := "endDate",
                        cls := endDateBorderClasses,
                        value := endDateValue,
                        attr("max") := today,
                        // Add HTMX attributes for real-time form update
                        attr("hx-post") := "/transactions/import/submit",
                        attr("hx-trigger") := "change",
                        attr("hx-target") := "#transaction-import-container",
                        attr("hx-swap") := "outerHTML",
                        // Add HTMX indicator for which field triggered the change
                        attr("hx-include") := "[name='_triggeredBy']",
                        attr("hx-vals") := """{"_triggeredBy": "endDate"}"""
                    )
                )
            ),

            // Error message display
            div(
                cls := "text-sm text-red-600 mt-2",
                style := (if viewModel.hasError then "display: block" else "display: none")
            )(
                viewModel.validationError.getOrElse("")
            )
        )
    end render
end DateRangeSelector
