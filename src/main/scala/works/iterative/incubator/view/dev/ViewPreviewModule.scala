package works.iterative.incubator.view.dev

import zio.*
import org.http4s.*
import org.http4s.headers.`Content-Type`
import org.http4s.dsl.Http4sDsl
import works.iterative.server.http.ZIOWebModule
import works.iterative.incubator.components.ScalatagsAppShell
import zio.interop.catz.*
import scalatags.Text
import works.iterative.incubator.transactions.web.view.*

/** Module for previewing UI views with test data. Provides routes to view different UI components
  * with example data.
  */
class ViewPreviewModule(appShell: ScalatagsAppShell)
    extends ZIOWebModule[ViewPreviewMain.PreviewEnv]:
    import ViewPreviewMain.PreviewTask
    private val dsl = Http4sDsl[PreviewTask]
    import dsl.*

    // Import scalatags selectively to avoid conflicts with http4s
    import scalatags.Text.all.{
        div as stDiv,
        h1 as stH1,
        h2 as stH2,
        p as stP,
        a as stA,
        ul as stUl,
        li as stLi,
        cls,
        href,
        *
    }

    // Example data provider
    private val dataProvider = new TestDataProvider()

    // Use the real view implementations
    private val sourceAccountViews: SourceAccountViews = new SourceAccountViewsImpl()
    private val transactionViews: TransactionViews = new TransactionViewsImpl()

    /** Service layer - in a real module this would connect to repositories For preview, we're using
      * static example data
      */
    object service:
        // Get example data for source accounts
        def getSourceAccountData(scenario: String): PreviewTask[examples.ExampleData] =
            ZIO.succeed(dataProvider.getSourceAccountData(scenario))

        // Get example data for transactions
        def getTransactionData(scenario: String): PreviewTask[examples.ExampleData] =
            ZIO.succeed(dataProvider.getTransactionData(scenario))

        // Get list of available preview scenarios
        def getAvailableScenarios: PreviewTask[Map[String, List[String]]] =
            ZIO.succeed(dataProvider.getAvailableScenarios)
    end service

    /** View layer - renders the preview UI
      */
    object view:
        // Index page showing all available previews
        def index(scenarios: Map[String, List[String]]): Text.TypedTag[String] =
            val scenarioViews = scenarios.map { case (component, scenarioList) =>
                stDiv(cls := "mb-8")(
                    stH2(cls := "text-xl font-semibold mb-2")(s"$component Views"),
                    stUl(cls := "list-disc pl-8")(
                        scenarioList.map { scenario =>
                            stLi(cls := "mb-1")(
                                stA(
                                    href := s"/preview/$component/$scenario",
                                    cls := "text-blue-600 hover:underline"
                                )(
                                    s"$scenario"
                                )
                            )
                        }.toSeq
                    )
                )
            }.toSeq

            stDiv(cls := "container mx-auto py-8 px-4")(
                stH1(cls := "text-3xl font-bold mb-6")("View Previews"),
                stP(cls := "mb-6")(
                    "This is a development tool for previewing UI components with example data. ",
                    "Select a component and scenario to preview."
                ),
                scenarioViews
            )
        end index

        // Render a specific view with data
        def renderPreview(
            component: String,
            scenario: String,
            data: examples.ExampleData
        ): Text.TypedTag[String] =
            // Header with navigation and info
            val header = stDiv(cls := "bg-gray-100 p-4 mb-6")(
                stDiv(cls := "flex justify-between items-center")(
                    stDiv(
                        stH1(cls := "text-xl font-bold")(s"Previewing: $component - $scenario"),
                        stA(href := "/preview", cls := "text-blue-600 hover:underline")(
                            "Back to Index"
                        )
                    ),
                    stDiv(cls := "text-sm text-gray-600")(
                        stP("This is a preview with example data for development purposes.")
                    )
                )
            )

            // Component preview using real view implementations
            val content = (component, scenario) match
                case ("source-accounts", "default") =>
                    sourceAccountViews.sourceAccountList(data.sourceAccounts)
                case ("source-accounts", "empty") =>
                    sourceAccountViews.sourceAccountList(List())
                case ("source-accounts", "with-errors") =>
                    sourceAccountViews.sourceAccountList(data.sourceAccounts)
                case ("source-accounts", "form") =>
                    sourceAccountViews.sourceAccountForm(data.sourceAccounts.headOption)
                case ("source-accounts", scen) if data.sourceAccounts.nonEmpty =>
                    sourceAccountViews.sourceAccountDetail(data.sourceAccounts.head)

                case ("transactions", _) if data.transactionsWithState.nonEmpty =>
                    transactionViews.transactionList(
                        data.transactionsWithState,
                        if scenario == "with-warnings" then Some("Warning detected in transaction")
                        else None
                    )
                case ("transactions", "empty") =>
                    transactionViews.transactionList(List())
                case _ =>
                    stDiv(cls := "p-8 text-center text-red-500")(
                        s"Unknown preview: $component/$scenario"
                    )

            // Combine header and content
            stDiv(
                header,
                stDiv(cls := "container mx-auto px-4")(content)
            )
        end renderPreview
    end view

    /** Routes for the preview module
      */
    override def routes: HttpRoutes[PreviewTask] =
        HttpRoutes.of[PreviewTask] {
            // Index route showing all available previews
            case GET -> Root / "preview" =>
                for
                    scenarios <- service.getAvailableScenarios
                    resp <- Ok(appShell.wrap("View Previews", view.index(scenarios)).render)
                        .map(_.withContentType(`Content-Type`(MediaType.text.html)))
                yield resp

            // Route for previewing a specific component with a specific scenario
            case GET -> Root / "preview" / component / scenario =>
                for
                    data <- if component == "source-accounts" then
                        service.getSourceAccountData(scenario)
                    else if component == "transactions" then
                        service.getTransactionData(scenario)
                    else
                        ZIO.succeed(examples.ExampleData.empty)
                    resp <- Ok(appShell.wrap(
                        s"Preview: $component - $scenario",
                        view.renderPreview(component, scenario, data)
                    ).render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
                yield resp
        }
end ViewPreviewModule
