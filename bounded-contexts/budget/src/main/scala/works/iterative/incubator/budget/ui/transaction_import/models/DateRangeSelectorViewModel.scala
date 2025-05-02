package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/**
 * View model for the date range selector component.
 *
 * @param startDate The selected start date
 * @param endDate The selected end date
 * @param validationError Optional error message if the date range is invalid
 */
case class DateRangeSelectorViewModel(
    startDate: LocalDate,
    endDate: LocalDate,
    validationError: Option[String] = None
):
    /**
     * Determines if the date range is valid according to business rules.
     *
     * @return true if the date range is valid, false otherwise
     */
    def isValid: Boolean = 
        startDate != null && 
        endDate != null && 
        !startDate.isAfter(endDate) && 
        !startDate.isAfter(LocalDate.now()) &&
        !endDate.isAfter(LocalDate.now()) &&
        !startDate.plusDays(90).isBefore(endDate) // Fio Bank API limitation: max 90 days

    /**
     * Indicates whether there is a validation error to display.
     *
     * @return true if a validation error exists
     */
    def hasError: Boolean = validationError.isDefined
    
    /**
     * Returns the maximum allowed date (today).
     *
     * @return the current date
     */
    def maxDate: LocalDate = LocalDate.now()