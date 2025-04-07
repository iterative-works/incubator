package works.iterative.incubator.ynab.domain.model

/**
 * YNAB Transaction Import Result
 *
 * Represents the result of importing a transaction to YNAB
 *
 * Domain Model: This is a domain value object representing the result of an import operation.
 */
sealed trait YnabTransactionImportResult
case class YnabTransactionImportSuccess(ynabTransactionId: String) extends YnabTransactionImportResult
case class YnabTransactionImportError(error: Throwable) extends YnabTransactionImportResult