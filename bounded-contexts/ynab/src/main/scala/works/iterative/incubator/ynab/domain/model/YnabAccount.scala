package works.iterative.incubator.ynab.domain.model

/**
 * YNAB Account
 *
 * Represents a YNAB account that transactions can be imported into
 *
 * Domain Model: This is a core domain entity representing a YNAB account.
 */
case class YnabAccount(
    id: String,
    name: String,
    accountType: String,
    balance: BigDecimal,
    closed: Boolean = false,
    onBudget: Boolean = true,
    transferPayeeId: Option[String] = None,
    deleted: Boolean = false
)