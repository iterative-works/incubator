package works.iterative.incubator.budget.ui.transaction_import.models

/** Represents the status of a transaction import operation.
  *
  * Category: View Model
  * Layer: UI/Presentation
  */
enum ImportStatus:
    case NotStarted, InProgress, Completed, Error
