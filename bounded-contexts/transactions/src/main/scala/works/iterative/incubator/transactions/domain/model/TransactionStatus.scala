package works.iterative.incubator.transactions.domain.model

/** Represents the current status of a transaction in the processing pipeline.
  *
  * - Imported: Transaction has been imported from the bank but not yet categorized
  * - Categorized: Transaction has been categorized but not yet submitted to YNAB
  * - Submitted: Transaction has been successfully submitted to YNAB
  *
  * Classification: Domain Enumeration (Value Object)
  */
enum TransactionStatus:
    case Imported, Categorized, Submitted