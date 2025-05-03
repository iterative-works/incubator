package works.iterative.incubator.ui.preview

import works.iterative.tapir.BaseUri
import scalatags.Text.all.*
import zio.*
import works.iterative.incubator.budget.ui.transaction_import.models.DateRangeSelectorViewModel
import works.iterative.incubator.budget.ui.transaction_import.components.DateRangeSelector
import java.time.LocalDate

/** Preview module for the DateRangeSelector component Shows the component in various states for
  * testing and development
  */
class DateRangeSelectorPreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends ComponentStatePreviewModule[DateRangeSelectorViewModel]:

    override def basePath: List[String] = List("transaction-import", "date-range-selector")
    override def title: String = "Date Range Selector"

    // Create various states for the date range selector
    val defaultState = ComponentState(
        name = "default",
        description = "Default state with current month selected",
        viewModel = DateRangeSelectorViewModel(
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now(),
            validationError = None
        )
    )

    val errorState = ComponentState(
        name = "with-error",
        description = "With validation error (start date after end date)",
        viewModel = DateRangeSelectorViewModel(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().minusDays(5),
            validationError = Some("Start date cannot be after end date")
        )
    )

    val rangeTooLongState = ComponentState(
        name = "range-too-long",
        description = "Range exceeding 90 days (Fio Bank API limitation)",
        viewModel = DateRangeSelectorViewModel(
            startDate = LocalDate.now().minusDays(100),
            endDate = LocalDate.now(),
            validationError = Some("Date range cannot exceed 90 days (Fio Bank API limitation)")
        )
    )

    val futureDatesState = ComponentState(
        name = "future-dates",
        description = "With dates in the future (not allowed)",
        viewModel = DateRangeSelectorViewModel(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(10),
            validationError = Some("Dates cannot be in the future")
        )
    )

    // This needs to be defined before the trait initialization
    override def states: List[ComponentState[DateRangeSelectorViewModel]] = List(
        defaultState,
        errorState,
        rangeTooLongState,
        futureDatesState
    )

    /** Find a state by name */
    private def findState(name: String): Option[ComponentState[DateRangeSelectorViewModel]] =
        states.find(_.name == name)

    /** Render the list of available states for this component */
    override def renderStatesList: ZIO[Any, String, Frag] =
        ZIO.succeed {
            appShell.wrap(
                pageTitle = title,
                content = div(
                    h3(cls := "text-xl font-semibold mb-4", s"$title Component Preview"),
                    p(
                        cls := "mb-6",
                        "This preview shows the DateRangeSelector component in various states for development and testing purposes."
                    ),

                    // Component description
                    div(
                        cls := "bg-blue-50 p-4 rounded-lg mb-6",
                        h4(cls := "font-medium text-blue-800 mb-2", "Component Information"),
                        p(
                            cls := "text-blue-700 mb-2",
                            "The DateRangeSelector component allows users to select a start and end date for transaction imports."
                        ),
                        p(
                            cls := "text-blue-700",
                            "It includes validation for date ranges according to business rules and API limitations."
                        )
                    ),

                    // Available states
                    h4(cls := "text-lg font-semibold mb-3", "Available States:"),
                    ul(
                        cls := "space-y-2 mb-6",
                        states.map { state =>
                            li(
                                a(
                                    href := s"/preview/${basePath.mkString("/")}/${state.name}",
                                    cls := "text-blue-600 hover:underline",
                                    state.name
                                ),
                                span(cls := "text-gray-600 ml-2", s"- ${state.description}")
                            )
                        }
                    ),

                    // Default component preview
                    h4(cls := "text-lg font-semibold mb-3", "Default Preview:"),
                    div(
                        cls := "preview-container bg-gray-50",
                        DateRangeSelector.render(defaultState.viewModel)
                    )
                ),
                currentPath = currentPath
            )
        }

    /** Render a specific component state */
    override def renderState(stateName: String): ZIO[Any, String, Frag] =
        ZIO.succeed {
            val stateOpt = findState(stateName)

            appShell.wrap(
                pageTitle = s"$title - ${stateOpt.map(_.name).getOrElse("Unknown State")}",
                content = div(
                    // Breadcrumb navigation
                    div(
                        cls := "text-sm text-gray-500 mb-4",
                        a(
                            href := s"/preview/${basePath.mkString("/")}",
                            cls := "hover:underline",
                            title
                        ),
                        span(" > "),
                        span(stateOpt.map(_.name).getOrElse("Unknown State"))
                    ),
                    stateOpt match
                        case Some(state) =>
                            frag(
                                h3(cls := "text-xl font-semibold mb-2", state.name),
                                p(cls := "text-gray-600 mb-4", state.description),

                                // Component preview
                                div(
                                    cls := "preview-container bg-gray-50 mb-6",
                                    DateRangeSelector.render(state.viewModel)
                                ),

                                // View model details
                                div(
                                    cls := "mt-8 border-t pt-4",
                                    h4(
                                        cls := "text-lg font-semibold mb-2",
                                        "View Model Properties:"
                                    ),
                                    pre(
                                        cls := "bg-gray-100 p-4 rounded overflow-x-auto text-sm",
                                        code(
                                            s"""DateRangeSelectorViewModel(
                                               |  startDate = ${state.viewModel.startDate},
                                               |  endDate = ${state.viewModel.endDate},
                                               |  validationError = ${state.viewModel.validationError}
                                               |)
                                               |
                                               |isValid = ${state.viewModel.isValid}
                                               |hasError = ${state.viewModel.hasError}
                                               |maxDate = ${state.viewModel.maxDate}
                                               |""".stripMargin
                                        )
                                    )
                                ),

                                // Navigation between states
                                div(
                                    cls := "mt-6 pt-4 border-t",
                                    h4(cls := "text-lg font-semibold mb-2", "Try Other States:"),
                                    ul(
                                        cls := "flex flex-wrap gap-2",
                                        states.filterNot(_.name == state.name).map { otherState =>
                                            li(
                                                a(
                                                    href := s"/preview/${basePath.mkString("/")}/${otherState.name}",
                                                    cls := "px-3 py-1 bg-gray-200 hover:bg-gray-300 rounded text-sm",
                                                    otherState.name
                                                )
                                            )
                                        }
                                    )
                                )
                            )

                        case None =>
                            div(
                                cls := "p-4 bg-red-100 text-red-700 rounded",
                                "Error: State not found. ",
                                a(
                                    href := s"/preview/${basePath.mkString("/")}",
                                    cls := "underline",
                                    "Return to states list"
                                )
                            )
                ),
                currentPath = currentPath
            )
        }
end DateRangeSelectorPreviewModule
