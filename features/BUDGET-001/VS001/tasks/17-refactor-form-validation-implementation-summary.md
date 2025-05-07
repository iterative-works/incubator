# Transaction Import Form Validation Refactoring - Implementation Summary

## Core Architecture Changes

We're fundamentally changing our approach to form validation by:

1. **Moving from Component-Level to Domain-Level Validation**: Components should only display data and errors, not perform validation
2. **Treating the Form as a Single Entity**: The entire form creates a command that is validated by the domain
3. **Simplifying UI Updates**: Full form rerender instead of partial out-of-band updates

## Implementation Steps

### 1. Create Transaction Import Command Model

Create a command object that represents the domain operation being performed:

```scala
case class TransactionImportCommand(
  accountId: String,
  startDate: String,
  endDate: String
)
```

This will be used to encapsulate the form data for validation and processing.

### 2. Update Domain Validation Service (or create mock)

Define a domain service for validating the import command:

```scala
trait TransactionImportService:
  /** 
   * Validates and processes a transaction import command.
   *
   * @param command The import command to validate and process
   * @return Either validation errors or import results
   */
  def validateAndProcess(command: TransactionImportCommand): 
    ZIO[Any, String, Either[ValidationErrors, ImportResults]]

case class ValidationErrors(
  errors: Map[String, String] = Map.empty,
  globalErrors: List[String] = List.empty
):
  def hasErrors: Boolean = errors.nonEmpty || globalErrors.nonEmpty
```

For now, we can create a mock implementation that returns validation errors based on predefined rules.

### 3. Simplify Existing Components

#### A. DateRangeSelector Component

Remove all validation logic and HTMX validation calls:

```scala
object DateRangeSelector:
  def render(viewModel: DateRangeSelectorViewModel): Frag =
    // Keep input rendering, but remove validation-specific HTMX attributes
    // Only display errors passed via the view model
    div(
      cls := "w-full",
      id := "date-range-selector"
    )(
      h3(cls := "text-lg font-medium mb-2")("Select Date Range for Import"),
      div(cls := "flex flex-col sm:flex-row")(
        // Start date input
        div(cls := "flex-grow w-full")(
          label(`for` := "startDate", cls := labelClasses)("From:"),
          input(
            `type` := "date",
            id := "startDate",
            name := "startDate",
            cls := startDateBorderClasses,
            value := startDateValue,
            attr("max") := today
          )
        ),
        // End date input
        div(cls := "flex-grow w-full")(
          label(`for` := "endDate", cls := labelClasses)("To:"),
          input(
            `type` := "date",
            id := "endDate",
            name := "endDate",
            cls := endDateBorderClasses,
            value := endDateValue,
            attr("max") := today
          )
        )
      ),
      // Error message display - only shows errors passed in view model
      div(
        cls := "text-sm text-red-600 mt-2",
        style := (if viewModel.hasError then "display: block" else "display: none")
      )(
        viewModel.validationError.getOrElse("")
      )
    )
```

#### B. AccountSelector Component

Similarly, remove validation logic:

```scala
object AccountSelector:
  def render(viewModel: AccountSelectorViewModel): Frag =
    div(
      cls := "mb-4",
      id := "account-selector-container"
    )(
      h2(cls := "text-lg font-semibold mb-2 text-gray-700")("Select Account"),
      div(cls := "relative")(
        select(
          cls := s"w-full px-3 py-2 border rounded-md ${validationClass(viewModel)}",
          id := "accountId",
          name := "accountId",
          value := viewModel.selectedAccountId.getOrElse("")
        )(
          // Default empty option
          option(
            value := "",
            if viewModel.selectedAccountId.isEmpty then selected := "" else (),
            "-- Select an account --"
          ),
          // Generate options for each account
          viewModel.accounts.map { account =>
            option(
              value := account.id,
              if viewModel.selectedAccountId.contains(account.id) then selected := "" else (),
              account.name
            )
          }
        ),
        // Error message - only shows errors passed in view model
        viewModel.validationError.map { error =>
          div(
            cls := "text-red-500 text-sm mt-1",
            error
          )
        }
      )
    )
```

#### C. ImportButton Component

Simplify to only respond to enabled/disabled state:

```scala
object ImportButton:
  def render(viewModel: ImportButtonViewModel): Frag =
    button(
      `type` := "submit",
      cls := buttonClasses,
      attr("aria-disabled") := (if viewModel.isDisabled then "true" else "false"),
      disabled := viewModel.isDisabled
    )(
      loadingSpinner,
      viewModel.buttonText,
      placeholderSpinner
    )
```

### 4. Create Transaction Import Form Component

Create a new component that combines all fields into a cohesive form:

```scala
object TransactionImportForm:
  def render(viewModel: TransactionImportFormViewModel): Frag =
    form(
      id := "transaction-import-form",
      cls := "bg-white rounded-lg py-6 w-full",
      attr("hx-post") := "/transactions/import/submit",
      attr("hx-swap") := "outerHTML"
    )(
      // Global error message if any
      viewModel.globalError.map { error =>
        div(cls := "bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4")(
          error
        )
      },
      
      // Account selector
      div(
        cls := "mb-4 w-full",
        AccountSelector.render(
          AccountSelectorViewModel(
            accounts = viewModel.accounts,
            selectedAccountId = viewModel.selectedAccountId,
            validationError = viewModel.fieldErrors.get("accountId")
          )
        )
      ),
      
      // Date range selector
      div(
        cls := "mb-4 w-full",
        DateRangeSelector.render(
          DateRangeSelectorViewModel(
            startDate = viewModel.startDate,
            endDate = viewModel.endDate,
            validationError = 
              if viewModel.fieldErrors.contains("startDate") then viewModel.fieldErrors.get("startDate")
              else if viewModel.fieldErrors.contains("endDate") then viewModel.fieldErrors.get("endDate")
              else if viewModel.fieldErrors.contains("dateRange") then viewModel.fieldErrors.get("dateRange")
              else None
          )
        )
      ),
      
      // Import button
      div(
        cls := "flex",
        ImportButton.render(
          ImportButtonViewModel(
            isEnabled = !viewModel.hasErrors,
            isLoading = viewModel.isSubmitting,
            accountId = viewModel.selectedAccountId,
            startDate = viewModel.startDate,
            endDate = viewModel.endDate
          )
        )
      ),
      
      // Status indicator (if needed)
      viewModel.importStatus match
        case ImportStatus.NotStarted => ()
        case status =>
          div(
            id := "status-indicator-container",
            cls := "mt-2 flex items-center justify-end",
            StatusIndicator.render(
              StatusIndicatorViewModel(
                status = status,
                isVisible = true
              )
            )
          )
    )
```

### 5. Create Transaction Import Form View Model

```scala
case class TransactionImportFormViewModel(
  // Form data
  accounts: List[AccountOption] = AccountSelectorViewModel.defaultAccounts,
  selectedAccountId: Option[String] = None,
  startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
  endDate: LocalDate = LocalDate.now(),
  
  // Validation errors
  fieldErrors: Map[String, String] = Map.empty,
  globalError: Option[String] = None,
  
  // State
  isSubmitting: Boolean = false,
  importStatus: ImportStatus = ImportStatus.NotStarted,
  importResults: Option[ImportResults] = None
):
  def hasErrors: Boolean = fieldErrors.nonEmpty || globalError.isDefined
  
  def withValidationErrors(errors: ValidationErrors): TransactionImportFormViewModel =
    this.copy(
      fieldErrors = errors.errors,
      globalError = errors.globalErrors.headOption
    )
    
  def withImportResults(results: ImportResults): TransactionImportFormViewModel =
    this.copy(
      importResults = Some(results),
      importStatus = ImportStatus.Completed,
      isSubmitting = false
    )
    
  def withError(error: String): TransactionImportFormViewModel =
    this.copy(
      globalError = Some(error),
      importStatus = ImportStatus.Error,
      isSubmitting = false
    )
    
  def submitting: TransactionImportFormViewModel =
    this.copy(
      isSubmitting = true,
      importStatus = ImportStatus.InProgress
    )
```

### 6. Update TransactionImportModule

Simplify to have just two endpoints:

```scala
class TransactionImportModule(
  transactionImportView: TransactionImportView,
  transactionImportService: TransactionImportService
) extends TapirEndpointModule[Any]:

  // Base endpoint configuration
  private val baseEndpoint = endpoint
    .in("transactions" / "import")
    .errorOut(stringBody)
    .out(htmlBodyUtf8.map[Frag](raw)(_.render))

  // GET endpoint for the initial form page
  val importFormEndpoint = baseEndpoint
    .name("Transaction Import Form")
    .description("Display the transaction import form")
    .get
    
  // POST endpoint for form submission and validation
  val submitFormEndpoint = baseEndpoint
    .name("Submit Import Form")
    .description("Submit the import form for validation and processing")
    .post
    .in("submit")
    .in(formBody[Map[String, String]])
    
  // Implementation of the GET endpoint
  private def getImportForm: ZIO[Any, String, Frag] =
    for
      accounts <- transactionImportService.getAccounts()
      viewModel = TransactionImportFormViewModel(accounts = accounts)
    yield transactionImportView.renderImportForm(viewModel)
    
  // Implementation of the POST endpoint
  private def submitImportForm(formData: Map[String, String]): ZIO[Any, String, Frag] =
    for
      // Parse form data
      command <- ZIO.succeed(createCommand(formData))
      // Show submitting state
      initialViewModel = createViewModel(formData).submitting
      // Validate and potentially process
      result <- transactionImportService.validateAndProcess(command)
      // Create view model based on result
      viewModel = result match
        case Left(errors) => initialViewModel.withValidationErrors(errors)
        case Right(importResults) => initialViewModel.withImportResults(importResults)
    yield transactionImportView.renderImportForm(viewModel)
      
  // Helper to create command from form data
  private def createCommand(formData: Map[String, String]): TransactionImportCommand =
    TransactionImportCommand(
      accountId = formData.getOrElse("accountId", ""),
      startDate = formData.getOrElse("startDate", ""),
      endDate = formData.getOrElse("endDate", "")
    )
    
  // Helper to create initial view model from form data
  private def createViewModel(formData: Map[String, String]): TransactionImportFormViewModel =
    val startDate = Try(LocalDate.parse(formData.getOrElse("startDate", "")))
      .getOrElse(LocalDate.now().withDayOfMonth(1))
    val endDate = Try(LocalDate.parse(formData.getOrElse("endDate", "")))
      .getOrElse(LocalDate.now())
      
    TransactionImportFormViewModel(
      selectedAccountId = Option(formData.getOrElse("accountId", "")).filter(_.nonEmpty),
      startDate = startDate,
      endDate = endDate
    )
    
  // Server endpoints
  val importFormServerEndpoint = importFormEndpoint.zServerLogic(_ => getImportForm)
  val submitFormServerEndpoint = submitFormEndpoint.zServerLogic(submitImportForm)
  
  // List of all endpoints for documentation and routing
  override def endpoints = List(importFormEndpoint, submitFormEndpoint)
  override def serverEndpoints = List(importFormServerEndpoint, submitFormServerEndpoint)
```

### 7. Update TransactionImportView

Simplify to use the new form component:

```scala
class TransactionImportView(appShell: ScalatagsAppShell):
  /** Render the main import form page
   *
   * @param viewModel The form view model
   * @return The rendered HTML
   */
  def renderImportForm(viewModel: TransactionImportFormViewModel): Frag =
    appShell.wrap(
      pageTitle = "Transaction Import",
      content = div(
        cls := "container mx-auto px-4 py-8",
        // Header
        h1(
          cls := "text-2xl font-bold text-gray-800 mb-3 bg-blue-100 p-4 rounded-md",
          "Import Transactions from Fio Bank"
        ),
        // Help text
        div(
          cls := "mt-6 text-sm text-gray-500",
          p(
            "Note: This will import all transactions from the selected account and period. " +
              "Transactions will be categorized using predefined rules."
          )
        ),
        // The main form component
        TransactionImportForm.render(viewModel),
        // Results panel if applicable
        viewModel.importResults.map { results =>
          div(
            id := "results-panel-container",
            cls := "mt-6",
            ResultsPanel.render(
              ResultsPanelViewModel(
                importResults = Some(results),
                isVisible = true,
                startDate = viewModel.startDate,
                endDate = viewModel.endDate
              )
            )
          )
        }
      )
    )
```

### 8. Implement Mock TransactionImportService

Create a mock service for testing:

```scala
class MockTransactionImportService extends TransactionImportService:
  private val random = new Random()
  
  override def validateAndProcess(command: TransactionImportCommand): 
    ZIO[Any, String, Either[ValidationErrors, ImportResults]] =
    ZIO.succeed {
      // Parse dates to validate them
      val startDateResult = Try(LocalDate.parse(command.startDate))
      val endDateResult = Try(LocalDate.parse(command.endDate))
      val accountIdResult = AccountId.fromString(command.accountId)
      
      // Collect validation errors
      val errors = Map.newBuilder[String, String]
      
      // Validate account ID
      if command.accountId.isEmpty then
        errors += ("accountId" -> "Account selection is required")
      else accountIdResult match
        case Left(error) => errors += ("accountId" -> s"Invalid account ID: $error")
        case Right(_) => ()
      
      // Validate start date
      if command.startDate.isEmpty then
        errors += ("startDate" -> "Start date is required")
      else if startDateResult.isFailure then
        errors += ("startDate" -> "Invalid start date format")
        
      // Validate end date
      if command.endDate.isEmpty then
        errors += ("endDate" -> "End date is required")
      else if endDateResult.isFailure then
        errors += ("endDate" -> "Invalid end date format")
      
      // Cross-field validation for dates
      if startDateResult.isSuccess && endDateResult.isSuccess then
        val startDate = startDateResult.get
        val endDate = endDateResult.get
        
        if startDate.isAfter(endDate) then
          errors += ("dateRange" -> "Start date cannot be after end date")
        else if startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now()) then
          errors += ("dateRange" -> "Dates cannot be in the future")
        else if startDate.plusDays(90).isBefore(endDate) then
          errors += ("dateRange" -> "Date range cannot exceed 90 days (Fio Bank API limitation)")
      
      val validationErrors = ValidationErrors(errors.result())
      
      // If validation passes, generate a mock result
      if !validationErrors.hasErrors then
        Right(generateMockResults(startDateResult.get, endDateResult.get))
      else
        Left(validationErrors)
    }
    
  private def generateMockResults(startDate: LocalDate, endDate: LocalDate): ImportResults =
    val now = Instant.now()
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt + 1
    val transactionCount = if daysBetween <= 0 then 1 else (random.nextInt(daysBetween) + 1)
    
    ImportResults(
      transactionCount = transactionCount,
      errorMessage = None,
      startTime = now.minusSeconds(5),
      endTime = Some(now)
    )
    
  def getAccounts(): ZIO[Any, String, List[AccountOption]] =
    ZIO.succeed(AccountSelectorViewModel.defaultAccounts)
```

### 9. Update Component Tests

Update the component tests to reflect the new approach:

```scala
class TransactionImportFormSpec extends ZIOSpec[Any]:
  def spec = suite("TransactionImportForm")(
    test("should display validation errors for invalid input") {
      // Create view model with errors
      val viewModel = TransactionImportFormViewModel(
        fieldErrors = Map(
          "accountId" -> "Account selection is required",
          "dateRange" -> "Start date cannot be after end date"
        )
      )
      
      // Render form
      val html = TransactionImportForm.render(viewModel).render
      
      // Verify error messages are displayed
      assertTrue(
        html.contains("Account selection is required"),
        html.contains("Start date cannot be after end date")
      )
    },
    
    test("should disable submit button when errors exist") {
      // Create view model with errors
      val viewModel = TransactionImportFormViewModel(
        fieldErrors = Map("accountId" -> "Account selection is required")
      )
      
      // Render form
      val html = TransactionImportForm.render(viewModel).render
      
      // Verify button is disabled
      assertTrue(
        html.contains("disabled=\"disabled\"")
      )
    }
    // Add more tests...
  )
```

## Migration Strategy

1. Create the new components and services alongside existing ones
2. Update the module to support both approaches during transition
3. Switch the rendered view to use the new form component
4. Remove the old validation endpoints and components once transition is complete

## Benefits of This Approach

1. **Clearer Separation of Concerns**: UI components focus purely on display, domain focuses on validation
2. **Simpler Component Design**: Components don't need to know about validation rules
3. **More Consistent UX**: Form errors are displayed together with a unified submission flow
4. **Better Alignment with Domain**: Form maps directly to a domain command
5. **Simplified Error Handling**: All errors are processed in one place
6. **Easier Testing**: Pure components and isolated validation logic are easier to test
7. **More Maintainable**: Changes to validation rules only need to happen in one place

## Testing Strategy

1. **Unit Tests**: 
   - Test validation logic in isolation
   - Test form component rendering with different view models
   - Test error mapping and display

2. **Integration Tests**:
   - Test full submission flow with valid and invalid data
   - Test form rendering with validation errors
   - Test results display after successful submission

3. **End-to-End Tests**:
   - Test full user journeys for importing transactions
   - Test error handling in real browser environment