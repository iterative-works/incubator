package works.iterative.incubator.ui.preview

import works.iterative.tapir.BaseUri
import scalatags.Text.all.*
import zio.*
import works.iterative.incubator.budget.ui.transaction_import.models.ImportButtonViewModel
import works.iterative.incubator.budget.ui.transaction_import.components.ImportButton
import java.time.LocalDate

/** Preview module for the ImportButton component. Shows the component in various states for testing
  * and development.
  */
class ImportButtonPreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends ComponentStatePreviewModule[ImportButtonViewModel]:

    override def basePath: List[String] = List("transaction-import", "import-button")
    override def title: String = "Import Button"

    // Create various states for the import button
    val defaultState = ComponentState(
        name = "default",
        description = "Default enabled state ready for import",
        viewModel = ImportButtonViewModel(
            isEnabled = true,
            isLoading = false,
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now()
        )
    )

    val disabledState = ComponentState(
        name = "disabled",
        description = "Disabled state when import is not available",
        viewModel = ImportButtonViewModel(
            isEnabled = false,
            isLoading = false,
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now()
        )
    )

    val loadingState = ComponentState(
        name = "loading",
        description = "Loading state during import operation",
        viewModel = ImportButtonViewModel(
            isEnabled = true,
            isLoading = true,
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now()
        )
    )

    val disabledLoadingState = ComponentState(
        name = "disabled-loading",
        description = "Edge case: disabled and loading simultaneously",
        viewModel = ImportButtonViewModel(
            isEnabled = false,
            isLoading = true,
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now()
        )
    )

    // This needs to be defined before the trait initialization
    override def states: List[ComponentState[ImportButtonViewModel]] = List(
        defaultState,
        disabledState,
        loadingState,
        disabledLoadingState
    )

    /** Find a state by name */
    private def findState(name: String): Option[ComponentState[ImportButtonViewModel]] =
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
                        "This preview shows the ImportButton component in various states for development and testing purposes."
                    ),

                    // Component description
                    div(
                        cls := "bg-blue-50 p-4 rounded-lg mb-6",
                        h4(cls := "font-medium text-blue-800 mb-2", "Component Information"),
                        p(
                            cls := "text-blue-700 mb-2",
                            "The ImportButton component triggers transaction imports from connected bank accounts."
                        ),
                        p(
                            cls := "text-blue-700",
                            "It displays a loading spinner during import operations and can be disabled based on application state."
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
                        ImportButton.render(defaultState.viewModel)
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
                                    ImportButton.render(state.viewModel)
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
                                            s"""ImportButtonViewModel(
                                               |  isEnabled = ${state.viewModel.isEnabled},
                                               |  isLoading = ${state.viewModel.isLoading},
                                               |  startDate = ${state.viewModel.startDate},
                                               |  endDate = ${state.viewModel.endDate}
                                               |)
                                               |
                                               |buttonText = "${state.viewModel.buttonText}"
                                               |isDisabled = ${state.viewModel.isDisabled}
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
end ImportButtonPreviewModule
