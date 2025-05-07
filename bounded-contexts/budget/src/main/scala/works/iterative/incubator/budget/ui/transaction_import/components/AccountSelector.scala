package works.iterative.incubator.budget.ui.transaction_import.components

import works.iterative.incubator.budget.ui.transaction_import.models.AccountSelectorViewModel
import works.iterative.incubator.budget.ui.transaction_import.styles.TailwindStyles
import scalatags.Text.all._

/** UI component for selecting an account for transaction import.
  *
  * Category: UI Component
  * Layer: UI/Presentation
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
            div(
                cls := "relative",
                select(
                    cls := s"w-full px-3 py-2 border rounded-md ${validationClass(viewModel)}",
                    id := "account-selector",
                    name := "accountId",
                    hx_post := "/validate-account",
                    hx_trigger := "change",
                    hx_target := "#account-selector-container",
                    hx_swap := "outerHTML",
                    // Default empty option
                    option(
                        value := "",
                        selected := viewModel.selectedAccountId.isEmpty,
                        disabled := true,
                        "-- Select an account --"
                    ),
                    // Generate options for each account
                    viewModel.accounts.map { account =>
                        option(
                            value := account.id,
                            selected := viewModel.selectedAccountId.contains(account.id),
                            account.name
                        )
                    }
                )
            ),
            // Error message
            viewModel.validationError.map { error =>
                div(
                    cls := "text-red-500 text-sm mt-1",
                    error
                )
            }
        )
    end render

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