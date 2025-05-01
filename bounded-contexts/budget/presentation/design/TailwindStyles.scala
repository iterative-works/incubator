package works.iterative.incubator.budget.presentation.design

/**
 * Centralized definition of TailwindCSS class combinations used across UI components.
 * This helps maintain consistency and makes refactoring easier.
 */
object TailwindStyles {
  // Layout classes
  val container = "max-w-container mx-auto px-sm py-md"
  val card = "bg-white rounded-card shadow-card p-sm mb-md"
  val flexRow = "flex flex-row items-center"
  val flexCol = "flex flex-col"
  val flexBetween = "flex justify-between items-center"
  val flexCenter = "flex justify-center items-center"
  val gridContainer = "grid gap-sm"
  val statsGrid = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-sm"
  
  // Typography classes
  val heading1 = "text-h1 font-bold text-neutral-dark leading-h1"
  val heading2 = "text-h2 font-medium text-neutral-dark leading-h2"
  val heading3 = "text-h3 font-medium text-neutral-dark leading-h3"
  val bodyText = "text-body text-neutral-dark leading-body"
  val smallText = "text-small text-neutral-gray leading-small"
  val microText = "text-micro text-neutral-gray leading-micro"
  
  // Button classes
  val buttonBase = "py-xs px-sm rounded font-medium transition duration-200 focus:outline-none focus:ring-2"
  val primaryButton = s"$buttonBase bg-primary text-white hover:bg-primary-dark focus:ring-primary-light"
  val secondaryButton = s"$buttonBase bg-neutral-light text-neutral-dark hover:bg-neutral-gray focus:ring-neutral-gray"
  val dangerButton = s"$buttonBase bg-accent-red text-white hover:bg-red-700 focus:ring-red-300"
  val successButton = s"$buttonBase bg-accent-green text-white hover:bg-green-700 focus:ring-green-300"
  val disabledButton = s"$buttonBase bg-neutral-light text-neutral-gray cursor-not-allowed"
  
  // Form classes
  val formGroup = "mb-sm"
  val label = "block text-small font-medium text-neutral-dark mb-xs"
  val input = "w-full border border-neutral-gray rounded px-xs py-xs focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
  val select = "w-full border border-neutral-gray rounded px-xs py-xs focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
  val checkbox = "h-4 w-4 text-primary focus:ring-primary border-neutral-gray rounded"
  
  // Table classes
  val tableContainer = "w-full overflow-x-auto"
  val table = "min-w-full divide-y divide-neutral-light"
  val tableHeader = "bg-neutral-light"
  val tableHeaderCell = "px-sm py-xs text-left text-small font-medium text-neutral-dark uppercase tracking-wider"
  val tableRow = "hover:bg-neutral-light"
  val tableCell = "px-sm py-sm whitespace-nowrap text-body"
  val sortableHeader = "cursor-pointer hover:bg-neutral-gray"
  val sortIndicatorAsc = "ml-xs inline-block transform rotate-180"  // Down arrow
  val sortIndicatorDesc = "ml-xs inline-block"  // Up arrow
  
  // Status indicators
  val statusIndicator = "inline-flex items-center"
  val statusDot = "h-2 w-2 rounded-full mr-xs"
  val statusDotPending = s"$statusDot bg-neutral-gray"
  val statusDotProcessing = s"$statusDot bg-primary"
  val statusDotComplete = s"$statusDot bg-accent-green"
  val statusDotError = s"$statusDot bg-accent-red"
  val statusLabel = "text-small"
  
  // Amount styling
  val positiveAmount = "font-medium text-accent-green"
  val negativeAmount = "font-medium text-accent-red"
  
  // Notification
  val notificationBase = "p-sm rounded-card mb-md flex items-start"
  val notificationSuccess = s"$notificationBase bg-green-50 border-l-4 border-accent-green text-green-800"
  val notificationError = s"$notificationBase bg-red-50 border-l-4 border-accent-red text-red-800"
  val notificationInfo = s"$notificationBase bg-blue-50 border-l-4 border-primary text-blue-800"
  val notificationWarning = s"$notificationBase bg-yellow-50 border-l-4 border-accent-yellow text-yellow-800"
  val notificationIcon = "flex-shrink-0 mr-sm"
  val notificationContent = "flex-grow"
  val notificationClose = "flex-shrink-0 ml-sm text-neutral-dark hover:text-neutral-gray cursor-pointer"
  
  // Loading indicator
  val loadingContainer = "fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50"
  val loadingSpinner = "animate-spin h-10 w-10 border-4 border-white border-opacity-25 border-t-white rounded-full"
  val loadingText = "text-white mt-sm"
  
  // Modal dialog
  val modalOverlay = "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-40"
  val modalContent = "bg-white rounded-card shadow-modal w-full max-w-lg mx-sm"
  val modalHeader = "px-md py-sm border-b border-neutral-light"
  val modalTitle = "text-h2 font-medium"
  val modalBody = "px-md py-md"
  val modalFooter = "px-md py-sm border-t border-neutral-light flex justify-end space-x-xs"
  
  // Import dialog specific
  object ImportDialog {
    val container = modalContent
    val form = "space-y-md"
    val dateFields = "grid grid-cols-1 md:grid-cols-2 gap-sm"
    val actions = "flex justify-end space-x-xs mt-md"
  }
  
  // Transaction table specific
  object TransactionTable {
    val container = "mb-lg"
    val header = "mb-sm flex flex-col sm:flex-row sm:justify-between sm:items-center"
    val filterContainer = "mb-sm sm:mb-0 w-full sm:w-64"
    val filter = input
    val actionsContainer = "flex justify-end space-x-xs"
    val pagination = "mt-sm flex justify-between items-center"
    val paginationText = smallText
    val paginationControls = "flex space-x-xs"
    val sortableHeaderActive = s"$sortableHeader text-primary"
  }
  
  // Statistics panel specific
  object StatisticsPanel {
    val container = statsGrid
    val card = s"${TailwindStyles.card} flex flex-col"
    val label = "text-small text-neutral-gray mb-xs"
    val value = "text-h2 font-bold"
    val valuePositive = s"$value text-accent-green"
    val valueNegative = s"$value text-accent-red"
    val percentage = "text-small text-neutral-gray"
  }
}