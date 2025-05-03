package works.iterative.incubator.budget.ui.transaction_import.models

/** View model for the error message display component.
  *
  * @param errorMessage
  *   Error message to display
  * @param errorCode
  *   A unique code for support reference
  * @param isVisible
  *   Whether to display the error
  */
case class ErrorMessageDisplayViewModel(
    errorMessage: String,
    errorCode: String,
    isVisible: Boolean = true
):
    /** Provides a user-friendly error title.
      *
      * @return
      *   A standardized error title
      */
    def errorTitle: String = "Import Failed"

    /** Provides a guidance message for the user.
      *
      * @return
      *   A helpful message with next steps
      */
    def guidanceMessage: String =
        "Please check your connection and try again. If the problem persists, " +
            s"contact support with error code: $errorCode."

    /** Determines if the error has connection-related keywords.
      *
      * @return
      *   true if the error appears to be connection-related
      */
    def isConnectionError: Boolean =
        Seq("connection", "timeout", "network", "unreachable")
            .exists(keyword => errorMessage.toLowerCase.contains(keyword))

    /** Provides a tailored suggestion based on error type.
      *
      * @return
      *   A specific suggestion based on error analysis
      */
    def errorSuggestion: String =
        if isConnectionError then
            "Check your internet connection and try again."
        else if errorMessage.toLowerCase.contains("authentication") then
            "Your authentication token may have expired. Please re-authenticate."
        else
            "Try again or contact support if the issue persists."
end ErrorMessageDisplayViewModel
