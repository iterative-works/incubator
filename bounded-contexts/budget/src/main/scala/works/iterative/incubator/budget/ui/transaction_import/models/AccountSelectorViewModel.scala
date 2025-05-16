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
