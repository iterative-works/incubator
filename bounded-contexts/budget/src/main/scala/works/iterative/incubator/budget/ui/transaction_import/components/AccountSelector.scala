package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.AccountSelectorViewModel
import scalatags.Text.all.*

/** UI component for selecting an account for transaction import.
  *
  * Category: UI Component Layer: UI/Presentation
  */
object AccountSelector:
    /** Render the account selector component.
      *
      * @param viewModel
      *   The view model containing account options and selection state
      * @return
      *   HTML content for the component
      */
    def render(viewModel: AccountSelectorViewModel): Frag =
        div(
            cls := "mb-4",
            h2(
                cls := "text-lg font-semibold mb-2 text-gray-700",
                "Select Account"
            ),
            renderControl(viewModel)
        )
    end render

    /** Render just the select control part without the header. This is used for HTMX updates to
      * avoid duplication.
      *
      * @param viewModel
      *   The view model containing account options and selection state
      * @return
      *   HTML content for just the select control
      */
    def renderControl(viewModel: AccountSelectorViewModel): Frag =
        div(
            cls := "relative",
            id := "account-selector-container",
            select(
                cls := s"w-full px-3 py-2 border rounded-md ${validationClass(viewModel)}",
                id := "accountId",
                name := "accountId",
                value := viewModel.selectedAccountId.getOrElse(""),
                // Add HTMX attributes for real-time form update
                attr("hx-post") := "/transactions/import/submit",
                attr("hx-trigger") := "change",
                attr("hx-target") := "#transaction-import-form",
                attr("hx-swap") := "outerHTML",
                // Default empty option
                option(
                    value := "",
                    if viewModel.selectedAccountId.isEmpty then selected := "" else (),
                    "-- Select an account --"
                ),
                // Generate options for each account
                viewModel.accounts.map { account =>
                    option(
                        value := account.id,
                        if viewModel.selectedAccountId.contains(account.id) then selected := ""
                        else (),
                        account.name
                    )
                }
            ),
            // Error message
            viewModel.validationError.map { error =>
                div(
                    cls := "text-red-500 text-sm mt-1",
                    error
                )
            }
        )
    end renderControl

    /** Determine the appropriate CSS class based on validation state.
      *
      * @param viewModel
      *   The account selector view model
      * @return
      *   CSS classes for the input field
      */
    private def validationClass(viewModel: AccountSelectorViewModel): String =
        if viewModel.validationError.isDefined then
            "border-red-500 focus:border-red-500 focus:ring-red-500"
        else
            "border-gray-300 focus:border-blue-500 focus:ring-blue-500"
end AccountSelector
