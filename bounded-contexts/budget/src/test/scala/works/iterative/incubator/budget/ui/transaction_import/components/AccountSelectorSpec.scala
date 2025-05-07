package works.iterative.incubator.budget.ui.transaction_import.components

import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.ui.transaction_import.models.{AccountSelectorViewModel, AccountOption}
import works.iterative.incubator.budget.ui.transaction_import.components.AccountSelector.render

/** Tests for the AccountSelector component.
  */
object AccountSelectorSpec extends ZIOSpecDefault:
    // Test account options
    val testAccounts = List(
        AccountOption("0100-1234567890", "Fio Bank - Main Account"),
        AccountOption("0300-0987654321", "ČSOB - Business Account"),
        AccountOption("0100-5647382910", "Komerční banka - Savings")
    )

    def spec = suite("AccountSelector")(
        test("should render with no account selected") {
            // Given a view model with no selected account
            val viewModel = AccountSelectorViewModel(
                accounts = testAccounts,
                selectedAccountId = None,
                validationError = None
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should contain expected elements
            val result1 = assert(rendered)(containsString("<select"))
            val result2 = assert(rendered)(containsString("id=\"account-selector\""))
            val result3 = assert(rendered)(containsString("<option value=\"\" selected=\"true\" disabled=\"true\">"))
            val result4 = assert(rendered)(containsString("-- Select an account --"))
            val result5 = assert(rendered)(containsString("border-gray-300")) // No error border
            result1 && result2 && result3 && result4 && result5
        },
        
        test("should render with an account selected") {
            // Given a view model with a selected account
            val selectedAccountId = "0100-1234567890"
            val viewModel = AccountSelectorViewModel(
                accounts = testAccounts,
                selectedAccountId = Some(selectedAccountId),
                validationError = None
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should contain expected elements and mark the selected account
            val result1 = assert(rendered)(containsString(s"value=\"$selectedAccountId\" selected"))
            val result2 = assert(rendered)(containsString("value=\"\" selected=\"false\"")) // Default option not selected
            val result3 = assert(rendered)(containsString("Fio Bank - Main Account"))
            result1 && result2 && result3
        },
        
        test("should render with validation error") {
            // Given a view model with a validation error
            val errorMessage = "Please select an account"
            val viewModel = AccountSelectorViewModel(
                accounts = testAccounts,
                selectedAccountId = None,
                validationError = Some(errorMessage)
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should show the error message with appropriate styling
            val result1 = assert(rendered)(containsString(errorMessage))
            val result2 = assert(rendered)(containsString("text-red-500"))
            val result3 = assert(rendered)(containsString("border-red-500")) // Error border
            result1 && result2 && result3
        },
        
        test("should render all account options from the view model") {
            // Given a view model with multiple accounts
            val viewModel = AccountSelectorViewModel(
                accounts = testAccounts,
                selectedAccountId = None,
                validationError = None
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should render all account options
            val result1 = assert(rendered)(containsString("Fio Bank - Main Account"))
            val result2 = assert(rendered)(containsString("ČSOB - Business Account"))
            val result3 = assert(rendered)(containsString("Komerční banka - Savings"))
            result1 && result2 && result3
        },
        
        test("should have proper HTMX attributes for validation") {
            // Given a valid view model
            val viewModel = AccountSelectorViewModel(
                accounts = testAccounts,
                selectedAccountId = None,
                validationError = None
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should have the HTMX attributes for validation
            val result1 = assert(rendered)(containsString("hx-post=\"/validate-account\""))
            val result2 = assert(rendered)(containsString("hx-trigger=\"change\""))
            val result3 = assert(rendered)(containsString("hx-target=\"#account-selector-container\""))
            val result4 = assert(rendered)(containsString("hx-swap=\"outerHTML\""))
            result1 && result2 && result3 && result4
        },
        
        test("should render appropriate heading") {
            // Given a view model
            val viewModel = AccountSelectorViewModel(
                accounts = testAccounts,
                selectedAccountId = None,
                validationError = None
            )

            // When rendering the component
            val rendered = render(viewModel).render

            // Then it should have the appropriate heading
            val result = assert(rendered)(containsString("<h2"))
            val result2 = assert(rendered)(containsString("Select Account"))
            result && result2
        }
    )
end AccountSelectorSpec