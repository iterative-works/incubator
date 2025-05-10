package works.iterative.incubator.budget.ui.transaction_import.models

/** Represents a selectable account with display information.
  *
  * @param id
  *   The composite account ID in the format "bankId-bankAccountId"
  * @param name
  *   A human-readable name for the account (e.g., "Fio Bank - Main Account")
  */
case class AccountOption(
    id: String,
    name: String
)

/** View model for the account selector component.
  *
  * @param accounts
  *   List of available accounts to choose from
  * @param selectedAccountId
  *   Currently selected account ID
  * @param validationError
  *   Optional validation error message
  */
case class AccountSelectorViewModel(
    accounts: List[AccountOption],
    selectedAccountId: Option[String] = None,
    validationError: Option[String] = None
)

/** Companion object for AccountSelectorViewModel.
  */
object AccountSelectorViewModel:
    /** Default list of accounts for demonstration purposes.
      * In a real implementation, these would come from a repository.
      */
    val defaultAccounts: List[AccountOption] = List(
        AccountOption("0100-1234567890", "Fio Bank - Main Account"),
        AccountOption("0300-0987654321", "ČSOB - Business Account"),
        AccountOption("0100-5647382910", "Komerční banka - Savings")
    )
end AccountSelectorViewModel