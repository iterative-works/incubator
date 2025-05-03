package works.iterative.incubator.ui.preview

import works.iterative.tapir.BaseUri
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import scalatags.Text.all.*
import zio.*

/** Navigation module for the preview server Handles the navigation structure and categories
  */
class NavigationPreviewModule(
    val appShell: PreviewAppShell,
    val baseUri: BaseUri
) extends PreviewModule:

    override def basePath: List[String] = List("navigation")

    /** Navigation endpoint */
    val navigationEndpoint = baseEndpoint
        .in(basePathInput)
        .description("Component Preview Navigation")
        .get

    /** Render the navigation structure page */
    def renderNavigationPage: ZIO[Any, String, Frag] =
        ZIO.succeed {
            appShell.wrap(
                pageTitle = "Navigation Structure",
                content = div(
                    h3(cls := "text-xl font-semibold mb-4", "Navigation Structure"),
                    p(
                        cls := "mb-4",
                        "This page shows the structure of the navigation available in the preview server."
                    ),
                    div(
                        cls := "mt-6",
                        h4(cls := "text-lg font-semibold mb-2", "Available Categories:"),
                        div(
                            cls := "space-y-4",
                            // Transaction Import category
                            div(
                                cls := "border rounded-lg p-4",
                                h5(cls := "font-medium text-lg mb-2", "Transaction Import"),
                                p(
                                    cls := "text-sm text-gray-600 mb-2",
                                    "Components related to the transaction import feature"
                                ),
                                ul(
                                    cls := "list-disc pl-6 space-y-1",
                                    li(a(
                                        href := "/preview/transaction-import/date-range-selector",
                                        cls := "text-blue-600 hover:underline",
                                        "DateRangeSelector"
                                    )),
                                    // Add more components as implemented
                                    li(cls := "text-gray-400", "ImportButton (coming soon)"),
                                    li(cls := "text-gray-400", "StatusIndicator (coming soon)"),
                                    li(cls := "text-gray-400", "ResultsPanel (coming soon)")
                                )
                            ),

                            // Future categories can be added here
                            div(
                                cls := "border rounded-lg p-4 bg-gray-50",
                                h5(
                                    cls := "font-medium text-lg mb-2",
                                    "Other Categories (Coming Soon)"
                                ),
                                p(
                                    cls := "text-sm text-gray-600",
                                    "Additional component categories will be added as they are implemented"
                                )
                            )
                        )
                    )
                ),
                currentPath = "/preview/navigation"
            )
        }

    /** Server endpoint for the navigation page */
    val navigationServerEndpoint = navigationEndpoint.zServerLogic(_ => renderNavigationPage)

    override val endpoints = List(navigationEndpoint)

    override val serverEndpoints: List[ServerEndpoint[Any, RIO[PreviewEnv, *]]] = List(
        navigationServerEndpoint.asInstanceOf[ServerEndpoint[Any, RIO[PreviewEnv, *]]]
    )
end NavigationPreviewModule
