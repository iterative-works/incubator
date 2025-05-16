package works.iterative.incubator.budget.domain.service

import java.time.LocalDate

/** Domain-specific error types for the transaction import process.
  *
  * Category: Value Object Layer: Domain
  */
sealed trait TransactionImportError

object TransactionImportError:
    /** Error indicating the date range is invalid.
      *
      * @param message
      *   A detailed description of why the date range is invalid
      */
    case class InvalidDateRange(message: String) extends TransactionImportError

    /** Error indicating a failure in the bank API connection.
      *
      * @param message
      *   A detailed description of the bank API failure
      * @param cause
      *   Optional underlying exception that caused the failure
      */
    case class BankApiError(message: String, cause: Option[Throwable] = None)
        extends TransactionImportError

    /** Error indicating no transactions were found for the date range.
      *
      * @param startDate
      *   The start date of the queried range
      * @param endDate
      *   The end date of the queried range
      */
    case class NoTransactionsFound(startDate: LocalDate, endDate: LocalDate)
        extends TransactionImportError
        
    /** Error indicating all transactions found already exist in the database.
      *
      * @param startDate
      *   The start date of the queried range
      * @param endDate
      *   The end date of the queried range
      * @param count
      *   The number of duplicate transactions found
      */
    case class AllTransactionsDuplicate(startDate: LocalDate, endDate: LocalDate, count: Int)
        extends TransactionImportError

    /** Error indicating a failure in saving the imported transactions.
      *
      * @param message
      *   A detailed description of the storage error
      * @param cause
      *   Optional underlying exception that caused the failure
      */
    case class TransactionStorageError(message: String, cause: Option[Throwable] = None)
        extends TransactionImportError

    /** Error indicating issues with an import batch.
      *
      * @param message
      *   A detailed description of the batch error
      * @param cause
      *   Optional underlying exception that caused the failure
      */
    case class ImportBatchError(message: String, cause: Option[Throwable] = None)
        extends TransactionImportError

    /** General error for any other unexpected issues.
      *
      * @param message
      *   A detailed error description
      * @param cause
      *   Optional underlying exception that caused the error
      */
    case class UnexpectedError(message: String, cause: Option[Throwable] = None)
        extends TransactionImportError
end TransactionImportError
