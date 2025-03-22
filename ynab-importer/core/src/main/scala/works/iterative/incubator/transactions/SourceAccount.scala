package works.iterative.incubator.transactions

import java.time.Instant

/** Represents a bank account from which transactions are imported
  *
  * This entity serves as a reference/lookup entity for mapping between bank accounts and YNAB
  * accounts, as well as storing configuration for accessing bank APIs.
  *
  * @param id
  *   Unique identifier for the source account
  * @param accountId
  *   The account ID/number as used by the bank
  * @param bankId
  *   The bank ID/code
  * @param name
  *   User-defined name or alias for this account
  * @param currency
  *   ISO 4217 currency code (e.g., "CZK", "EUR", "USD")
  * @param ynabAccountId
  *   Optional ID of the corresponding account in YNAB
  * @param active
  *   Whether this account is active for importing transactions
  * @param lastSyncTime
  *   Optional timestamp of the last successful sync
  */
case class SourceAccount(
    id: Long,
    accountId: String,
    bankId: String,
    name: String,
    currency: String,
    ynabAccountId: Option[String] = None,
    active: Boolean = true,
    lastSyncTime: Option[Instant] = None
)
