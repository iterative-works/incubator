package works.iterative.incubator.transactions
package infrastructure

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import works.iterative.scalatags.ScalatagsSupport
import works.iterative.scalatags.components.ScalatagsAppShell

class TransactionImportModule(appShell: ScalatagsAppShell) extends ZIOWebModule[Any]
    with ScalatagsSupport:
    object service // TODO: Will implement later

    object view:
        def transactionList: scalatags.Text.TypedTag[String] =
            import scalatags.Text.all.*
            import works.iterative.scalatags.sl as sl

            div(cls := "p-4")(
                // Header with title and actions
                div(cls := "flex justify-between items-center mb-4")(
                    h1(cls := "text-2xl font-bold")("Transaction Import"),
                    sl.Button(sl.variant := "primary")("Import New Transactions")
                ),

                // Filters and controls
                div(cls := "mb-4 flex gap-2")(
                    sl.Select(
                        sl.label := "Status",
                        sl.Option(value := "all")("All"),
                        sl.Option(value := "imported")("Imported"),
                        sl.Option(value := "categorized")("Categorized"),
                        sl.Option(value := "submitted")("Submitted")
                    ),
                    sl.Input(
                        sl.label := "Search",
                        placeholder := "Search transactions..."
                    )
                ),

                // Transaction table
                div(cls := "overflow-x-auto")(
                    table(cls := "min-w-full")(
                        thead(cls := "bg-gray-50")(
                            tr(
                                th(cls := "p-4 text-left")("Date"),
                                th(cls := "p-4 text-left")("Description"),
                                th(cls := "p-4 text-left")("Amount"),
                                th(cls := "p-4 text-left")("Status"),
                                th(cls := "p-4 text-left")("Category"),
                                th(cls := "p-4 text-left")("Actions")
                            )
                        ),
                        tbody(
                            // TODO: Replace with actual data
                            tr(cls := "border-t")(
                                td(cls := "p-4")("2024-03-06"),
                                td(cls := "p-4")("Sample Transaction"),
                                td(cls := "p-4")("$100.00"),
                                td(cls := "p-4")(
                                    sl.Badge(sl.variant := "neutral")("Imported")
                                ),
                                td(cls := "p-4")(
                                    sl.Select(
                                        sl.Option(value := "groceries")("Groceries"),
                                        sl.Option(value := "utilities")("Utilities")
                                    )
                                ),
                                td(cls := "p-4")(
                                    sl.Button(sl.size := "small")("Submit to YNAB")
                                )
                            )
                        )
                    )
                )
            )
        end transactionList
    end view

    override def routes: HttpRoutes[WebTask] =
        val dsl = Http4sDsl[WebTask]
        import dsl.*

        HttpRoutes.of[WebTask] {
            case GET -> Root / "transactions" =>
                for
                    response <- Ok(appShell.wrap("Transactions", view.transactionList))
                yield response
        }
    end routes
end TransactionImportModule
