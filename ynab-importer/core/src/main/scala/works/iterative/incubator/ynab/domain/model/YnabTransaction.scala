package works.iterative.incubator.ynab.domain.model

import java.time.LocalDate
import java.util.UUID

/**
 * YNAB Transaction
 *
 * Represents a transaction that can be imported into YNAB
 *
 * Domain Model: This is a core domain entity representing a YNAB transaction.
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