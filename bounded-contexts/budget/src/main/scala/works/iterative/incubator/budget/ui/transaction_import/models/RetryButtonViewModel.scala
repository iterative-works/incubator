package works.iterative.incubator.budget.ui.transaction_import.models

import java.time.LocalDate

/**
 * View model for the retry button component.
 *
 * @param isVisible Whether the button is shown
 * @param isEnabled Whether the button is clickable
 * @param startDate The start date to use for the retry
 * @param endDate The end date to use for the retry
 */
case class RetryButtonViewModel(
    isVisible: Boolean,
    isEnabled: Boolean = true,
    startDate: LocalDate,
    endDate: LocalDate
):
    /**
     * Gets the button text.
     *
     * @return The button text
     */
    def buttonText: String = "Try Again"
    
    /**
     * Determines if the button should be disabled.
     *
     * @return true if the button is not enabled
     */
    def isDisabled: Boolean = !isEnabled
    
    /**
     * Gets a formatted string representation of date parameters.
     *
     * @return A string with the parameters for the retry request
     */
    def importParameters: String = s"startDate=${startDate}&endDate=${endDate}"