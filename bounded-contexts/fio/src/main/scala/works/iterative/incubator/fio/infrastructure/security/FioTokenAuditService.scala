package works.iterative.incubator.fio.infrastructure.security

import zio.*
import java.time.Instant

/** Token audit event types
  *
  * Classification: Infrastructure Security Value Object
  */
enum TokenAuditEventType:
    case Access
    case Update
    case Invalidate
    case CacheHit
end TokenAuditEventType

/** Token audit event
  *
  * Classification: Infrastructure Security Value Object
  */
case class TokenAuditEvent(
    timestamp: Instant,
    eventType: TokenAuditEventType,
    accountId: Long,
    sourceAccountId: Option[Long] = None,
    message: String
)

/** Interface for token audit logging service
  *
  * This service provides audit logging for token-related operations.
  *
  * Classification: Infrastructure Security Service
  */
trait FioTokenAuditService:
    /** Log a token audit event
      *
      * @param event
      *   The audit event to log
      * @return
      *   Unit on success
      */
    def logEvent(event: TokenAuditEvent): Task[Unit]

    /** Get recent audit events
      *
      * @param limit
      *   Maximum number of events to retrieve
      * @return
      *   Recent audit events
      */
    def getRecentEvents(limit: Int = 100): Task[List[TokenAuditEvent]]

    /** Get audit events for a specific account
      *
      * @param accountId
      *   The account ID
      * @param limit
      *   Maximum number of events to retrieve
      * @return
      *   Audit events for the account
      */
    def getEventsForAccount(accountId: Long, limit: Int = 100): Task[List[TokenAuditEvent]]
end FioTokenAuditService

/** Live implementation of FioTokenAuditService
  *
  * This implementation provides in-memory audit logging for token operations. For production use,
  * consider using a persistent storage instead.
  *
  * Classification: Infrastructure Security Service Implementation
  */
class FioTokenAuditServiceLive() extends FioTokenAuditService:
    private val auditLog: Ref[List[TokenAuditEvent]] =
        Unsafe.unsafely:
            Ref.unsafe.make(List.empty[TokenAuditEvent])

    /** Log a token audit event
      */
    override def logEvent(event: TokenAuditEvent): Task[Unit] =
        for
            _ <-
                auditLog.update(events =>
                    (event :: events).take(1000)
                ) // Keep the most recent 1000 events
            _ <- ZIO.logInfo(
                s"Token audit: ${event.eventType} for accountId=${event.accountId}" +
                    event.sourceAccountId.map(id => s", sourceAccountId=$id").getOrElse("") +
                    s" - ${event.message}"
            )
        yield ()

    /** Get recent audit events
      */
    override def getRecentEvents(limit: Int = 100): Task[List[TokenAuditEvent]] =
        auditLog.get.map(_.take(limit))

    /** Get audit events for a specific account
      */
    override def getEventsForAccount(
        accountId: Long,
        limit: Int = 100
    ): Task[List[TokenAuditEvent]] =
        auditLog.get.map(_.filter(_.accountId == accountId).take(limit))
end FioTokenAuditServiceLive

object FioTokenAuditServiceLive:
    /** ZIO layer for the audit service
      */
    val layer: ULayer[FioTokenAuditService] =
        ZLayer.succeed(FioTokenAuditServiceLive())
end FioTokenAuditServiceLive
