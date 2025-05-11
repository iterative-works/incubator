package works.iterative.incubator.ui.preview

import works.iterative.tapir.BaseUri
import scalatags.Text.all.*
import zio.*
import works.iterative.incubator.budget.ui.transaction_import.models.StatusIndicatorViewModel
import works.iterative.incubator.budget.ui.transaction_import.models.ImportStatus
import works.iterative.incubator.budget.ui.transaction_import.components.StatusIndicator

/** Preview module for the StatusIndicator component Shows the component in various states for
  * development and testing
  */
class StatusIndicatorPreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends ComponentStatePreviewModule[StatusIndicatorViewModel]:

    override def basePath: List[String] = List("transaction-import", "status-indicator")
    override def title: String = "Status Indicator"

    // Create various states for the status indicator
    val notStartedState = ComponentState(
        name = "not-started",
        description = "Initial state before import is started",
        viewModel = StatusIndicatorViewModel(
            status = ImportStatus.NotStarted,
            isVisible = true
        )
    )

    val inProgressState = ComponentState(
        name = "in-progress",
        description = "While import is in progress (animated)",
        viewModel = StatusIndicatorViewModel(
            status = ImportStatus.InProgress,
            isVisible = true
        )
    )

    val completedState = ComponentState(
        name = "completed",
        description = "After import has completed successfully",
        viewModel = StatusIndicatorViewModel(
            status = ImportStatus.Completed,
            isVisible = true
        )
    )

    val errorState = ComponentState(
        name = "error",
        description = "When import has failed",
        viewModel = StatusIndicatorViewModel(
            status = ImportStatus.Error,
            isVisible = true
        )
    )

    val hiddenState = ComponentState(
        name = "hidden",
        description = "Status indicator not visible",
        viewModel = StatusIndicatorViewModel(
            status = ImportStatus.NotStarted,
            isVisible = false
        )
    )

    // This needs to be defined before the trait initialization
    override def states: List[ComponentState[StatusIndicatorViewModel]] = List(
        notStartedState,
        inProgressState,
        completedState,
        errorState,
        hiddenState
    )

    /** Find a state by name */
    private def findState(name: String): Option[ComponentState[StatusIndicatorViewModel]] =
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
                        "This preview shows the StatusIndicator component in various states for development and testing purposes."
                    ),

                    // Component description
                    div(
                        cls := "bg-blue-50 p-4 rounded-lg mb-6",
                        h4(cls := "font-medium text-blue-800 mb-2", "Component Information"),
                        p(
                            cls := "text-blue-700 mb-2",
                            "The StatusIndicator component displays the current status of transaction import operations."
                        ),
                        p(
                            cls := "text-blue-700",
                            "It shows different visual indicators based on the operation status (not started, in progress, completed, error)."
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
                        cls := "preview-container bg-gray-50 p-4",
                        StatusIndicator.render(notStartedState.viewModel)
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
                                    cls := "preview-container bg-gray-50 p-4 mb-6",
                                    StatusIndicator.render(state.viewModel)
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
                                            s"""StatusIndicatorViewModel(
                                               |  status = ${state.viewModel.status},
                                               |  isVisible = ${state.viewModel.isVisible}
                                               |)
                                               |
                                               |showLoadingSpinner = ${state.viewModel.showLoadingSpinner}
                                               |showSuccessIcon = ${state.viewModel.showSuccessIcon}
                                               |showErrorIcon = ${state.viewModel.showErrorIcon}
                                               |getStatusText = "${state.viewModel.getStatusText}"
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
end StatusIndicatorPreviewModule
