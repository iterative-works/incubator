package works.iterative.incubator.fio.domain.model

import java.time.Instant

/**
 * Represents the state of Fio transaction imports
 * Used to track last imported transaction ID and timestamp
 *
 * Classification: Domain Entity
 */
final case class FioImportState(
    sourceAccountId: Long,
    lastTransactionId: Option[Long],
    lastImportTimestamp: Instant
)

/**
 * Repository for storing and retrieving Fio import state
 *
 * Classification: Domain Repository Interface
 */
trait FioImportStateRepository:
    /**
     * Get the import state for a specific source account
     *
     * @param sourceAccountId The source account ID to get import state for
     * @return The import state if it exists
     */
    def getImportState(sourceAccountId: Long): zio.Task[Option[FioImportState]]
    
    /**
     * Update the import state for a specific source account
     *
     * @param state The new import state
     * @return Unit indicating success
     */
    def updateImportState(state: FioImportState): zio.Task[Unit]