package works.iterative.incubator.budget.domain.model

/** Represents the status of a transaction in the processing workflow.
  *
  * Category: Value Object
  * Layer: Domain
  */
enum TransactionStatus:
    /** Transaction has been imported but not yet processed */
    case Imported

    /** Transaction has been categorized with AI suggestions */
    case Categorized

    /** Transaction has been manually validated by the user */
    case Validated

    /** Transaction has been submitted to YNAB */
    case Submitted

    /** Transaction processing has been rejected or failed */
    case Failed
end TransactionStatus
