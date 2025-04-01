package works.iterative.incubator.ynab

import java.time.LocalDate
import java.util.UUID

/**
 * YNAB Domain Models
 *
 * These models represent the core domain entities for YNAB integration,
 * following our Functional Core/Imperative Shell architecture.
 */

/**
 * YNAB Budget
 *
 * Represents a YNAB budget that contains accounts, categories, and transactions
 */
case class YnabBudget(
    id: String,
    name: String,
    lastModifiedOn: Option[LocalDate] = None,
    currencyCode: Option[String] = None
)

/**
 * YNAB Account
 *
 * Represents a YNAB account that transactions can be imported into
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

/**
 * YNAB Category Group
 *
 * Represents a group of categories in YNAB
 */
case class YnabCategoryGroup(
    id: String,
    name: String,
    hidden: Boolean = false,
    deleted: Boolean = false
)

/**
 * YNAB Category
 *
 * Represents a category that transactions can be assigned to
 */
case class YnabCategory(
    id: String,
    name: String,
    groupId: String,
    groupName: String,
    hidden: Boolean = false,
    deleted: Boolean = false,
    budgeted: Option[BigDecimal] = None,
    activity: Option[BigDecimal] = None,
    balance: Option[BigDecimal] = None
)

/**
 * YNAB Transaction
 *
 * Represents a transaction that can be imported into YNAB
 */
case class YnabTransaction(
    id: Option[String] = None,
    date: LocalDate,
    amount: BigDecimal,
    memo: Option[String] = None,
    cleared: String = "cleared", // "cleared", "uncleared", or "reconciled"
    approved: Boolean = true,
    accountId: String,
    payeeName: Option[String] = None,
    payeeId: Option[String] = None,
    categoryId: Option[String] = None,
    flagColor: Option[String] = None, // "red", "orange", "yellow", "green", "blue", "purple"
    importId: Option[String] = None
):
    /**
     * Generate an import ID based on the transaction details
     * This helps prevent duplicate imports
     */
    def generateImportId: String =
        // Format: YYYYMMDD:amount:source-id
        // This is a common approach for YNAB import IDs, ensuring uniqueness
        val dateStr = date.toString.replace("-", "")
        val amountStr = (amount * 1000).toLongExact.toString
        val uuid = UUID.randomUUID().toString.take(8)
        s"$dateStr:$amountStr:$uuid"