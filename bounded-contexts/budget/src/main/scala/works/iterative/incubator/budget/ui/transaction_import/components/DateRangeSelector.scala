package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.DateRangeSelectorViewModel
import scalatags.Text.all.*
import java.time.format.DateTimeFormatter

/** Component for selecting a date range for transaction imports. Validates that:
  *   - Start date is not after end date
  *   - Dates are not in the future
  *   - Range is not greater than 90 days (Fio Bank API limitation)
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
            id := "date-range-selector",
            attr("hx-target") := "#date-range-selector",
            attr("hx-swap") := "outerHTML"
        )(
            h3(cls := "text-lg font-medium mb-2")("Select Date Range for Import"),
            div(
                cls := "flex flex-col sm:flex-row items-center space-y-2 sm:space-y-0 sm:space-x-4 w-full"
            )(
                div(
                    cls := "flex-grow w-full",
                    label(`for` := "start-date", cls := s"$labelClasses")("From:"),
                    input(
                        `type` := "date",
                        id := "start-date",
                        name := "startDate",
                        cls := startDateBorderClasses,
                        value := startDateValue,
                        attr("hx-post") := "/transactions/import/validate-dates",
                        attr("hx-trigger") := "change",
                        attr("hx-include") := "#end-date",
                        attr("max") := today
                    )
                ),
                div(
                    cls := "flex-grow w-full",
                    label(`for` := "end-date", cls := s"$labelClasses")(
                        "To:"
                    ),
                    input(
                        `type` := "date",
                        id := "end-date",
                        name := "endDate",
                        cls := endDateBorderClasses,
                        value := endDateValue,
                        attr("hx-post") := "/transactions/import/validate-dates",
                        attr("hx-trigger") := "change",
                        attr("hx-include") := "#start-date",
                        attr("max") := today
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
