package works.iterative.incubator.ui.preview

import scalatags.Text.all.*
import works.iterative.tapir.BaseUri
import zio.*

import works.iterative.incubator.budget.ui.transaction_import.models.ResultsPanelViewModel
import works.iterative.incubator.budget.ui.transaction_import.models.ImportResults
import works.iterative.incubator.budget.ui.transaction_import.components.ResultsPanel

import java.time.LocalDate
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Preview module for the ResultsPanel component showing various states and scenarios */
class ResultsPanelPreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends ComponentStatePreviewModule[ResultsPanelViewModel]:

    override def basePath: List[String] = List("transaction-import", "results-panel")
    override def title: String = "Results Panel Component Preview"

    private val today = LocalDate.now()
    private val oneMonthAgo = today.minusMonths(1)
    private val startTime = Instant.now().minus(5, ChronoUnit.SECONDS)
    private val endTime = Instant.now()

    // Default state - panel not visible
    private val defaultState = ComponentState(
        name = "default-hidden",
        description = "The panel is not visible (initial state before import)",
        viewModel = ResultsPanelViewModel(
            importResults = None,
            isVisible = false,
            startDate = oneMonthAgo,
            endDate = today
        )
    )

    // Success state with multiple transactions
    private val successState = ComponentState(
        name = "success",
        description = "Shows a successful import with multiple transactions",
        viewModel = ResultsPanelViewModel(
            importResults = Some(ImportResults(
                transactionCount = 42,
                errorMessage = None,
                startTime = startTime,
                endTime = Some(endTime)
            )),
            isVisible = true,
            startDate = oneMonthAgo,
            endDate = today
        )
    )

    // Empty result state - no transactions found
    private val emptyResultState = ComponentState(
        name = "empty-result",
        description = "Shows when import completed successfully but found no transactions",
        viewModel = ResultsPanelViewModel(
            importResults = Some(ImportResults(
                transactionCount = 0,
                errorMessage = None,
                startTime = startTime,
                endTime = Some(endTime)
            )),
            isVisible = true,
            startDate = oneMonthAgo,
            endDate = today
        )
    )

    // Single transaction state
    private val singleTransactionState = ComponentState(
        name = "single-transaction",
        description = "Shows a successful import with exactly one transaction",
        viewModel = ResultsPanelViewModel(
            importResults = Some(ImportResults(
                transactionCount = 1,
                errorMessage = None,
                startTime = startTime,
                endTime = Some(endTime)
            )),
            isVisible = true,
            startDate = oneMonthAgo,
            endDate = today
        )
    )

    // Error state
    private val errorState = ComponentState(
        name = "error",
        description = "Shows an error state when import fails",
        viewModel = ResultsPanelViewModel(
            importResults = Some(ImportResults(
                transactionCount = 0,
                errorMessage =
                    Some("Unable to connect to the transaction service. Please try again later."),
                startTime = startTime,
                endTime = Some(endTime)
            )),
            isVisible = true,
            startDate = oneMonthAgo,
            endDate = today
        )
    )

    // Authentication error state
    private val authErrorState = ComponentState(
        name = "authentication-error",
        description = "Shows an error related to authentication issues",
        viewModel = ResultsPanelViewModel(
            importResults = Some(ImportResults(
                transactionCount = 0,
                errorMessage =
                    Some("Authentication failed. Please reconnect your account in Settings."),
                startTime = startTime,
                endTime = Some(endTime)
            )),
            isVisible = true,
            startDate = oneMonthAgo,
            endDate = today
        )
    )

    // List of all available states
    override def states: List[ComponentState[ResultsPanelViewModel]] = List(
        defaultState,
        successState,
        emptyResultState,
        singleTransactionState,
        errorState,
        authErrorState
    )

    // Helper method to find a state by name
    private def findState(name: String): Option[ComponentState[ResultsPanelViewModel]] =
        states.find(_.name == name)

    // Render the component using the ResultsPanel component
    private def renderComponent(viewModel: ResultsPanelViewModel): Frag =
        ResultsPanel.render(viewModel)

    // Render property details for debugging
    private def renderPropertyDetails(viewModel: ResultsPanelViewModel): Frag =
        table(cls := "w-full text-sm text-left")(
            thead(cls := "text-xs text-gray-700 bg-gray-100")(
                tr(
                    th(cls := "px-4 py-2 w-1/3")("Property"),
                    th(cls := "px-4 py-2")("Value")
                )
            ),
            tbody(
                tr(
                    td(cls := "px-4 py-2 font-medium")("isVisible"),
                    td(cls := "px-4 py-2")(viewModel.isVisible.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("startDate"),
                    td(cls := "px-4 py-2")(viewModel.startDate.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("endDate"),
                    td(cls := "px-4 py-2")(viewModel.endDate.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("transactionCount"),
                    td(cls := "px-4 py-2")(viewModel.transactionCount.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("showSuccessSummary"),
                    td(cls := "px-4 py-2")(viewModel.showSuccessSummary.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("showErrorMessage"),
                    td(cls := "px-4 py-2")(viewModel.showErrorMessage.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("showRetryButton"),
                    td(cls := "px-4 py-2")(viewModel.showRetryButton.toString)
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("errorMessage"),
                    td(cls := "px-4 py-2")(viewModel.errorMessage.getOrElse("None"))
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("completionTimeSeconds"),
                    td(cls := "px-4 py-2")(
                        viewModel.completionTimeSeconds.map(_.toString).getOrElse("None")
                    )
                ),
                tr(
                    td(cls := "px-4 py-2 font-medium")("errorCode"),
                    td(cls := "px-4 py-2")(viewModel.errorCode)
                )
            )
        )

    // Render the list of available states
    override def renderStatesList: ZIO[Any, String, Frag] =
        ZIO.succeed {
            appShell.wrap(
                pageTitle = s"$title - States",
                content = div(
                    h3(cls := "text-xl font-semibold mb-4")(title),
                    p(cls := "mb-6")(
                        "This component displays the results of a transaction import operation, including success " +
                            "or error messages, transaction counts, and provides action buttons for retry or viewing transactions."
                    ),

                    // States list
                    div(cls := "bg-white shadow overflow-hidden rounded-md")(
                        ul(cls := "divide-y divide-gray-200")(
                            states.map { state =>
                                li(
                                    a(
                                        href := s"/preview/${basePath.mkString("/")}/${state.name}",
                                        cls := "block hover:bg-gray-50 p-4"
                                    )(
                                        div(cls := "flex items-center justify-between")(
                                            div(
                                                p(cls := "text-sm font-medium text-blue-600")(
                                                    state.name
                                                ),
                                                p(cls := "text-sm text-gray-500 mt-1")(
                                                    state.description
                                                )
                                            ),
                                            span(
                                                cls := "inline-flex items-center rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-medium text-blue-800"
                                            )(
                                                "View"
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    )
                ),
                currentPath = s"/preview/${basePath.mkString("/")}"
            )
        }

    // Render a specific state
    override def renderState(stateName: String): ZIO[Any, String, Frag] =
        ZIO.fromOption(findState(stateName))
            .mapError(_ => s"State '$stateName' not found")
            .map { state =>
                appShell.wrap(
                    pageTitle = s"$title - ${state.name}",
                    content = div(
                        // Navigation
                        div(cls := "mb-4")(
                            a(
                                href := s"/preview/${basePath.mkString("/")}",
                                cls := "text-sm text-blue-600 hover:underline"
                            )(
                                "← Back to states list"
                            )
                        ),

                        // State info
                        div(cls := "mb-6")(
                            h3(cls := "text-xl font-semibold mb-2")(state.name),
                            p(cls := "text-gray-600")(state.description)
                        ),

                        // Component preview
                        div(cls := "mb-8")(
                            h4(cls := "text-lg font-medium mb-3")("Component Preview:"),
                            div(cls := "border p-4 bg-gray-50 rounded")(
                                renderComponent(state.viewModel)
                            )
                        ),

                        // View model properties
                        div(
                            h4(cls := "text-lg font-medium mb-3")("View Model Properties:"),
                            div(cls := "border rounded overflow-hidden")(
                                renderPropertyDetails(state.viewModel)
                            )
                        )
                    ),
                    currentPath = s"/preview/${basePath.mkString("/")}/$stateName"
                )
            }
end ResultsPanelPreviewModule
