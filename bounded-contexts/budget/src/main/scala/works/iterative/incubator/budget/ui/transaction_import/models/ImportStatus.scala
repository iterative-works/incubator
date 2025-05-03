package works.iterative.incubator.budget.ui.transaction_import.models

/** Represents the status of a transaction import operation.
  */
enum ImportStatus:
    case NotStarted, InProgress, Completed, Error
