package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate
import works.iterative.incubator.budget.domain.model.AccountId

/** Form input for transaction import.
  *
  * @param accountId
  *   The ID of the account to import transactions from in the format "bankId-bankAccountId"
  * @param startDate
  *   The start date for the transaction import range in ISO format (YYYY-MM-DD)
  * @param endDate
  *   The end date for the transaction import range in ISO format (YYYY-MM-DD)
  */
case class ImportFormInput(
    accountId: String,
    startDate: String,
    endDate: String
):
    /** Convert string dates to LocalDate objects.
      *
      * @return
      *   A tuple of (startDate, endDate) as LocalDate objects
      */
    def toLocalDates: (LocalDate, LocalDate) =
        (LocalDate.parse(startDate), LocalDate.parse(endDate))
        
    /** Convert string accountId to AccountId domain object.
      *
      * @return
      *   Either an error message or the AccountId object
      */
    def toAccountId: Either[String, AccountId] =
        AccountId.fromString(accountId)
end ImportFormInput
