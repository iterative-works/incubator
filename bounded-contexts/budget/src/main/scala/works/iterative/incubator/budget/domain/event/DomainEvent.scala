package works.iterative.incubator.budget.domain.event

import java.time.Instant

/** Base trait for domain events
  *
  * Domain events represent significant occurrences in the domain that other parts
  * of the system might be interested in.
  */
trait DomainEvent:
    /** When the event occurred */
    def occurredAt: Instant