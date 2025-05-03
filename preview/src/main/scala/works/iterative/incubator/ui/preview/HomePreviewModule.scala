package works.iterative.incubator.ui.preview

import works.iterative.tapir.BaseUri
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import scalatags.Text.all.*
import zio.*

/** Home module for the preview server Provides the landing page with instructions
  */
class HomePreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends PreviewModule:

    override def basePath: List[String] = Nil

    /** Home page endpoint */
    val homeEndpoint = baseEndpoint
        .description("Component Preview Home Page")
        .get

    /** Render the home page with instructions */
    def renderHomePage: ZIO[Any, String, Frag] =
        ZIO.succeed {
            appShell.wrap(
                pageTitle = "Component Preview Dashboard",
                content = div(
                    h3(
                        cls := "text-xl font-semibold mb-4",
                        "Welcome to the Component Preview Server"
                    ),
                    p(
                        cls := "mb-4",
                        "This tool allows you to view UI components in isolation with various states and configurations."
                    ),
                    p(
                        cls := "mb-4",
                        "Use the sidebar navigation to browse available components and preview their different states."
                    ),
                    div(
                        cls := "mt-6 border-t pt-4",
                        h4(cls := "text-lg font-semibold mb-2", "How to use this tool:"),
                        ul(
                            cls := "list-disc pl-6 space-y-2",
                            li("Select a component from the sidebar"),
                            li("View the component in different states"),
                            li("Test interactions where applicable"),
                            li("Use this for visual verification during development")
                        )
                    ),
                    div(
                        cls := "mt-6 border-t pt-4",
                        h4(cls := "text-lg font-semibold mb-2", "Available Components:"),
                        ul(
                            cls := "list-disc pl-6 space-y-1",
                            li(a(
                                href := "/preview/transaction-import/date-range-selector",
                                cls := "text-blue-600 hover:underline",
                                "DateRangeSelector - Date selection for transaction imports"
                            )),
                            li(a(
                                href := "/preview/budget/import-button",
                                cls := "text-blue-600 hover:underline",
                                "ImportButton - Button for triggering transaction imports"
                            ))
                            // Add more components as they are implemented
                        )
                    )
                ),
                currentPath = "/preview"
            )
        }

    /** Server endpoint for the home page */
    val homeServerEndpoint = homeEndpoint.zServerLogic(_ => renderHomePage)

    override val endpoints = List(homeEndpoint)

    override val serverEndpoints: List[ServerEndpoint[Any, RIO[PreviewEnv, *]]] = List(
        homeServerEndpoint.asInstanceOf[ServerEndpoint[Any, RIO[PreviewEnv, *]]]
    )
end HomePreviewModule
