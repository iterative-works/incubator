package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/** Form input for transaction import.
  *
  * @param startDate
  *   The start date for the transaction import range in ISO format (YYYY-MM-DD)
  * @param endDate
  *   The end date for the transaction import range in ISO format (YYYY-MM-DD)
  */
case class ImportFormInput(
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
end ImportFormInput