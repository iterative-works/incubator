# UI Component Implementation Plan for VS001

## Overview

This document outlines the implementation plan for the UI components of the Basic Transaction Import vertical slice (VS001). Having completed the view models, we will now focus on creating the UI components using Scalatags and Tailwind CSS.

## Implementation Strategy

We will take a bottom-up approach, implementing individual components first and then composing them into larger components and pages. This approach enables:

1. Better testability of individual components
2. Reuse of components across different scenarios
3. Incremental development and validation

## Component Implementation Order

We'll implement components in the following order:

1. **TailwindClasses Utility** - Create shared Tailwind classes for consistent styling
2. **Basic UI Components** - Implement low-level UI components:
   - AccountSelector
   - DateRangeSelector
   - ImportButton
   - StatusIndicator
3. **Composite Components** - Implement composed components:
   - ImportActionPanel (contains ImportButton and StatusIndicator)
   - TransactionCountSummary
   - ErrorMessageDisplay
   - RetryButton
   - ResultsPanel (contains TransactionCountSummary, ErrorMessageDisplay, and RetryButton)
4. **Page Component** - Implement the main import page component
5. **Web Module** - Create the web module with HTTP routes

## Implementation Details

### 1. TailwindClasses Utility

Create a utility object with Tailwind class constants for consistent styling:

```scala
object TailwindClasses:
  // Button styles
  val primaryButton = "py-2 px-4 rounded-md font-medium text-white bg-ynab-blue hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
  val dangerButton = "py-2 px-4 rounded-md font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 transition-colors"
  
  // Form styles
  val formControl = "w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
  val formLabel = "block text-sm font-medium text-gray-700 mb-1"
  
  // Card styles
  val card = "bg-white shadow-md rounded-lg p-6"
  
  // Text styles
  val heading = "text-lg font-semibold mb-3"
  val successHeading = s"$heading text-green-700"
  val errorHeading = s"$heading text-red-700"
```

### 2. AccountSelector Component

Implement the AccountSelector view:

```scala
object AccountSelector:
  def render(viewModel: AccountSelectorViewModel): Frag =
    div(
      cls := "mb-4",
      h2(
        cls := "text-lg font-semibold mb-2 text-gray-700",
        "Select Account"
      ),
      div(
        cls := "relative",
        select(
          cls := s"w-full px-3 py-2 border rounded-md ${validationClass(viewModel)}",
          id := "account-selector",
          name := "accountId",
          hx_post := "/validate-account",
          hx_trigger := "change",
          hx_target := "#account-selector-container",
          hx_swap := "outerHTML",
          // Default empty option
          option(
            value := "",
            selected := viewModel.selectedAccountId.isEmpty,
            disabled := true,
            "-- Select an account --"
          ),
          // Generate options for each account
          viewModel.accounts.map { account =>
            option(
              value := account.id,
              selected := viewModel.selectedAccountId.contains(account.id),
              account.name
            )
          }
        )
      ),
      // Error message
      viewModel.validationError.map { error =>
        div(
          cls := "text-red-500 text-sm mt-1",
          error
        )
      }
    )
    
  private def validationClass(viewModel: AccountSelectorViewModel): String =
    if viewModel.validationError.isDefined then
      "border-red-500 focus:border-red-500 focus:ring-red-500"
    else
      "border-gray-300 focus:border-blue-500 focus:ring-blue-500"
```

### 3. DateRangeSelector Component

Implement the DateRangeSelector view:

```scala
class DateRangeSelectorView:
  def render(viewModel: DateRangeSelectorViewModel): Frag =
    div(cls := "p-4 bg-white rounded-lg shadow-md")(
      div(cls := "mb-4")(
        label(forId := "start-date", cls := TailwindClasses.formLabel)("Start Date"),
        input(
          tpe := "date",
          id := "start-date",
          name := "startDate",
          cls := TailwindClasses.formControl,
          value := viewModel.startDate.toString,
          data("hx-post") := "/validate-dates",
          data("hx-trigger") := "change",
          max := viewModel.maxDate.toString
        )
      ),
      div(cls := "mb-4")(
        label(forId := "end-date", cls := TailwindClasses.formLabel)("End Date"),
        input(
          tpe := "date",
          id := "end-date",
          name := "endDate",
          cls := TailwindClasses.formControl,
          value := viewModel.endDate.toString,
          data("hx-post") := "/validate-dates",
          data("hx-trigger") := "change",
          max := viewModel.maxDate.toString
        )
      ),
      div(
        cls := "text-sm text-red-600 mt-2",
        style := s"display: ${if viewModel.hasError then "block" else "none"}"
      )(
        viewModel.validationError.getOrElse("")
      )
    )
```

### 4. ImportButton Component

Implement the ImportButton view:

```scala
class ImportButtonView:
  def render(viewModel: ImportButtonViewModel): Frag =
    button(
      id := "import-button",
      cls := List(
        TailwindClasses.primaryButton -> true,
        "w-full flex items-center justify-center" -> true,
        "disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-blue-400" -> true
      ),
      disabled := viewModel.isDisabled,
      data("hx-post") := "/import-transactions",
      data("hx-target") := "#results-panel",
      data("hx-indicator") := "#status-indicator",
      data("hx-disabled") := viewModel.isDisabled.toString,
      data("hx-vals") := s"""{"accountId": "${viewModel.accountId.getOrElse("")}", "startDate": "${viewModel.startDate}", "endDate": "${viewModel.endDate}"}"""
    )(
      if viewModel.isLoading then
        frag(
          svg(cls := "animate-spin -ml-1 mr-3 h-5 w-5 text-white", xmlns := "http://www.w3.org/2000/svg", fill := "none", viewBox := "0 0 24 24")(
            circle(cls := "opacity-25", cx := "12", cy := "12", r := "10", stroke := "currentColor", strokeWidth := "4"),
            path(cls := "opacity-75", fill := "currentColor", d := "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z")
          ),
          "Importing..."
        )
      else
        "Import Transactions"
    )
```

### 5. StatusIndicator Component

Implement the StatusIndicator view:

```scala
class StatusIndicatorView:
  def render(viewModel: StatusIndicatorViewModel): Frag =
    div(
      id := "status-indicator",
      cls := "mt-4 flex items-center",
      style := s"display: ${if viewModel.isVisible then "flex" else "none"}"
    )(
      // Loading spinner
      div(
        cls := "animate-spin h-6 w-6 mr-3 text-blue-500",
        style := s"display: ${if viewModel.showLoadingSpinner then "block" else "none"}"
      )(
        svg(xmlns := "http://www.w3.org/2000/svg", fill := "none", viewBox := "0 0 24 24")(
          circle(cls := "opacity-25", cx := "12", cy := "12", r := "10", stroke := "currentColor", strokeWidth := "4"),
          path(cls := "opacity-75", fill := "currentColor", d := "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z")
        )
      ),
      
      // Success checkmark
      div(
        cls := "text-green-500 h-6 w-6 mr-3",
        style := s"display: ${if viewModel.showSuccessIcon then "block" else "none"}"
      )(
        svg(xmlns := "http://www.w3.org/2000/svg", fill := "none", viewBox := "0 0 24 24", stroke := "currentColor")(
          path(strokeLinecap := "round", strokeLinejoin := "round", strokeWidth := "2", d := "M5 13l4 4L19 7")
        )
      ),
      
      // Error icon
      div(
        cls := "text-red-500 h-6 w-6 mr-3",
        style := s"display: ${if viewModel.showErrorIcon then "block" else "none"}"
      )(
        svg(xmlns := "http://www.w3.org/2000/svg", fill := "none", viewBox := "0 0 24 24", stroke := "currentColor")(
          path(strokeLinecap := "round", strokeLinejoin := "round", strokeWidth := "2", d := "M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z")
        )
      ),
      
      // Status text
      div(cls := "text-sm font-medium text-gray-700")(viewModel.getStatusText)
    )
```

### 6. Import Action Panel Component

Implement the ImportActionPanel view which contains ImportButton and StatusIndicator:

```scala
class ImportActionPanelView(
  importButtonView: ImportButtonView,
  statusIndicatorView: StatusIndicatorView
):
  def render(viewModel: ImportActionPanelViewModel): Frag =
    div(cls := "mt-6")(
      importButtonView.render(
        ImportButtonViewModel(
          isEnabled = viewModel.isEnabled,
          isLoading = viewModel.isLoading,
          startDate = viewModel.startDate,
          endDate = viewModel.endDate
        )
      ),
      statusIndicatorView.render(
        StatusIndicatorViewModel(
          status = viewModel.status,
          isVisible = viewModel.showStatusIndicator
        )
      )
    )
```

### 7. Transaction Count Summary Component

Implement the TransactionCountSummary view:

```scala
class TransactionCountSummaryView:
  def render(viewModel: TransactionCountSummaryViewModel): Frag =
    div(
      cls := TailwindClasses.card,
      style := s"display: ${if viewModel.isVisible then "block" else "none"}"
    )(
      h3(cls := TailwindClasses.successHeading)("Import Successful"),
      p(cls := "text-gray-700 mb-2")(
        "Successfully imported ",
        span(cls := "font-bold text-gray-900")(viewModel.formattedTransactionCount),
        " from ",
        span(cls := "font-bold text-gray-900")(viewModel.formattedStartDate),
        " to ",
        span(cls := "font-bold text-gray-900")(viewModel.formattedEndDate),
        "."
      ),
      p(cls := "text-sm text-gray-600 mb-4")(
        s"Completed in ${viewModel.formattedCompletionTime}."
      ),
      a(
        href := "/transactions",
        cls := TailwindClasses.primaryButton + " inline-block"
      )(
        "View imported transactions"
      )
    )
```

### 8. Error Message Display Component

Implement the ErrorMessageDisplay view:

```scala
class ErrorMessageDisplayView:
  def render(viewModel: ErrorMessageDisplayViewModel): Frag =
    div(
      cls := TailwindClasses.card + " border-l-4 border-red-500",
      style := s"display: ${if viewModel.isVisible then "block" else "none"}"
    )(
      h3(cls := TailwindClasses.errorHeading)(viewModel.errorTitle),
      p(cls := "text-red-700 mb-4")(viewModel.errorMessage),
      p(cls := "text-sm text-gray-600 mb-4")(
        viewModel.guidanceMessage
      )
    )
```

### 9. Retry Button Component

Implement the RetryButton view:

```scala
class RetryButtonView:
  def render(viewModel: RetryButtonViewModel): Frag =
    button(
      id := "retry-button",
      cls := TailwindClasses.dangerButton + " mt-4 flex items-center",
      style := s"display: ${if viewModel.isVisible then "block" else "none"}",
      disabled := viewModel.isDisabled,
      data("hx-post") := "/import-transactions",
      data("hx-target") := "#results-panel",
      data("hx-indicator") := "#status-indicator",
      data("hx-vals") := s"""{"accountId": "${viewModel.accountId.getOrElse("")}", "startDate": "${viewModel.startDate}", "endDate": "${viewModel.endDate}"}"""
    )(
      svg(
        cls := "h-5 w-5 mr-2",
        xmlns := "http://www.w3.org/2000/svg",
        fill := "none",
        viewBox := "0 0 24 24",
        stroke := "currentColor"
      )(
        path(
          strokeLinecap := "round",
          strokeLinejoin := "round",
          strokeWidth := "2",
          d := "M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
        )
      ),
      viewModel.buttonText
    )
```

### 10. Results Panel Component

Implement the ResultsPanel view which contains TransactionCountSummary, ErrorMessageDisplay, and RetryButton:

```scala
class ResultsPanelView(
  transactionCountSummaryView: TransactionCountSummaryView,
  errorMessageDisplayView: ErrorMessageDisplayView,
  retryButtonView: RetryButtonView
):
  def render(viewModel: ResultsPanelViewModel): Frag =
    div(id := "results-panel", style := s"display: ${if viewModel.isVisible then "block" else "none"}")(
      // Transaction count summary
      if viewModel.showSuccessSummary then
        transactionCountSummaryView.render(
          TransactionCountSummaryViewModel(
            transactionCount = viewModel.transactionCount,
            startDate = viewModel.startDate,
            endDate = viewModel.endDate,
            completionTimeSeconds = viewModel.completionTimeSeconds
          )
        )
      else
        frag(),
        
      // Error message
      if viewModel.showErrorMessage then
        frag(
          errorMessageDisplayView.render(
            ErrorMessageDisplayViewModel(
              errorMessage = viewModel.errorMessage.getOrElse("Unknown error occurred"),
              errorCode = viewModel.errorCode
            )
          ),
          
          // Retry button
          if viewModel.showRetryButton then
            retryButtonView.render(
              RetryButtonViewModel(
                isVisible = true,
                startDate = viewModel.startDate,
                endDate = viewModel.endDate
              )
            )
          else
            frag()
        )
      else
        frag()
    )
```

### 11. Import Page Component

Implement the main import page component:

```scala
class ImportPageView(
  accountSelectorView: AccountSelectorView,
  dateRangeSelectorView: DateRangeSelectorView,
  importActionPanelView: ImportActionPanelView,
  resultsPanelView: ResultsPanelView
):
  def render(viewModel: ImportPageViewModel): Frag =
    div(cls := "container mx-auto px-4 py-8 max-w-2xl")(
      // Page header
      div(cls := "mb-6")(
        h1(cls := "text-2xl font-bold text-gray-900")("Import Transactions"),
        p(cls := "text-gray-600")("Import your transactions from Fio Bank for the selected account and date range.")
      ),
      
      // Account selector
      accountSelectorView.render(
        AccountSelectorViewModel(
          accounts = viewModel.accounts,
          selectedAccountId = viewModel.selectedAccountId,
          validationError = viewModel.accountValidationError
        )
      ),
      
      // Date range selector
      dateRangeSelectorView.render(
        DateRangeSelectorViewModel(
          startDate = viewModel.startDate,
          endDate = viewModel.endDate,
          validationError = viewModel.validationError
        )
      ),
      
      // Import action panel
      importActionPanelView.render(
        ImportActionPanelViewModel(
          isEnabled = viewModel.isValid,
          status = viewModel.importStatus,
          accountId = viewModel.selectedAccountId,
          startDate = viewModel.startDate,
          endDate = viewModel.endDate
        )
      ),
      
      // Results panel
      resultsPanelView.render(
        ResultsPanelViewModel(
          importResults = viewModel.importResults,
          isVisible = viewModel.showResults,
          startDate = viewModel.startDate,
          endDate = viewModel.endDate
        )
      )
    )
```

### 12. Mock Import Service

Create a mock service for UI development:

```scala
class MockImportService:
  def importTransactions(startDate: LocalDate, endDate: LocalDate): Task[ImportResults] =
    // Simulate API call delay
    for
      _ <- ZIO.sleep(Duration.fromSeconds(2))
      result <- simulateImportResult(startDate, endDate)
    yield result

  private def simulateImportResult(startDate: LocalDate, endDate: LocalDate): Task[ImportResults] =
    // For demo purposes, generate different results based on date ranges
    val now = Instant.now()

    if startDate.getYear < 2000 then
      // Simulate error for very old dates
      ZIO.succeed(ImportResults(
        transactionCount = 0,
        errorMessage = Some("Cannot import transactions from before 2000"),
        startTime = now,
        endTime = Some(now.plusSeconds(2))
      ))
    else if startDate.isEqual(endDate) && startDate.getDayOfMonth == 15 then
      // Simulate no transactions on the 15th of any month
      ZIO.succeed(ImportResults(
        transactionCount = 0,
        errorMessage = None,
        startTime = now,
        endTime = Some(now.plusSeconds(1))
      ))
    else
      // Normal case: generate random transaction count
      val daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1
      val count = math.max(1, (daysBetween * (2 + Random.nextInt(5))).toInt)

      ZIO.succeed(ImportResults(
        transactionCount = count,
        errorMessage = None,
        startTime = now,
        endTime = Some(now.plusSeconds(1 + Random.nextInt(3)))
      ))
```

### 13. Import Module

Create the web module with routes:

```scala
class TransactionImportModule(
  appShell: ScalatagsAppShell,
  importPageView: ImportPageView,
  importService: ImportService
) extends ZIOWebModule[AppEnv]:
  import zio.*
  import zhttp.http.*

  override def routes: HttpApp[AppEnv, Throwable] =
    Http.collectZIO[Request] {
      // Main import page route
      case Method.GET -> Root / "import" =>
        val viewModel = ImportPageViewModel()
        ZIO.succeed(
          Response.html(
            appShell.wrap(
              "Import Transactions",
              importPageView.render(viewModel)
            )
          )
        )
        
      // Date validation route
      case req @ Method.POST -> Root / "validate-dates" =>
        for
          formData <- req.bodyAsFormData
          startDateStr = formData.getOrElse("startDate", "")
          endDateStr = formData.getOrElse("endDate", "")
          response <- validateDates(startDateStr, endDateStr)
        yield response
        
      // Import transactions route
      case req @ Method.POST -> Root / "import-transactions" =>
        for
          formData <- req.bodyAsFormData
          startDateStr = formData.getOrElse("startDate", "")
          endDateStr = formData.getOrElse("endDate", "")
          response <- importTransactions(startDateStr, endDateStr)
        yield response
    }
    
  private def validateDates(startDateStr: String, endDateStr: String): UIO[Response] =
    try
      val startDate = if startDateStr.isEmpty then LocalDate.now().withDayOfMonth(1) else LocalDate.parse(startDateStr)
      val endDate = if endDateStr.isEmpty then LocalDate.now() else LocalDate.parse(endDateStr)
      
      val validationError = 
        if startDate.isAfter(endDate) then
          Some("Start date cannot be after end date")
        else if startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now()) then
          Some("Dates cannot be in the future")
        else if ChronoUnit.DAYS.between(startDate, endDate) > 90 then
          Some("Date range cannot exceed 90 days (Fio Bank API limitation)")
        else
          None
          
      val viewModel = DateRangeSelectorViewModel(
        startDate = startDate,
        endDate = endDate,
        validationError = validationError
      )
      
      ZIO.succeed(
        Response.html(
          new DateRangeSelectorView().render(viewModel).render
        )
      )
    catch
      case _: Exception =>
        val viewModel = DateRangeSelectorViewModel(
          startDate = LocalDate.now().withDayOfMonth(1),
          endDate = LocalDate.now(),
          validationError = Some("Invalid date format")
        )
        
        ZIO.succeed(
          Response.html(
            new DateRangeSelectorView().render(viewModel).render
          )
        )
        
  private def importTransactions(startDateStr: String, endDateStr: String): ZIO[ImportService, Throwable, Response] =
    try
      val startDate = if startDateStr.isEmpty then LocalDate.now().withDayOfMonth(1) else LocalDate.parse(startDateStr)
      val endDate = if endDateStr.isEmpty then LocalDate.now() else LocalDate.parse(endDateStr)
      
      for
        viewModel <- ZIO.succeed(
          ImportPageViewModel(
            startDate = startDate,
            endDate = endDate,
            importStatus = ImportStatus.InProgress
          )
        )
        initialResponse = Response.html(
          resultsPanelView.render(
            ResultsPanelViewModel(
              importResults = None,
              isVisible = true,
              startDate = startDate,
              endDate = endDate
            )
          ).render
        )
        results <- importService.importTransactions(startDate, endDate)
        finalViewModel = ImportPageViewModel(
          startDate = startDate,
          endDate = endDate,
          importStatus = if results.isSuccess then ImportStatus.Completed else ImportStatus.Error,
          importResults = Some(results)
        )
        finalResponse = Response.html(
          resultsPanelView.render(
            ResultsPanelViewModel(
              importResults = Some(results),
              isVisible = true,
              startDate = startDate,
              endDate = endDate
            )
          ).render
        )
      yield finalResponse
    catch
      case e: Exception =>
        ZIO.succeed(
          Response.html(
            resultsPanelView.render(
              ResultsPanelViewModel(
                importResults = Some(ImportResults(
                  transactionCount = 0,
                  errorMessage = Some(s"Invalid request parameters: ${e.getMessage}"),
                  startTime = Instant.now(),
                  endTime = Some(Instant.now())
                )),
                isVisible = true,
                startDate = LocalDate.now().withDayOfMonth(1),
                endDate = LocalDate.now()
              )
            ).render
          )
        )
```

## Testing Strategy

1. **Component Unit Tests** - Test each component in isolation with various view model states
2. **Integration Tests** - Test component composition and interactions
3. **Manual Scenario Tests** - Manually validate each user scenario in the browser

## Next Steps

1. Implement the TailwindClasses utility
2. Implement the individual view components
3. Implement the composed components
4. Create the mock service
5. Implement the web module
6. Test all scenarios
7. Schedule user feedback session

## Implementation Timeline

| Component | Target Completion | Owner |
|-----------|------------------|-------|
| TailwindClasses Utility | 2025-05-03 | Team |
| Basic UI Components | 2025-05-03 | Team |
| Composite Components | 2025-05-04 | Team |
| Mock Service | 2025-05-04 | Team |
| Web Module | 2025-05-04 | Team |
| Scenario Testing | 2025-05-05 | Team |
| User Feedback Session | 2025-05-05 | Team |