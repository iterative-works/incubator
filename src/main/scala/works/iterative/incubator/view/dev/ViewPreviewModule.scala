package works.iterative.incubator.view.dev

import zio._
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.dsl.Http4sDsl
import works.iterative.server.http.ZIOWebModule
import works.iterative.incubator.components.ScalatagsAppShell
import zio.interop.catz._
import scalatags.Text

/**
 * Module for previewing UI views with test data.
 * Provides routes to view different UI components with example data.
 */
class ViewPreviewModule(appShell: ScalatagsAppShell) extends ZIOWebModule[ViewPreviewMain.PreviewEnv] {
  import ViewPreviewMain.PreviewTask
  private val dsl = Http4sDsl[PreviewTask]
  import dsl._
  
  // Import scalatags selectively to avoid conflicts with http4s
  import scalatags.Text.all.{div => stDiv, h1 => stH1, h2 => stH2, p => stP, a => stA, ul => stUl, li => stLi,
    span => stSpan, cls, href, _}
  
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
    def index(scenarios: Map[String, List[String]]): Text.TypedTag[String] = {
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
    }
    
    // Render a specific view with data
    def renderPreview(component: String, scenario: String, data: examples.ExampleData): Text.TypedTag[String] = {
      // Header with navigation and info
      val header = stDiv(cls := "bg-gray-100 p-4 mb-6")(
        stDiv(cls := "flex justify-between items-center")(
          stDiv(
            stH1(cls := "text-xl font-bold")(s"Previewing: $component - $scenario"),
            stA(href := "/preview", cls := "text-blue-600 hover:underline")("Back to Index")
          ),
          stDiv(cls := "text-sm text-gray-600")(
            stP("This is a preview with example data for development purposes.")
          )
        )
      )
      
      // Component preview
      val content = (component, scenario) match {
        case ("source-accounts", scen) => sourceAccountViews.renderView(scen, data)
        case ("transactions", scen) => transactionViews.renderView(scen, data)
        case _ => stDiv(cls := "p-8 text-center text-red-500")(s"Unknown preview: $component/$scenario")
      }
      
      // Combine header and content
      stDiv(
        header,
        stDiv(cls := "container mx-auto px-4")(content)
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
          resp <- Ok(appShell.wrap("View Previews", view.index(scenarios)).render)
            .map(_.withContentType(`Content-Type`(MediaType.text.html)))
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
          resp <- Ok(appShell.wrap(s"Preview: $component - $scenario", 
            view.renderPreview(component, scenario, data)
          ).render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        } yield resp
    }
  }
}