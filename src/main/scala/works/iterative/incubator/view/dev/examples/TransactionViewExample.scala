package works.iterative.incubator.view.dev.examples

import scalatags.Text.all._
import scalatags.Text.TypedTag
import works.iterative.incubator.transactions._
import java.time.format.DateTimeFormatter

/**
 * Example views for transactions.
 * Simplified versions of the real TransactionModule views.
 */
class TransactionViewExample {
  
  // Helper for formatting dates
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  
  /**
   * Render a view based on the scenario and data
   */
  def renderView(scenario: String, data: ExampleData): TypedTag[String] = scenario match {
    case "default" => listView(data.transactions, data.processingStates)
    case "empty" => emptyView()
    case "with-pending" => listView(data.transactions, data.processingStates)
    case "with-warnings" => warningView(data.transactions, data.processingStates, data.warnings)
    case _ => div(cls := "p-4 text-red-500")("Unknown scenario: " + scenario)
  }
  
  /**
   * View for listing transactions
   */
  def listView(transactions: List[Transaction], states: List[TransactionProcessingState]): TypedTag[String] = {
    // Helper to get state for a transaction
    def getStateForTransaction(txId: TransactionId): Option[TransactionProcessingState] = 
      states.find(_.transactionId == txId)
    
    // Helper to format amount with color
    def formatAmount(amount: BigDecimal): TypedTag[String] = {
      val formatted = amount.toString
      if (amount < 0) {
        span(cls := "text-red-500")(formatted)
      } else {
        span(cls := "text-green-600")(s"+$formatted")
      }
    }
    
    // Helper to format status badge
    def statusBadge(status: TransactionStatus): TypedTag[String] = status match {
      case TransactionStatus.Imported => 
        span(cls := "bg-yellow-200 text-yellow-800 py-1 px-2 rounded-full text-xs")("Imported")
      case TransactionStatus.Categorized => 
        span(cls := "bg-blue-200 text-blue-800 py-1 px-2 rounded-full text-xs")("Categorized")
      case TransactionStatus.Submitted => 
        span(cls := "bg-green-200 text-green-800 py-1 px-2 rounded-full text-xs")("Submitted")
      case _ => 
        span(cls := "bg-gray-200 text-gray-800 py-1 px-2 rounded-full text-xs")("Unknown")
    }
    
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Transactions"),
      
      // Filters
      div(cls := "bg-white p-4 rounded shadow mb-4")(
        form(cls := "flex flex-wrap gap-4 items-end")(
          div(
            label(cls := "block text-gray-700 text-sm font-bold mb-2", `for` := "status")("Status"),
            select(cls := "shadow border rounded py-2 px-3 text-gray-700", id := "status", name := "status")(
              option(value := "")("All"),
              option(value := "imported")("Imported"),
              option(value := "categorized")("Categorized"),
              option(value := "submitted")("Submitted")
            )
          ),
          div(
            label(cls := "block text-gray-700 text-sm font-bold mb-2", `for` := "date-from")("Date From"),
            input(
              cls := "shadow border rounded py-2 px-3 text-gray-700",
              id := "date-from",
              name := "date-from",
              tpe := "date"
            )
          ),
          div(
            label(cls := "block text-gray-700 text-sm font-bold mb-2", `for` := "date-to")("Date To"),
            input(
              cls := "shadow border rounded py-2 px-3 text-gray-700",
              id := "date-to",
              name := "date-to",
              tpe := "date"
            )
          ),
          div(
            button(
              cls := "bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded",
              tpe := "submit"
            )("Filter")
          )
        )
      ),
      
      // Transaction list
      div(cls := "bg-white shadow rounded")(
        table(cls := "min-w-full")(
          thead(
            tr(cls := "bg-gray-200 text-gray-600 uppercase text-sm leading-normal")(
              th(cls := "py-3 px-6 text-left")("Date"),
              th(cls := "py-3 px-6 text-left")("Description"),
              th(cls := "py-3 px-6 text-right")("Amount"),
              th(cls := "py-3 px-6 text-left")("Account"),
              th(cls := "py-3 px-6 text-center")("Status"),
              th(cls := "py-3 px-6 text-right")("Actions")
            )
          ),
          tbody(
            transactions.map { tx =>
              val state = getStateForTransaction(tx.id)
              tr(cls := "border-b border-gray-200 hover:bg-gray-100")(
                td(cls := "py-3 px-6 text-left")(tx.date.format(dateFormatter)),
                td(cls := "py-3 px-6 text-left")(tx.userIdentification.getOrElse("No description")),
                td(cls := "py-3 px-6 text-right")(formatAmount(tx.amount)),
                td(cls := "py-3 px-6 text-left")(s"${tx.counterAccount.getOrElse("-")}/${tx.counterBankCode.getOrElse("-")}"),
                td(cls := "py-3 px-6 text-center")(
                  state.map(s => statusBadge(s.status)).getOrElse(
                    span(cls := "bg-gray-200 text-gray-800 py-1 px-2 rounded-full text-xs")("Unknown")
                  )
                ),
                td(cls := "py-3 px-6 text-right")(
                  a(href := "#", cls := "text-blue-500 hover:underline mr-2")("View"),
                  a(href := "#", cls := "text-yellow-500 hover:underline")("Process")
                )
              )
            }
          )
        )
      ),
      
      // Pagination
      div(cls := "mt-4 flex justify-between items-center")(
        div(cls := "text-gray-600")(s"Showing ${transactions.size} transactions"),
        div(cls := "flex")(
          a(href := "#", cls := "mx-1 px-3 py-2 bg-white rounded border")("Previous"),
          a(href := "#", cls := "mx-1 px-3 py-2 bg-blue-500 text-white rounded")("1"),
          a(href := "#", cls := "mx-1 px-3 py-2 bg-white rounded border")("2"),
          a(href := "#", cls := "mx-1 px-3 py-2 bg-white rounded border")("3"),
          a(href := "#", cls := "mx-1 px-3 py-2 bg-white rounded border")("Next")
        )
      )
    )
  }
  
  /**
   * View for empty state
   */
  def emptyView(): TypedTag[String] = {
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Transactions"),
      
      // Empty state
      div(cls := "bg-white shadow rounded p-8 text-center")(
        p(cls := "text-gray-500 mb-4")("No transactions found."),
        p(cls := "mb-4")("Import transactions from your bank account to get started."),
        a(href := "#", cls := "px-4 py-2 bg-blue-500 text-white rounded")("Import Transactions")
      )
    )
  }
  
  /**
   * View with warnings
   */
  def warningView(transactions: List[Transaction], states: List[TransactionProcessingState], warnings: List[String]): TypedTag[String] = {
    // Helper to get state for a transaction
    def getStateForTransaction(txId: TransactionId): Option[TransactionProcessingState] = 
      states.find(_.transactionId == txId)
    
    // Helper to format amount with color
    def formatAmount(amount: BigDecimal): TypedTag[String] = {
      val formatted = amount.toString
      if (amount < 0) {
        span(cls := "text-red-500")(formatted)
      } else {
        span(cls := "text-green-600")(s"+$formatted")
      }
    }
    
    // Helper to format status badge
    def statusBadge(status: TransactionStatus): TypedTag[String] = status match {
      case TransactionStatus.Imported => 
        span(cls := "bg-yellow-200 text-yellow-800 py-1 px-2 rounded-full text-xs")("Imported")
      case TransactionStatus.Categorized => 
        span(cls := "bg-blue-200 text-blue-800 py-1 px-2 rounded-full text-xs")("Categorized")
      case TransactionStatus.Submitted => 
        span(cls := "bg-green-200 text-green-800 py-1 px-2 rounded-full text-xs")("Submitted")
      case _ => 
        span(cls := "bg-gray-200 text-gray-800 py-1 px-2 rounded-full text-xs")("Unknown")
    }
    
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Transactions"),
      
      // Warning messages
      div(cls := "bg-orange-100 border border-orange-400 text-orange-700 px-4 py-3 rounded mb-4")(
        warnings.map { warning =>
          p(warning)
        }
      ),
      
      // Transaction list
      div(cls := "bg-white shadow rounded")(
        table(cls := "min-w-full")(
          thead(
            tr(cls := "bg-gray-200 text-gray-600 uppercase text-sm leading-normal")(
              th(cls := "py-3 px-6 text-left")("Date"),
              th(cls := "py-3 px-6 text-left")("Description"),
              th(cls := "py-3 px-6 text-right")("Amount"),
              th(cls := "py-3 px-6 text-left")("Account"),
              th(cls := "py-3 px-6 text-center")("Status"),
              th(cls := "py-3 px-6 text-right")("Actions")
            )
          ),
          tbody(
            transactions.map { tx =>
              val state = getStateForTransaction(tx.id)
              val hasPotentialWarning = tx.amount > BigDecimal("500")
              
              tr(cls := s"border-b border-gray-200 hover:bg-gray-100 ${if (hasPotentialWarning) "bg-orange-50" else ""}")(
                td(cls := "py-3 px-6 text-left")(tx.date.format(dateFormatter)),
                td(cls := "py-3 px-6 text-left")(tx.userIdentification.getOrElse("No description")),
                td(cls := "py-3 px-6 text-right")(formatAmount(tx.amount)),
                td(cls := "py-3 px-6 text-left")(s"${tx.counterAccount.getOrElse("-")}/${tx.counterBankCode.getOrElse("-")}"),
                td(cls := "py-3 px-6 text-center")(
                  state.map(s => statusBadge(s.status)).getOrElse(
                    span(cls := "bg-gray-200 text-gray-800 py-1 px-2 rounded-full text-xs")("Unknown")
                  )
                ),
                td(cls := "py-3 px-6 text-right")(
                  a(href := "#", cls := "text-blue-500 hover:underline mr-2")("View"),
                  if (hasPotentialWarning) {
                    a(href := "#", cls := "text-orange-500 hover:underline mr-2")("Resolve Warning")
                  } else {
                    a(href := "#", cls := "text-yellow-500 hover:underline")("Process")
                  }
                )
              )
            }
          )
        )
      )
    )
  }
}