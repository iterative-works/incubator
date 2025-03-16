package works.iterative.incubator.transactions
package infrastructure

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import zio.*
import works.iterative.scalatags.ScalatagsSupport
import works.iterative.scalatags.components.ScalatagsAppShell
import scalatags.text.Frag
import org.http4s.Response
import org.http4s.Request
import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*
import cats.Applicative
import works.iterative.incubator.components.ScalatagsTailwindTable
import service.TransactionRepository
import service.TransactionImportService
import java.time.LocalDate
import org.http4s.headers.Location
import org.http4s.Uri

class TransactionImportModule(appShell: ScalatagsAppShell)
    extends ZIOWebModule[TransactionRepository & TransactionImportService]
    with ScalatagsSupport:

    object service:
        // Get transactions from repository
        def getTransactions(using
            req: Request[WebTask]
        ): WebTask[(Seq[Transaction], Option[String])] =
            for
                transactions <-
                    ZIO.serviceWithZIO[TransactionRepository](_.find(TransactionQuery()))
            yield (transactions, req.params.get("importStatus"))

        // Import transactions for yesterday
        def importYesterdayTransactions: WebTask[String] =
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            ZIO.serviceWithZIO[TransactionImportService](
                _.importTransactions(yesterday, today)
            ).fold(
                err => s"Failed to import transactions: $err",
                count => s"Successfully imported $count transactions"
            )
        end importYesterdayTransactions

        def processWithAI(transactionIds: Seq[String]): UIO[Unit] = ZIO.unit // Stub for now

        def submitToYNAB(transactionIds: Seq[String]): UIO[Unit] = ZIO.unit // Stub for now
    end service

    object view:
        def transactionList(
            transactions: Seq[Transaction],
            importStatus: Option[String] = None
        ): scalatags.Text.TypedTag[String] =
            import scalatags.Text.all.*
            import works.iterative.scalatags.sl as sl

            def formatAmount(amount: BigDecimal, currency: String): String =
                f"${amount}%.2f $currency"

            def statusBadge(status: TransactionStatus): scalatags.Text.TypedTag[String] =
                status match
                    case TransactionStatus.Imported =>
                        sl.Badge(sl.variant := "neutral")("Imported")
                    case TransactionStatus.Categorized =>
                        sl.Badge(sl.variant := "primary")("Categorized")
                    case TransactionStatus.Submitted =>
                        sl.Badge(sl.variant := "success")("Submitted")

            def actionButtons(transaction: Transaction): scalatags.Text.TypedTag[String] =
                transaction.status match
                    case TransactionStatus.Imported =>
                        div(cls := "flex gap-2")(
                            sl.Button(sl.size := "small", sl.variant := "primary")(
                                "Process with AI"
                            )
                        )
                    case TransactionStatus.Categorized =>
                        div(cls := "flex gap-2")(
                            sl.Button(sl.size := "small", sl.variant := "success")(
                                "Submit to YNAB"
                            )
                        )
                    case TransactionStatus.Submitted =>
                        div(cls := "flex gap-2")(
                            sl.Button(
                                sl.size := "small",
                                sl.variant := "neutral",
                                sl.disabled := true
                            )(
                                "Submitted"
                            )
                        )

            // Define the table columns
            val columns = Seq(
                // Checkbox column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "",
                    render = tx =>
                        sl.Checkbox(id := s"select-${tx.id}"),
                    className = _ => "w-10"
                ),

                // Date column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Date",
                    render = tx =>
                        span(tx.date.toString)
                ),

                // Description column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Description",
                    render = tx =>
                        div(
                            div(
                                tx.userIdentification.getOrElse("") +
                                    tx.message.map(m => s" - $m").getOrElse("")
                            ),
                            div(cls := "text-xs text-gray-500")(
                                s"Acc: ${tx.counterAccount.getOrElse("-")}, Bank: ${tx.counterBankName.getOrElse("-")}"
                            )
                        )
                ),

                // Amount column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Amount",
                    render = tx =>
                        span(
                            formatAmount(tx.amount, tx.currency)
                        ),
                    className = tx =>
                        if tx.amount < 0 then "text-red-600" else "text-green-600"
                ),

                // Status column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Status",
                    render = tx =>
                        statusBadge(tx.status)
                ),

                // Payee column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Payee",
                    render = tx =>
                        tx.status match
                            case TransactionStatus.Imported =>
                                div(cls := "text-gray-400 italic")(
                                    "Not processed"
                                )
                            case _ =>
                                sl.Input(
                                    value := tx.suggestedPayeeName.getOrElse(""),
                                    placeholder := "Enter payee name"
                                )
                ),

                // Category column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Category",
                    render = tx =>
                        tx.status match
                            case TransactionStatus.Imported =>
                                div(cls := "text-gray-400 italic")(
                                    "Not processed"
                                )
                            case _ =>
                                sl.Select(
                                    sl.Option(
                                        value := tx.suggestedCategory.getOrElse(""),
                                        selected := true
                                    )(tx.suggestedCategory.getOrElse("Select category")),
                                    sl.Option(value := "groceries")("Groceries"),
                                    sl.Option(value := "dining")("Dining Out"),
                                    sl.Option(value := "utilities")("Utilities"),
                                    sl.Option(value := "transport")("Transportation"),
                                    sl.Option(value := "housing")("Housing"),
                                    sl.Option(value := "entertainment")("Entertainment"),
                                    sl.Option(value := "income")("Income"),
                                    sl.Option(value := "other")("Other")
                                )
                ),

                // Actions column
                ScalatagsTailwindTable.Column[Transaction](
                    header = "Actions",
                    render = tx =>
                        actionButtons(tx)
                )
            )

            val transactionTable = ScalatagsTailwindTable
                .table(columns, transactions)
                .withClass("border-collapse")
                .withHeaderClasses(Seq("bg-gray-100"))
                .render

            div(cls := "p-4")(
                // Header with title and actions
                div(cls := "flex justify-between items-center mb-4")(
                    h1(cls := "text-2xl font-bold")("Transaction Import"),

                    // Import button - simplified for testing
                    form(action := "/transactions/import-yesterday", method := "post")(
                        sl.Button(sl.variant := "primary", `type` := "submit")(
                            "Import Yesterday's Transactions"
                        )
                    )
                ),

                // Import status message (if available)
                importStatus.map(status =>
                    div(cls := "mb-4 p-4 bg-green-100 text-green-800 rounded")(
                        status
                    )
                ).getOrElse(frag()),

                // Filters and controls
                div(cls := "mb-4 flex gap-2 flex-wrap")(
                    sl.Select(
                        cls := "min-w-40",
                        sl.label := "Status",
                        sl.Option(value := "all")("All"),
                        sl.Option(value := "imported")("Imported"),
                        sl.Option(value := "categorized")("Categorized"),
                        sl.Option(value := "submitted")("Submitted")
                    ),
                    sl.Input(
                        cls := "min-w-60",
                        sl.label := "Search",
                        placeholder := "Search transactions..."
                    ),
                    div(cls := "flex-grow"), // Spacer
                    div(cls := "flex gap-2")(
                        sl.Button(sl.size := "medium", sl.variant := "primary")(
                            "Process Selected with AI"
                        ),
                        sl.Button(sl.size := "medium", sl.variant := "success")(
                            "Submit Selected to YNAB"
                        )
                    )
                ),

                // Transaction table
                div(cls := "overflow-x-auto")(
                    transactionTable
                ),

                // Pagination controls
                div(cls := "mt-4 flex justify-between items-center")(
                    div(cls := "text-sm text-gray-600")(
                        s"Showing ${transactions.size} transactions"
                    ),
                    div(cls := "flex gap-2")(
                        sl.Button(sl.size := "small", sl.variant := "neutral")("Previous"),
                        sl.Button(sl.size := "small", sl.variant := "neutral")("Next")
                    )
                )
            )
        end transactionList
    end view

    override def routes: HttpRoutes[WebTask] =
        val dsl = Http4sDsl[WebTask]
        import dsl.*

        def of[F[_]: Monad](pf: PartialFunction[Request[F], Request[F] => F[Response[F]]])
            : HttpRoutes[F] =
            Kleisli: req =>
                OptionT(Applicative[F].unit >> pf.lift(req).map(_(req)).sequence)

        def respondZIO[A](
            provide: Request[WebTask] ?=> WebTask[A],
            render: A => Frag
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(data =>
                    Ok(appShell.wrap("Transactions", render(data)))
                )

        def respondZIOFull[A](
            provide: Request[WebTask] ?=> WebTask[A],
            reply: A => WebTask[Response[WebTask]]
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(resp => reply(resp))

        of[WebTask] {
            // Main transaction listing page
            case GET -> Root / "transactions" =>
                respondZIO(
                    service.getTransactions,
                    (data, status) => view.transactionList(data, status)
                )

            // Handle the import-yesterday button submission
            case POST -> Root / "transactions" / "import-yesterday" =>
                respondZIOFull(
                    service.importYesterdayTransactions,
                    importStatus =>
                        // Redirect back to the transactions page with status message
                        SeeOther(Location(Uri.unsafeFromString("/transactions").withQueryParam(
                            "importStatus",
                            importStatus
                        )))
                )
        }
    end routes
end TransactionImportModule
