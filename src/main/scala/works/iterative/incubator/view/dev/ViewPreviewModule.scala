package works.iterative.incubator.view.dev

import zio._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import scalatags.Text.all._
import scalatags.Text.TypedTag
import works.iterative.server.http.ZIOWebModule
import works.iterative.incubator.components.ScalatagsAppShell

/**
 * Module for previewing UI views with test data.
 * Provides routes to view different UI components with example data.
 */
class ViewPreviewModule(appShell: ScalatagsAppShell) extends ZIOWebModule[ViewPreviewMain.PreviewEnv] {
  import ViewPreviewMain.PreviewTask
  private val dsl = Http4sDsl[PreviewTask]
  import dsl._
  
  // Example data provider
  private val dataProvider = new TestDataProvider()
  
  // Views for components we want to preview
  private val sourceAccountViews = new examples.SourceAccountViewExample()
  private val transactionViews = new examples.TransactionViewExample()
  
  /**
   * Service layer - in a real module this would connect to repositories
   * For preview, we're using static example data
   */
  object service {
    // Get example data for source accounts
    def getSourceAccountData(scenario: String): PreviewTask[examples.ExampleData] =
      ZIO.succeed(dataProvider.getSourceAccountData(scenario))
      
    // Get example data for transactions
    def getTransactionData(scenario: String): PreviewTask[examples.ExampleData] =
      ZIO.succeed(dataProvider.getTransactionData(scenario))
      
    // Get list of available preview scenarios
    def getAvailableScenarios: PreviewTask[Map[String, List[String]]] =
      ZIO.succeed(dataProvider.getAvailableScenarios)
  }
  
  /**
   * View layer - renders the preview UI
   */
  object view {
    // Index page showing all available previews
    def index(scenarios: Map[String, List[String]]): TypedTag[String] =
      div(cls := "container mx-auto py-8 px-4")(
        h1(cls := "text-3xl font-bold mb-6")("View Previews"),
        p(cls := "mb-6")(
          "This is a development tool for previewing UI components with example data. ",
          "Select a component and scenario to preview."
        ),
        
        scenarios.map { case (component, scenarioList) =>
          div(cls := "mb-8")(
            h2(cls := "text-xl font-semibold mb-2")(s"$component Views"),
            ul(cls := "list-disc pl-8")(
              scenarioList.map { scenario =>
                li(cls := "mb-1")(
                  a(
                    href := s"/preview/$component/$scenario", 
                    cls := "text-blue-600 hover:underline"
                  )(
                    s"$scenario"
                  )
                )
              }
            )
          )
        }
      )
    
    // Render a specific view with data
    def renderPreview(component: String, scenario: String, data: examples.ExampleData): TypedTag[String] = {
      // Header with navigation and info
      val header = div(cls := "bg-gray-100 p-4 mb-6")(
        div(cls := "flex justify-between items-center")(
          div(
            h1(cls := "text-xl font-bold")(s"Previewing: $component - $scenario"),
            a(href := "/preview", cls := "text-blue-600 hover:underline")("Back to Index")
          ),
          div(cls := "text-sm text-gray-600")(
            p("This is a preview with example data for development purposes.")
          )
        )
      )
      
      // Component preview
      val content = (component, scenario) match {
        case ("source-accounts", scen) => sourceAccountViews.renderView(scen, data)
        case ("transactions", scen) => transactionViews.renderView(scen, data)
        case _ => div(cls := "p-8 text-center text-red-500")(s"Unknown preview: $component/$scenario")
      }
      
      // Combine header and content
      div(
        header,
        div(cls := "container mx-auto px-4")(content)
      )
    }
  }
  
  /**
   * Routes for the preview module
   */
  override def routes: HttpRoutes[PreviewTask] = {
    HttpRoutes.of[PreviewTask] {
      // Index route showing all available previews
      case GET -> Root / "preview" =>
        for {
          scenarios <- service.getAvailableScenarios
          resp <- Ok(appShell.page("View Previews")(view.index(scenarios)).render)
            .map(_.withContentType(MediaType.text.html))
        } yield resp
        
      // Route for previewing a specific component with a specific scenario
      case GET -> Root / "preview" / component / scenario =>
        for {
          data <- if (component == "source-accounts") {
            service.getSourceAccountData(scenario)
          } else if (component == "transactions") {
            service.getTransactionData(scenario)
          } else {
            ZIO.succeed(examples.ExampleData.empty)
          }
          resp <- Ok(appShell.page(s"Preview: $component - $scenario")(
            view.renderPreview(component, scenario, data)
          ).render).map(_.withContentType(MediaType.text.html))
        } yield resp
    }
  }
}