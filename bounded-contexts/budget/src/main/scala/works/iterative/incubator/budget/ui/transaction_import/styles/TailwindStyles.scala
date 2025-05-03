package works.iterative.incubator.budget.ui.transaction_import.styles

/** Utility object that defines standardized TailwindCSS class combinations for UI components. This
  * centralizes styling to ensure consistency and reduce duplication across the application. Styles
  * are organized into semantic categories and support composition through string interpolation.
  */
object TailwindStyles:
    // Layout styles
    val container = "container mx-auto px-4 py-6"
    val card = "bg-white rounded-lg shadow-md p-6 mb-4"
    val panel = "bg-white rounded-lg shadow p-4 mb-4"
    val row = "flex flex-row items-center"
    val column = "flex flex-col"
    val grid = "grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3"
    val divider = "border-t border-gray-200 my-4"

    // Spacing
    val spacingX = "space-x-4"
    val spacingY = "space-y-4"
    val padding = "p-4"
    val margin = "m-4"

    // Typography styles
    val heading1 = "text-2xl font-bold text-gray-800 mb-4"
    val heading2 = "text-xl font-semibold text-gray-700 mb-3"
    val heading3 = "text-lg font-medium text-gray-700 mb-2"
    val bodyText = "text-sm text-gray-600"
    val label = "text-sm font-medium text-gray-700 mb-1"
    val errorText = "text-sm text-red-600"
    val successText = "text-sm text-green-600"
    val infoText = "text-sm text-blue-600"
    val smallText = "text-xs text-gray-500"

    // Form styles
    val formGroup = "mb-4"
    val input =
        "mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
    val formLabel = "block text-sm font-medium text-gray-700 mb-1"
    val dateInput = s"$input bg-white"

    // Button styles
    val buttonBase =
        "px-4 py-2 rounded-md font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors duration-200 flex items-center justify-center"
    val buttonPrimary = s"$buttonBase bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500"
    val buttonSecondary =
        s"$buttonBase bg-gray-200 text-gray-800 hover:bg-gray-300 focus:ring-gray-500"
    val buttonSuccess =
        s"$buttonBase bg-green-600 text-white hover:bg-green-700 focus:ring-green-500"
    val buttonDanger = s"$buttonBase bg-red-600 text-white hover:bg-red-700 focus:ring-red-500"
    val buttonWarning =
        s"$buttonBase bg-yellow-500 text-white hover:bg-yellow-600 focus:ring-yellow-400"
    val buttonDisabled = "opacity-50 cursor-not-allowed"

    // Status indicator styles
    val statusIndicatorBase = "flex items-center p-3 rounded-md mb-4"
    val statusIndicatorSuccess =
        s"$statusIndicatorBase bg-green-50 text-green-700 border border-green-200"
    val statusIndicatorError = s"$statusIndicatorBase bg-red-50 text-red-700 border border-red-200"
    val statusIndicatorLoading =
        s"$statusIndicatorBase bg-blue-50 text-blue-700 border border-blue-200"
    val statusIndicatorInfo =
        s"$statusIndicatorBase bg-gray-50 text-gray-700 border border-gray-200"

    // Icon styles
    val icon = "w-5 h-5 mr-2"
    val iconSuccess = s"$icon text-green-500"
    val iconError = s"$icon text-red-500"
    val iconInfo = s"$icon text-blue-500"
    val iconWarning = s"$icon text-yellow-500"
    val spinner = "animate-spin w-5 h-5 mr-2 text-blue-600"

    // Results panel styles
    val resultsSummary =
        "flex flex-col md:flex-row justify-between items-start md:items-center p-4 bg-gray-50 rounded-md mb-4"
    val resultsCount = "text-lg font-semibold mb-2 md:mb-0"
    val resultsTime = "text-sm text-gray-500"
    val resultsDetail = "mt-2"

    // Error display styles
    val errorContainer = "bg-red-50 border border-red-200 rounded-md p-4 mb-4"
    val errorTitle = "text-lg font-medium text-red-800"
    val errorDetail = "mt-2 text-sm text-red-700"
    val errorCode = "mt-2 text-xs font-mono bg-red-100 p-1 rounded"

    // Component-specific styles
    object DateRangeSelector:
        val container = "flex flex-col md:flex-row md:space-x-4 mb-6"
        val formGroup = TailwindStyles.formGroup
        val label = TailwindStyles.formLabel
        val dateInput = TailwindStyles.dateInput
        val errorText = TailwindStyles.errorText
    end DateRangeSelector

    object ImportButton:
        val button = buttonPrimary
        val buttonDisabled = s"$buttonPrimary ${TailwindStyles.buttonDisabled}"
        val loadingSpinner = spinner
        val buttonText = "ml-1"
    end ImportButton

    object StatusIndicator:
        val notStarted = statusIndicatorInfo
        val inProgress = statusIndicatorLoading
        val completed = statusIndicatorSuccess
        val error = statusIndicatorError
        val icon = TailwindStyles.icon
        val statusText = "ml-2 text-sm font-medium"
    end StatusIndicator

    object ResultsPanel:
        val container = panel
        val header = "flex justify-between items-center mb-4"
        val title = heading3
        val dateRange = "text-sm text-gray-500"
        val summary = resultsSummary
        val transactionCount = resultsCount
        val processingTime = resultsTime
        val errorContainer = TailwindStyles.errorContainer
        val errorTitle = TailwindStyles.errorTitle
        val errorMessage = TailwindStyles.errorDetail
        val errorCode = TailwindStyles.errorCode
        val retryButtonContainer = "mt-4 flex justify-end"
        val retryButton = buttonPrimary
    end ResultsPanel

    object TransactionCountSummary:
        val container = "flex flex-row items-center"
        val count = "text-2xl font-bold mr-2"
        val text = "text-gray-600"
    end TransactionCountSummary

    object RetryButton:
        val button = buttonPrimary
        val icon = "w-4 h-4 mr-2"

    object ImportActionPanel:
        val container = "flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-6"
        val dateRangeSelector = "flex-grow"
        val importButton = "md:self-end"
    end ImportActionPanel

    // Utility methods for conditional styling
    def conditionalClass(condition: Boolean, trueClass: String, falseClass: String = ""): String =
        if condition then trueClass else falseClass
end TailwindStyles
