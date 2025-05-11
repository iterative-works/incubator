package works.iterative.incubator.budget.domain.model

/** Represents the status of an import batch operation.
  *
  * Category: Value Object Layer: Domain
  */
enum ImportStatus:
    /** Import has not yet started */
    case NotStarted

    /** Import is currently in progress */
    case InProgress

    /** Import has completed successfully */
    case Completed

    /** Import has failed with an error */
    case Error
end ImportStatus
