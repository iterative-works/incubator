package works.iterative.incubator.ui.preview

import works.iterative.tapir.BaseUri
import scalatags.Text.all.*
import zio.*
import works.iterative.incubator.budget.ui.transaction_import.models.AccountSelectorViewModel
import works.iterative.incubator.budget.ui.transaction_import.components.AccountSelector

/** Preview module for the AccountSelector component Shows the component in various states for
  * testing and development
  */
class AccountSelectorPreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends ComponentStatePreviewModule[AccountSelectorViewModel]:

    override def basePath: List[String] = List("transaction-import", "account-selector")
    override def title: String = "Account Selector"

    // Create various states for the account selector
    val defaultState = ComponentState(
        name = "default",
        description = "Default state with multiple accounts and no selection",
        viewModel = AccountSelectorViewModel(
            accounts = AccountSelectorViewModel.defaultAccounts,
            selectedAccountId = None,
            validationError = None
        )
    )

    val selectedState = ComponentState(
        name = "selected",
        description = "With an account selected",
        viewModel = AccountSelectorViewModel(
            accounts = AccountSelectorViewModel.defaultAccounts,
            selectedAccountId = Some("0100-1234567890"),
            validationError = None
        )
    )

    val errorState = ComponentState(
        name = "with-error",
        description = "With validation error",
        viewModel = AccountSelectorViewModel(
            accounts = AccountSelectorViewModel.defaultAccounts,
            selectedAccountId = None,
            validationError = Some("Please select an account to continue")
        )
    )

    val emptyState = ComponentState(
        name = "empty",
        description = "No accounts available",
        viewModel = AccountSelectorViewModel(
            accounts = List.empty,
            selectedAccountId = None,
            validationError = Some("No accounts available. Please add an account first.")
        )
    )

    val disabledState = ComponentState(
        name = "disabled",
        description = "Component is disabled during import process",
        viewModel = AccountSelectorViewModel(
            accounts = AccountSelectorViewModel.defaultAccounts,
            selectedAccountId = Some("0100-1234567890"),
            validationError = None
        )
        // Note: In a real implementation, we'd have a 'disabled' property in the ViewModel
    )

    // This needs to be defined before the trait initialization
    override def states: List[ComponentState[AccountSelectorViewModel]] = List(
        defaultState,
        selectedState,
        errorState,
        emptyState,
        disabledState
    )

    /** Find a state by name */
    private def findState(name: String): Option[ComponentState[AccountSelectorViewModel]] =
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
                        "This preview shows the AccountSelector component in various states for development and testing purposes."
                    ),

                    // Component description
                    div(
                        cls := "bg-blue-50 p-4 rounded-lg mb-6",
                        h4(cls := "font-medium text-blue-800 mb-2", "Component Information"),
                        p(
                            cls := "text-blue-700 mb-2",
                            "The AccountSelector component allows users to select an account for transaction imports."
                        ),
                        p(
                            cls := "text-blue-700",
                            "It handles validation and provides feedback for error states."
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
                        AccountSelector.render(defaultState.viewModel)
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
                                    AccountSelector.render(state.viewModel)
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
                                            s"""AccountSelectorViewModel(
                                               |  accounts = ${state.viewModel.accounts.map(a =>
                                                  s"AccountOption(${a.id}, ${a.name})"
                                              ).mkString("[", ", ", "]")},
                                               |  selectedAccountId = ${state.viewModel.selectedAccountId},
                                               |  validationError = ${state.viewModel.validationError}
                                               |)
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
end AccountSelectorPreviewModule
