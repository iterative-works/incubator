package works.iterative.incubator.budget.ui.transaction_import.models

import works.iterative.incubator.budget.domain.model.AccountId
import java.time.LocalDate
import scala.util.Try

/** Command object representing a transaction import request. Encapsulates all data needed for
  * validation and processing of an import operation.
  *
  * @param accountId
  *   The ID of the account to import transactions from
  * @param startDate
  *   The start date for the transaction import range
  * @param endDate
  *   The end date for the transaction import range
  *
  * Category: Domain Command Layer: Domain/Application
  */
case class TransactionImportCommand(
    accountId: String,
    startDate: String,
    endDate: String
):
    /** Attempt to convert the string dates to LocalDate objects
      *
      * @return
      *   A Try containing a tuple of (startDate, endDate) as LocalDate objects
      */
    def toLocalDates: (Try[LocalDate], Try[LocalDate]) =
        (
            Try(LocalDate.parse(startDate)),
            Try(LocalDate.parse(endDate))
        )

    /** Attempt to convert the string accountId to an AccountId domain object
      *
      * @return
      *   Either an error message (Left) or the AccountId object (Right)
      */
    def toAccountId: Either[String, AccountId] =
        AccountId.fromString(accountId)

    /** Create an instance from a form data map
      *
      * @param formData
      *   Map of form field names to values
      * @return
      *   A new TransactionImportCommand instance
      */
    def withDefaults: TransactionImportCommand =
        this.copy(
            accountId = if accountId == null then "" else accountId,
            startDate = if startDate == null then "" else startDate,
            endDate = if endDate == null then "" else endDate
        )
end TransactionImportCommand

/** Companion object for TransactionImportCommand */
object TransactionImportCommand:
    /** Create a TransactionImportCommand from form data
      *
      * @param formData
      *   Map of form field names to values
      * @return
      *   A new TransactionImportCommand instance with values from the form data
      */
    def fromFormData(formData: Map[String, String]): TransactionImportCommand =
        TransactionImportCommand(
            accountId = formData.getOrElse("accountId", ""),
            startDate = formData.getOrElse("startDate", ""),
            endDate = formData.getOrElse("endDate", "")
        )

    /** Create an empty TransactionImportCommand */
    val empty: TransactionImportCommand = TransactionImportCommand("", "", "")
end TransactionImportCommand
