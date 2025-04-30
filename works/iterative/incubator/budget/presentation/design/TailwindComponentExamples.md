# TailwindCSS Component Examples

This document provides examples of our UI components implemented with TailwindCSS classes and the `TailwindStyles` object. These examples demonstrate how to use our design system with TailwindCSS in a consistent way.

## Dashboard View

```scala
import scalatags.Text.all._
import works.iterative.incubator.budget.presentation.design.TailwindStyles

class DashboardView:
  def render(
    transactions: Seq[TransactionViewModel],
    statistics: StatisticsViewModel,
    selectedIds: Set[String] = Set.empty
  ): Frag =
    div(cls := TailwindStyles.container)(
      h1(cls := TailwindStyles.heading1)("Budget Dashboard"),
      
      // Statistics Panel
      StatisticsPanel.render(statistics),
      
      // Transaction Table with Import Button
      div(cls := TailwindStyles.TransactionTable.header)(
        div(cls := TailwindStyles.TransactionTable.filterContainer)(
          input(
            cls := TailwindStyles.TransactionTable.filter,
            tpe := "text",
            placeholder := "Filter transactions...",
            attr("hx-trigger") := "keyup changed delay:500ms",
            attr("hx-get") := "/api/transactions/filter",
            attr("hx-target") := "#transaction-table-body"
          )
        ),
        div(cls := TailwindStyles.TransactionTable.actionsContainer)(
          button(
            cls := TailwindStyles.secondaryButton,
            attr("hx-get") := "/api/import-dialog",
            attr("hx-target") := "#dialog-container"
          )("Import"),
          button(
            cls := if (selectedIds.nonEmpty) TailwindStyles.primaryButton else TailwindStyles.disabledButton,
            attr("hx-post") := "/api/transactions/submit",
            attr("hx-target") := "#notification-area",
            disabled := selectedIds.isEmpty
          )(s"Submit to YNAB${if (selectedIds.nonEmpty) s" (${selectedIds.size})" else ""}")
        )
      ),
      
      // Transaction Table
      div(cls := TailwindStyles.tableContainer)(
        table(cls := TailwindStyles.table)(
          thead(cls := TailwindStyles.tableHeader)(
            tr(
              th(cls := TailwindStyles.tableHeaderCell)(
                input(
                  cls := TailwindStyles.checkbox,
                  tpe := "checkbox",
                  attr("hx-post") := "/api/transactions/select-all",
                  attr("hx-target") := "#transaction-table-container"
                )
              ),
              th(
                cls := s"${TailwindStyles.tableHeaderCell} ${TailwindStyles.sortableHeader}",
                attr("hx-get") := "/api/transactions/sort?field=date",
                attr("hx-target") := "#transaction-table-body"
              )("Date ", span(cls := TailwindStyles.sortIndicatorDesc)("↓")),
              th(
                cls := s"${TailwindStyles.tableHeaderCell} ${TailwindStyles.sortableHeader}",
                attr("hx-get") := "/api/transactions/sort?field=description",
                attr("hx-target") := "#transaction-table-body"
              )("Description"),
              th(
                cls := s"${TailwindStyles.tableHeaderCell} ${TailwindStyles.sortableHeader}",
                attr("hx-get") := "/api/transactions/sort?field=amount",
                attr("hx-target") := "#transaction-table-body"
              )("Amount"),
              th(
                cls := s"${TailwindStyles.tableHeaderCell} ${TailwindStyles.sortableHeader}",
                attr("hx-get") := "/api/transactions/sort?field=category",
                attr("hx-target") := "#transaction-table-body"
              )("Category"),
              th(
                cls := s"${TailwindStyles.tableHeaderCell} ${TailwindStyles.sortableHeader}",
                attr("hx-get") := "/api/transactions/sort?field=status",
                attr("hx-target") := "#transaction-table-body"
              )("Status")
            )
          ),
          tbody(id := "transaction-table-body")(
            transactions.map(transaction => TransactionRowView.render(transaction, selectedIds.contains(transaction.id)))
          )
        )
      ),
      
      // Pagination
      div(cls := TailwindStyles.TransactionTable.pagination)(
        span(cls := TailwindStyles.TransactionTable.paginationText)("Page 1 of 3"),
        div(cls := TailwindStyles.TransactionTable.paginationControls)(
          button(cls := TailwindStyles.secondaryButton, disabled := true)("Previous"),
          button(
            cls := TailwindStyles.primaryButton,
            attr("hx-get") := "/api/transactions?page=2",
            attr("hx-target") := "#transaction-table-container"
          )("Next")
        )
      ),
      
      // Dialog container for modal dialogs
      div(id := "dialog-container"),
      
      // Notification area
      div(id := "notification-area")
    )
```

## Transaction Row

```scala
import scalatags.Text.all._
import works.iterative.incubator.budget.presentation.design.TailwindStyles

object TransactionRowView:
  def render(transaction: TransactionViewModel, isSelected: Boolean): Frag =
    tr(
      id := s"transaction-row-${transaction.id}",
      cls := TailwindStyles.tableRow
    )(
      td(cls := TailwindStyles.tableCell)(
        input(
          cls := TailwindStyles.checkbox,
          tpe := "checkbox",
          name := "selected-transactions",
          value := transaction.id,
          attr("hx-post") := "/api/transactions/select",
          attr("hx-vals") := s"""{"id": "${transaction.id}", "selected": "toggle"}""",
          attr("hx-target") := "#submission-controls",
          checked := isSelected
        )
      ),
      td(cls := TailwindStyles.tableCell)(
        transaction.date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
      ),
      td(cls := TailwindStyles.tableCell)(
        transaction.description
      ),
      td(cls := s"${TailwindStyles.tableCell} ${amountClass(transaction.amount)}")(
        formatAmount(transaction.amount)
      ),
      td(
        id := s"category-cell-${transaction.id}",
        cls := s"${TailwindStyles.tableCell} cursor-pointer",
        attr("hx-get") := s"/api/transactions/${transaction.id}/edit-category",
        attr("hx-target") := s"#category-cell-${transaction.id}",
        attr("hx-trigger") := "click"
      )(
        transaction.category.getOrElse(
          span(cls := "text-neutral-gray italic")("Uncategorized")
        )
      ),
      td(cls := TailwindStyles.tableCell)(
        renderStatus(transaction.status)
      )
    )

  private def amountClass(amount: BigDecimal): String =
    if (amount > 0) TailwindStyles.positiveAmount else TailwindStyles.negativeAmount

  private def formatAmount(amount: BigDecimal): String =
    f"${if (amount > 0) "+" else ""}$$$amount%.2f"

  private def renderStatus(status: String): Frag =
    div(cls := TailwindStyles.statusIndicator)(
      span(cls := statusDotClass(status)),
      span(cls := TailwindStyles.statusLabel)(status)
    )

  private def statusDotClass(status: String): String =
    status match
      case "Pending" => TailwindStyles.statusDotPending
      case "Processing" => TailwindStyles.statusDotProcessing
      case "Submitted" | "Reconciled" => TailwindStyles.statusDotComplete
      case "Error" => TailwindStyles.statusDotError
      case _ => TailwindStyles.statusDotPending
```

## Category Edit Cell

```scala
import scalatags.Text.all._
import works.iterative.incubator.budget.presentation.design.TailwindStyles

object CategoryEditCellView:
  def render(
    transactionId: String, 
    currentCategory: Option[String],
    availableCategories: Seq[CategoryViewModel]
  ): Frag =
    td(
      id := s"category-cell-${transactionId}",
      cls := TailwindStyles.tableCell
    )(
      select(
        cls := TailwindStyles.select,
        name := "category",
        attr("hx-post") := s"/api/transactions/${transactionId}/categorize",
        attr("hx-target") := s"#category-cell-${transactionId}",
        attr("hx-trigger") := "change",
        autofocus
      )(
        option(value := "", selected := currentCategory.isEmpty)(
          "Select category"
        ),
        availableCategories.map { category =>
          option(
            value := category.id,
            selected := currentCategory.contains(category.name)
          )(category.name)
        }
      ),
      span(
        id := s"category-spinner-${transactionId}",
        cls := "htmx-indicator ml-xs",
        attr("aria-hidden") := "true"
      )(
        // Inline spinner with Tailwind
        svg(
          cls := "animate-spin h-4 w-4 text-primary",
          xmlns := "http://www.w3.org/2000/svg",
          fill := "none",
          viewBox := "0 0 24 24"
        )(
          circle(
            cls := "opacity-25",
            cx := "12",
            cy := "12", 
            r := "10", 
            stroke := "currentColor",
            strokeWidth := "4"
          ),
          path(
            cls := "opacity-75",
            fill := "currentColor",
            d := "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          )
        )
      )
    )
```

## Import Dialog

```scala
import scalatags.Text.all._
import works.iterative.incubator.budget.presentation.design.TailwindStyles

object ImportDialogView:
  def render(startDate: Option[LocalDate] = None, endDate: Option[LocalDate] = None): Frag =
    div(
      cls := TailwindStyles.modalOverlay,
      role := "dialog",
      aria.modal := "true",
      aria.labelledby := "import-dialog-title"
    )(
      div(cls := TailwindStyles.ImportDialog.container)(
        // Header
        div(cls := TailwindStyles.modalHeader)(
          h2(id := "import-dialog-title", cls := TailwindStyles.modalTitle)(
            "Import Transactions"
          )
        ),
        
        // Body
        div(cls := TailwindStyles.modalBody)(
          form(
            cls := TailwindStyles.ImportDialog.form,
            attr("hx-post") := "/api/import",
            attr("hx-target") := "#transaction-table-container",
            attr("hx-swap") := "outerHTML",
            attr("hx-indicator") := "#import-spinner"
          )(
            div(cls := TailwindStyles.ImportDialog.dateFields)(
              // Start Date
              div(cls := TailwindStyles.formGroup)(
                label(cls := TailwindStyles.label, `for` := "start-date")(
                  "Start Date"
                ),
                input(
                  cls := TailwindStyles.input,
                  tpe := "date",
                  id := "start-date",
                  name := "start-date",
                  value := startDate.map(_.toString).getOrElse("")
                )
              ),
              
              // End Date
              div(cls := TailwindStyles.formGroup)(
                label(cls := TailwindStyles.label, `for` := "end-date")(
                  "End Date"
                ),
                input(
                  cls := TailwindStyles.input,
                  tpe := "date",
                  id := "end-date",
                  name := "end-date",
                  value := endDate.map(_.toString).getOrElse("")
                )
              )
            ),
            
            // Actions
            div(cls := TailwindStyles.ImportDialog.actions)(
              button(
                cls := TailwindStyles.secondaryButton,
                tpe := "button",
                attr("hx-get") := "/api/cancel-import",
                attr("hx-target") := "#dialog-container",
                attr("hx-swap") := "innerHTML"
              )("Cancel"),
              button(
                cls := TailwindStyles.primaryButton,
                tpe := "submit"
              )(
                "Import",
                span(
                  id := "import-spinner",
                  cls := "htmx-indicator ml-xs",
                  attr("aria-hidden") := "true"
                )(
                  // Inline spinner with Tailwind
                  svg(
                    cls := "animate-spin h-4 w-4 text-white",
                    xmlns := "http://www.w3.org/2000/svg",
                    fill := "none",
                    viewBox := "0 0 24 24"
                  )(
                    circle(
                      cls := "opacity-25",
                      cx := "12",
                      cy := "12", 
                      r := "10", 
                      stroke := "currentColor",
                      strokeWidth := "4"
                    ),
                    path(
                      cls := "opacity-75",
                      fill := "currentColor",
                      d := "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
```

## Statistics Panel

```scala
import scalatags.Text.all._
import works.iterative.incubator.budget.presentation.design.TailwindStyles

object StatisticsPanel:
  def render(stats: StatisticsViewModel): Frag =
    div(cls := TailwindStyles.StatisticsPanel.container)(
      // Total Transactions
      div(cls := TailwindStyles.StatisticsPanel.card)(
        span(cls := TailwindStyles.StatisticsPanel.label)(
          "Total Transactions"
        ),
        span(cls := TailwindStyles.StatisticsPanel.value)(
          stats.totalTransactions.toString
        )
      ),
      
      // Categorized Transactions
      div(cls := TailwindStyles.StatisticsPanel.card)(
        span(cls := TailwindStyles.StatisticsPanel.label)(
          "Categorized"
        ),
        span(cls := TailwindStyles.StatisticsPanel.value)(
          stats.categorizedTransactions.toString
        ),
        span(cls := TailwindStyles.StatisticsPanel.percentage)(
          f"(${stats.categorizedPercentage}%.0f%%)"
        )
      ),
      
      // Uncategorized Transactions
      div(cls := TailwindStyles.StatisticsPanel.card)(
        span(cls := TailwindStyles.StatisticsPanel.label)(
          "Uncategorized"
        ),
        span(cls := TailwindStyles.StatisticsPanel.value)(
          stats.uncategorizedTransactions.toString
        ),
        span(cls := TailwindStyles.StatisticsPanel.percentage)(
          f"(${stats.uncategorizedPercentage}%.0f%%)"
        )
      ),
      
      // Submitted Transactions
      div(cls := TailwindStyles.StatisticsPanel.card)(
        span(cls := TailwindStyles.StatisticsPanel.label)(
          "Submitted to YNAB"
        ),
        span(cls := TailwindStyles.StatisticsPanel.value)(
          stats.submittedTransactions.toString
        ),
        span(cls := TailwindStyles.StatisticsPanel.percentage)(
          f"(${stats.submittedPercentage}%.0f%%)"
        )
      )
    )
```

## Notification Component

```scala
import scalatags.Text.all._
import works.iterative.incubator.budget.presentation.design.TailwindStyles

object NotificationView:
  def render(
    notificationType: String,
    message: String,
    isDismissible: Boolean = true,
    autoDismiss: Boolean = true
  ): Frag =
    div(
      id := "notification-area",
      cls := notificationClass(notificationType),
      role := "alert",
      attr("aria-live") := "assertive",
      if (autoDismiss) Seq(
        attr("hx-swap-oob") := "true",
        attr("hx-trigger") := "load delay:5s",
        attr("hx-get") := "/api/clear-notification",
        attr("hx-target") := "#notification-area"
      ) else Seq.empty
    )(
      // Icon
      div(cls := TailwindStyles.notificationIcon)(
        notificationIcon(notificationType)
      ),
      
      // Content
      p(cls := TailwindStyles.notificationContent)(
        message
      ),
      
      // Close button (if dismissible)
      if (isDismissible) 
        button(
          cls := TailwindStyles.notificationClose,
          attr("hx-get") := "/api/clear-notification",
          attr("hx-target") := "#notification-area",
          attr("aria-label") := "Dismiss notification"
        )("×")
      else 
        ""
    )
  
  private def notificationClass(notificationType: String): String =
    notificationType match
      case "success" => TailwindStyles.notificationSuccess
      case "error" => TailwindStyles.notificationError
      case "info" => TailwindStyles.notificationInfo
      case "warning" => TailwindStyles.notificationWarning
      case _ => TailwindStyles.notificationInfo
  
  private def notificationIcon(notificationType: String): Frag =
    notificationType match
      case "success" => 
        svg(cls := "h-5 w-5 text-green-400", viewBox := "0 0 20 20", fill := "currentColor")(
          path(fillRule := "evenodd", d := "M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z", clipRule := "evenodd")
        )
      case "error" =>
        svg(cls := "h-5 w-5 text-red-400", viewBox := "0 0 20 20", fill := "currentColor")(
          path(fillRule := "evenodd", d := "M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z", clipRule := "evenodd")
        )
      case "warning" =>
        svg(cls := "h-5 w-5 text-yellow-400", viewBox := "0 0 20 20", fill := "currentColor")(
          path(fillRule := "evenodd", d := "M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z", clipRule := "evenodd")
        )
      case _ =>
        svg(cls := "h-5 w-5 text-blue-400", viewBox := "0 0 20 20", fill := "currentColor")(
          path(fillRule := "evenodd", d := "M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2h-1V9a1 1 0 00-1-1z", clipRule := "evenodd")
        )
```

## HTMX and TailwindCSS Integration Patterns

### 1. Showing Loading Indicators

```scala
button(
  cls := TailwindStyles.primaryButton,
  attr("hx-post") := "/api/submit",
  attr("hx-indicator") := "#loading-indicator"
)(
  "Submit",
  span(
    id := "loading-indicator",
    cls := "htmx-indicator ml-xs inline-block",
    attr("aria-hidden") := "true"
  )(
    svg(
      cls := "animate-spin h-4 w-4 text-white",
      xmlns := "http://www.w3.org/2000/svg",
      fill := "none",
      viewBox := "0 0 24 24"
    )(
      circle(
        cls := "opacity-25",
        cx := "12",
        cy := "12", 
        r := "10", 
        stroke := "currentColor",
        strokeWidth := "4"
      ),
      path(
        cls := "opacity-75",
        fill := "currentColor",
        d := "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      )
    )
  )
)
```

### 2. Focus Styles for Accessibility

```scala
input(
  cls := "border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500",
  tpe := "text",
  placeholder := "Search..."
)
```

### 3. Interactive Elements with Hover and Focus States

```scala
a(
  href := "#",
  cls := "text-blue-600 hover:text-blue-800 focus:outline-none focus:underline"
)("Learn more")
```

### 4. Responsive Designs

```scala
div(
  cls := "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"
)(
  // Grid items
)
```

### 5. Error States

```scala
div(cls := "mb-4")(
  label(cls := "block text-sm font-medium text-gray-700 mb-1")("Email"),
  input(
    cls := "border border-red-500 rounded w-full px-3 py-2 focus:outline-none focus:ring-2 focus:ring-red-500",
    tpe := "email",
    aria.invalid := "true",
    aria.describedby := "email-error"
  ),
  p(id := "email-error", cls := "mt-1 text-sm text-red-600")(
    "Please enter a valid email address."
  )
)
```