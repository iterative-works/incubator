package works.iterative.incubator.view.dev.examples

import scalatags.Text.all._
import scalatags.Text.TypedTag
import works.iterative.incubator.transactions.domain.model.SourceAccount

/**
 * Example views for source accounts.
 * Simplified versions of the real SourceAccountModule views.
 */
class SourceAccountViewExample {
  
  /**
   * Render a view based on the scenario and data
   */
  def renderView(scenario: String, data: ExampleData): TypedTag[String] = scenario match {
    case "default" => listView(data.sourceAccounts)
    case "empty" => emptyView()
    case "with-errors" => errorView(data.sourceAccounts, data.errors)
    case "form" => formView(data.formValues)
    case _ => div(cls := "p-4 text-red-500")("Unknown scenario: " + scenario)
  }
  
  /**
   * View for listing source accounts
   */
  def listView(accounts: List[SourceAccount]): TypedTag[String] = {
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Source Accounts"),
      
      // Action buttons
      div(cls := "flex justify-end mb-4")(
        a(href := "#", cls := "px-4 py-2 bg-green-500 text-white rounded")("Add New Account")
      ),
      
      // Account list
      div(cls := "bg-white shadow rounded")(
        table(cls := "min-w-full")(
          thead(
            tr(cls := "bg-gray-200 text-gray-600 uppercase text-sm leading-normal")(
              th(cls := "py-3 px-6 text-left")("Name"),
              th(cls := "py-3 px-6 text-left")("Account Number"),
              th(cls := "py-3 px-6 text-left")("Bank"),
              th(cls := "py-3 px-6 text-left")("Status"),
              th(cls := "py-3 px-6 text-right")("Actions")
            )
          ),
          tbody(
            accounts.map { account =>
              tr(cls := "border-b border-gray-200 hover:bg-gray-100")(
                td(cls := "py-3 px-6 text-left")(account.name),
                td(cls := "py-3 px-6 text-left")(account.accountId),
                td(cls := "py-3 px-6 text-left")(account.bankId),
                td(cls := "py-3 px-6 text-left")(
                  if (account.active) {
                    span(cls := "bg-green-200 text-green-700 py-1 px-3 rounded-full text-xs")("Active")
                  } else {
                    span(cls := "bg-gray-200 text-gray-700 py-1 px-3 rounded-full text-xs")("Inactive")
                  }
                ),
                td(cls := "py-3 px-6 text-right")(
                  a(href := "#", cls := "text-blue-500 hover:underline mr-2")("View"),
                  a(href := "#", cls := "text-yellow-500 hover:underline mr-2")("Edit"),
                  button(cls := "text-red-500 hover:underline")("Delete")
                )
              )
            }
          )
        )
      )
    )
  }
  
  /**
   * View for empty state
   */
  def emptyView(): TypedTag[String] = {
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Source Accounts"),
      
      // Action buttons
      div(cls := "flex justify-end mb-4")(
        a(href := "#", cls := "px-4 py-2 bg-green-500 text-white rounded")("Add New Account")
      ),
      
      // Empty state
      div(cls := "bg-white shadow rounded p-8 text-center")(
        p(cls := "text-gray-500 mb-4")("No source accounts found."),
        a(href := "#", cls := "px-4 py-2 bg-blue-500 text-white rounded")("Add Your First Account")
      )
    )
  }
  
  /**
   * View with error messages
   */
  def errorView(accounts: List[SourceAccount], errors: List[String]): TypedTag[String] = {
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Source Accounts"),
      
      // Error messages
      div(cls := "bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4")(
        errors.map { error =>
          p(error)
        }
      ),
      
      // Account list
      div(cls := "bg-white shadow rounded")(
        table(cls := "min-w-full")(
          thead(
            tr(cls := "bg-gray-200 text-gray-600 uppercase text-sm leading-normal")(
              th(cls := "py-3 px-6 text-left")("Name"),
              th(cls := "py-3 px-6 text-left")("Account Number"),
              th(cls := "py-3 px-6 text-left")("Bank"),
              th(cls := "py-3 px-6 text-left")("Status"),
              th(cls := "py-3 px-6 text-right")("Actions")
            )
          ),
          tbody(
            accounts.map { account =>
              tr(cls := "border-b border-gray-200 hover:bg-gray-100")(
                td(cls := "py-3 px-6 text-left")(account.name),
                td(cls := "py-3 px-6 text-left")(account.accountId),
                td(cls := "py-3 px-6 text-left")(account.bankId),
                td(cls := "py-3 px-6 text-left")(
                  if (account.active) {
                    span(cls := "bg-green-200 text-green-700 py-1 px-3 rounded-full text-xs")("Active")
                  } else {
                    span(cls := "bg-gray-200 text-gray-700 py-1 px-3 rounded-full text-xs")("Inactive")
                  }
                ),
                td(cls := "py-3 px-6 text-right")(
                  a(href := "#", cls := "text-blue-500 hover:underline mr-2")("View"),
                  a(href := "#", cls := "text-yellow-500 hover:underline mr-2")("Edit"),
                  button(cls := "text-red-500 hover:underline")("Delete")
                )
              )
            }
          )
        )
      )
    )
  }
  
  /**
   * View for form
   */
  def formView(values: Map[String, String]): TypedTag[String] = {
    div(cls := "container mx-auto")(
      h1(cls := "text-2xl font-bold mb-4")("Add Source Account"),
      
      div(cls := "bg-white shadow rounded p-6")(
        form(action := "#", method := "post")(
          div(cls := "mb-4")(
            label(cls := "block text-gray-700 text-sm font-bold mb-2", `for` := "name")("Account Name"),
            input(
              cls := "shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline",
              id := "name",
              name := "name",
              tpe := "text",
              placeholder := "Main Checking",
              value := values.getOrElse("name", "")
            )
          ),
          
          div(cls := "mb-4")(
            label(cls := "block text-gray-700 text-sm font-bold mb-2", `for` := "accountId")("Account Number"),
            input(
              cls := "shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline",
              id := "accountId",
              name := "accountId",
              tpe := "text",
              placeholder := "CZ123456789",
              value := values.getOrElse("accountId", "")
            )
          ),
          
          div(cls := "mb-4")(
            label(cls := "block text-gray-700 text-sm font-bold mb-2", `for` := "bankId")("Bank Code"),
            input(
              cls := "shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline",
              id := "bankId",
              name := "bankId",
              tpe := "text",
              placeholder := "FIO",
              value := values.getOrElse("bankId", "")
            )
          ),
          
          div(cls := "mb-4")(
            label(cls := "block text-gray-700 text-sm font-bold mb-2")(
              input(tpe := "checkbox", name := "active", checked := true),
              span(cls := "ml-2")("Active")
            )
          ),
          
          div(cls := "flex items-center justify-between")(
            button(
              cls := "bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline",
              tpe := "submit"
            )("Save Account"),
            a(
              href := "#",
              cls := "inline-block align-baseline font-bold text-sm text-blue-500 hover:text-blue-800"
            )("Cancel")
          )
        )
      )
    )
  }
}