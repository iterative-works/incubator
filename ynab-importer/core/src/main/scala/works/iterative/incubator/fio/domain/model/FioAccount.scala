package works.iterative.incubator.fio.domain.model

import java.time.Instant

/** Represents a Fio Bank account with its specific properties
  *
  * This entity holds Fio Bank-specific account information, including API tokens
  * and synchronization details.
  *
  * Classification: Domain Entity
  */
case class FioAccount(
    id: Long,                   // Unique identifier for this Fio account
    sourceAccountId: Long,      // Reference to the generic source account
    token: String,              // API token for Fio Bank
    lastSyncTime: Option[Instant] = None,   // Last time transactions were synced
    lastFetchedId: Option[Long] = None      // ID of the last transaction fetched
)

/** Command to create a new Fio Bank account
  *
  * Classification: Domain Value Object (Command)
  */
case class CreateFioAccount(
    sourceAccountId: Long,     // Reference to the generic source account
    token: String              // API token for Fio Bank
)