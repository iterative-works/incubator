package works.iterative.incubator.fio.domain.model

import java.time.Instant
import zio.Task

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

/**
 * Repository interface for Fio Bank accounts
 *
 * Classification: Domain Repository Interface
 */
trait FioAccountRepository:
    /**
     * Create a new Fio Bank account
     *
     * @param command Command with account details
     * @return Created account ID
     */
    def create(command: CreateFioAccount): Task[Long]
    
    /**
     * Get a Fio Bank account by ID
     *
     * @param id Account ID
     * @return The account if found
     */
    def getById(id: Long): Task[Option[FioAccount]]
    
    /**
     * Get a Fio Bank account by source account ID
     *
     * @param sourceAccountId Source account ID
     * @return The account if found
     */
    def getBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]]
    
    /**
     * Get all Fio Bank accounts
     *
     * @return List of all accounts
     */
    def getAll(): Task[List[FioAccount]]
    
    /**
     * Update a Fio Bank account
     *
     * @param account Updated account
     * @return Unit on success
     */
    def update(account: FioAccount): Task[Unit]
    
    /**
     * Delete a Fio Bank account
     *
     * @param id Account ID
     * @return Unit on success
     */
    def delete(id: Long): Task[Unit]
    
    /**
     * Update the last fetched transaction ID for an account
     *
     * @param id Account ID
     * @param lastFetchedId Last fetched transaction ID
     * @param syncTime Time of the sync
     * @return Unit on success
     */
    def updateLastFetched(id: Long, lastFetchedId: Long, syncTime: Instant): Task[Unit]