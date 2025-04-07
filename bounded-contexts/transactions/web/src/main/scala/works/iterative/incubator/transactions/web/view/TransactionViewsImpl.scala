package works.iterative.incubator.transactions.web.view

import works.iterative.scalatags.ScalatagsSupport
import scalatags.Text.TypedTag
import works.iterative.incubator.transactions.domain.model.TransactionStatus
import works.iterative.incubator.components.ScalatagsTailwindTable

/**
 * Implementation of the TransactionViews trait using ScalaTags.
 * This class renders the HTML for transaction related views.
 *
 * Classification: Web View Implementation
 */
class TransactionViewsImpl extends TransactionViews with ScalatagsSupport {
  import scalatags.Text.all._
  import works.iterative.scalatags.sl as sl

  /**
   * View for displaying a list of transactions with their processing states.
   */
  override def transactionList(
    transactions: Seq[TransactionWithState],
    importStatus: Option[String] = None
  ): TypedTag[String] = {
    def formatAmount(amount: BigDecimal, currency: String): String =
      f"${amount}%.2f $currency"

    def statusBadge(status: TransactionStatus): TypedTag[String] =
      status match
        case TransactionStatus.Imported =>
          sl.Badge(sl.variant := "neutral")("Imported")
        case TransactionStatus.Categorized =>
          sl.Badge(sl.variant := "primary")("Categorized")
        case TransactionStatus.Submitted =>
          sl.Badge(sl.variant := "success")("Submitted")

    def actionButtons(tx: TransactionWithState): TypedTag[String] =
      tx.status match
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
      ScalatagsTailwindTable.Column[TransactionWithState](
        header = "",
        render = tx =>
          sl.Checkbox(
            id := s"select-${tx.id.sourceAccountId}-${tx.id.transactionId}"
          ),
        className = _ => "w-10"
      ),

      // Date column
      ScalatagsTailwindTable.Column[TransactionWithState](
        header = "Date",
        render = tx =>
          span(tx.date.toString)
      ),

      // Description column
      ScalatagsTailwindTable.Column[TransactionWithState](
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
      ScalatagsTailwindTable.Column[TransactionWithState](
        header = "Amount",
        render = tx =>
          span(
            formatAmount(tx.amount, tx.currency)
          ),
        className = tx =>
          if tx.amount < 0 then "text-red-600" else "text-green-600"
      ),

      // Status column
      ScalatagsTailwindTable.Column[TransactionWithState](
        header = "Status",
        render = tx =>
          statusBadge(tx.status)
      ),

      // Payee column
      ScalatagsTailwindTable.Column[TransactionWithState](
        header = "Payee",
        render = tx =>
          tx.status match
            case TransactionStatus.Imported =>
              div(cls := "text-gray-400 italic")(
                "Not processed"
              )
            case _ =>
              sl.Input(
                value := tx.effectivePayeeName.getOrElse(""),
                placeholder := "Enter payee name"
              )
      ),

      // Category column
      ScalatagsTailwindTable.Column[TransactionWithState](
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
                  value := tx.effectiveCategory.getOrElse(""),
                  selected := true
                )(tx.effectiveCategory.getOrElse("Select category")),
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
      ScalatagsTailwindTable.Column[TransactionWithState](
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
        if transactions.isEmpty then
          div(cls := "text-center py-8 text-gray-500")(
            "No transactions found. Import some transactions to get started."
          )
        else
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
  }
}