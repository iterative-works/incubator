package works.iterative.incubator.transactions
package infrastructure

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import zio.*
import works.iterative.scalatags.ScalatagsSupport
import works.iterative.scalatags.components.ScalatagsAppShell
import java.time.{LocalDate, Instant}
import zio.json.*
import zio.stream.ZStream
import scalatags.text.Frag
import org.http4s.Response
import org.http4s.Request
import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*
import cats.Applicative
import works.iterative.incubator.components.ScalatagsTailwindTable
import scalatags.Text.all.*

class TransactionImportModule(appShell: ScalatagsAppShell) extends ZIOWebModule[Any]
    with ScalatagsSupport:
    import TransactionImportModule.*

    object service:
        // Mock data for initial development
        def getTransactions: Task[List[TransactionRow]] =
            for
                stream <- ZStream.fromResource("mock_transactions.json")
                    .via(zio.stream.ZPipeline.utf8Decode)
                    .runCollect
                jsonStr = stream.mkString
                transactions <- ZIO.fromEither(jsonStr.fromJson[List[TransactionRow]])
                    .mapError(err =>
                        new RuntimeException(s"Failed to parse transaction data: $err")
                    )
            yield transactions

        def processWithAI(transactionIds: List[String]): UIO[Unit] = ZIO.unit // Stub for now

        def submitToYNAB(transactionIds: List[String]): UIO[Unit] = ZIO.unit // Stub for now
    end service

    object view:
        def transactionList(transactions: List[TransactionRow]): scalatags.Text.TypedTag[String] =
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

            def actionButtons(transaction: TransactionRow): scalatags.Text.TypedTag[String] =
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
                ScalatagsTailwindTable.Column[TransactionRow](
                    header = "",
                    render = tx =>
                        sl.Checkbox(id := s"select-${tx.id}"),
                    className = _ => "w-10"
                ),

                // Date column
                ScalatagsTailwindTable.Column[TransactionRow](
                    header = "Date",
                    render = tx =>
                        span(tx.date.toString)
                ),

                // Description column
                ScalatagsTailwindTable.Column[TransactionRow](
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
                ScalatagsTailwindTable.Column[TransactionRow](
                    header = "Amount",
                    render = tx =>
                        span(
                            formatAmount(tx.amount, tx.currency)
                        ),
                    className = tx =>
                        if tx.amount < 0 then "text-red-600" else "text-green-600"
                ),

                // Status column
                ScalatagsTailwindTable.Column[TransactionRow](
                    header = "Status",
                    render = tx =>
                        statusBadge(tx.status)
                ),

                // Payee column
                ScalatagsTailwindTable.Column[TransactionRow](
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
                ScalatagsTailwindTable.Column[TransactionRow](
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
                ScalatagsTailwindTable.Column[TransactionRow](
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
                    sl.Button(sl.variant := "primary")("Import New Transactions")
                ),

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
            provide(using _).flatMap(data => Ok(appShell.wrap("Transactions", render(data))))

        of[WebTask] {
            case GET -> Root / "transactions" =>
                respondZIO(
                    service.getTransactions,
                    view.transactionList
                )
        }
    end routes
end TransactionImportModule

object TransactionImportModule:
    case class TransactionRow(
        // Source data from FIO
        id: String, // Unique ID from FIO (column_22)
        date: java.time.LocalDate, // Transaction date
        amount: BigDecimal, // Transaction amount
        currency: String, // Currency code (e.g., CZK)
        counterAccount: Option[String], // Counter account number
        counterBankCode: Option[String], // Counter bank code
        counterBankName: Option[String], // Name of the counter bank
        variableSymbol: Option[String], // Variable symbol
        constantSymbol: Option[String], // Constant symbol
        specificSymbol: Option[String], // Specific symbol
        userIdentification: Option[String], // User identification
        message: Option[String], // Message for recipient
        transactionType: String, // Transaction type
        comment: Option[String], // Comment

        // Processing state
        status: TransactionStatus, // Imported, Categorized, Submitted

        // AI computed/processed fields for YNAB
        suggestedPayeeName: Option[String], // AI suggested payee name
        suggestedCategory: Option[String], // AI suggested category
        suggestedMemo: Option[String], // AI cleaned/processed memo

        // User overrides (if user wants to adjust AI suggestions)
        overridePayeeName: Option[String], // User override for payee
        overrideCategory: Option[String], // User override for category
        overrideMemo: Option[String], // User override for memo

        // YNAB integration fields
        ynabTransactionId: Option[String], // ID assigned by YNAB after submission
        ynabAccountId: Option[String], // YNAB account ID where transaction was submitted

        // Metadata
        importedAt: java.time.Instant, // When this transaction was imported
        processedAt: Option[java.time.Instant], // When AI processed this
        submittedAt: Option[java.time.Instant] // When submitted to YNAB
    ) derives JsonDecoder

    enum TransactionStatus derives JsonDecoder:
        case Imported, Categorized, Submitted
end TransactionImportModule
