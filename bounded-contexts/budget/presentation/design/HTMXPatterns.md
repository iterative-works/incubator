# HTMX Interaction Patterns

This document outlines the HTMX patterns used in the Budget UI components, ensuring consistent, efficient server-side rendering with dynamic UI updates.

## Core HTMX Principles

1. **Hypermedia-Driven Application**: Use hypermedia controls to drive application state
2. **Progressive Enhancement**: Start with functional HTML forms, enhance with HTMX
3. **Targeted Updates**: Only update the parts of the page that need to change
4. **Minimal JavaScript**: Use declarative HTMX attributes over custom JS when possible
5. **Server-Side Rendering**: Keep presentation logic on the server with Scalatags

## Common HTMX Attributes

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `hx-get` | Make a GET request | `hx-get="/api/transactions"` |
| `hx-post` | Make a POST request | `hx-post="/api/categorize"` |
| `hx-put` | Make a PUT request | `hx-put="/api/transactions/123"` |
| `hx-target` | Target element to update | `hx-target="#transaction-list"` |
| `hx-swap` | How to swap content | `hx-swap="outerHTML"` |
| `hx-trigger` | Event to trigger request | `hx-trigger="change"` |
| `hx-indicator` | Element to show during request | `hx-indicator="#spinner"` |
| `hx-include` | Include additional form fields | `hx-include="[name='filter']"` |
| `hx-vals` | Values to include in request | `hx-vals='{"id": "123"}'` |
| `hx-push-url` | Update browser URL | `hx-push-url="true"` |

## Common Component Patterns

### 1. Filtering Pattern

Filter the transaction table based on user input with a delay:

```html
<input 
  type="text" 
  name="filter-text" 
  placeholder="Filter transactions..."
  hx-get="/api/transactions/filter"
  hx-target="#transaction-table-body"
  hx-trigger="keyup changed delay:500ms"
  hx-include="[name='filter-text']"
  hx-indicator="#filter-indicator"
/>

<div id="filter-indicator" class="htmx-indicator">
  <div class="spinner-sm"></div>
</div>
```

Server implementation would:
1. Receive the filter text
2. Filter the transactions
3. Render just the table body with Scalatags
4. Return HTML fragment

### 2. Sorting Pattern

Sort the transaction table when clicking column headers:

```html
<th 
  class="transaction-table__header {{sortClass}}" 
  hx-get="/api/transactions/sort"
  hx-target="#transaction-table-body"
  hx-swap="innerHTML"
  hx-include="[name='current-sort']"
  hx-vals='{"field": "date", "toggle": "true"}'
>
  Date <span class="sort-indicator">{{sortIndicator}}</span>
</th>

<!-- Hidden field to track current sort state -->
<input type="hidden" name="current-sort" value="date:desc" />
```

Server implementation would:
1. Receive the field name and current sort state
2. Toggle direction if same field, or set new field with default direction
3. Sort the transactions
4. Render the updated table body
5. Return HTML fragment with updated sort indicators

### 3. Modal Dialog Pattern

Open and close modal dialogs for imports and other operations:

```html
<!-- Trigger button -->
<button 
  id="import-button"
  class="button button--primary" 
  hx-get="/api/import-dialog"
  hx-target="#dialog-container"
  hx-swap="innerHTML"
>Import</button>

<!-- Dialog container -->
<div id="dialog-container"></div>
```

Server returns a modal dialog:

```html
<div 
  class="modal" 
  role="dialog" 
  aria-modal="true"
  tabindex="-1"
  hx-on:keyup="if(event.key === 'Escape') document.getElementById('cancel-button').click()"
>
  <div class="modal__content">
    <h2>Import Transactions</h2>
    <form
      hx-post="/api/import"
      hx-target="#transaction-table-container"
      hx-swap="outerHTML"
      hx-indicator="#import-spinner"
    >
      <!-- Form fields -->
      
      <div class="modal__footer">
        <button 
          id="cancel-button"
          type="button" 
          class="button button--secondary"
          hx-get="/api/cancel-import"
          hx-target="#dialog-container"
          hx-swap="innerHTML"
        >Cancel</button>
        
        <button type="submit" class="button button--primary">
          Import
          <span id="import-spinner" class="htmx-indicator spinner-sm"></span>
        </button>
      </div>
    </form>
  </div>
</div>
```

### 4. In-place Editing Pattern

Edit a transaction's category directly in the table:

```html
<!-- Default display mode -->
<td 
  class="transaction-table__cell transaction-table__cell--category"
  hx-get="/api/transactions/123/edit-category"
  hx-target="this"
  hx-swap="outerHTML"
  hx-trigger="click"
  tabindex="0"
  role="button"
  aria-label="Edit category Food"
>
  Food
</td>
```

Server returns editable version:

```html
<td class="transaction-table__cell transaction-table__cell--editing">
  <select
    name="category"
    hx-post="/api/transactions/123/categorize"
    hx-target="this parentNode"
    hx-swap="outerHTML"
    hx-trigger="change"
    hx-indicator="#cat-spinner-123"
    autofocus
  >
    <option value="">Select category</option>
    <option value="food" selected>Food</option>
    <option value="transportation">Transportation</option>
    <!-- Other options -->
  </select>
  <span id="cat-spinner-123" class="htmx-indicator spinner-sm"></span>
</td>
```

### 5. Selection State Pattern

Track selected transactions and update UI accordingly:

```html
<td class="transaction-table__cell">
  <input 
    type="checkbox" 
    name="selected-transactions" 
    value="123" 
    hx-post="/api/transactions/select"
    hx-vals='{"id": "123", "selected": "toggle"}'
    hx-target="#selection-status"
    hx-swap="innerHTML"
  />
</td>

<!-- Selection status element that gets updated -->
<div id="selection-status">
  <span class="selection-count">0 selected</span>
  <button 
    class="button button--primary button--disabled" 
    disabled
  >Submit to YNAB</button>
</div>
```

When selection changes, server returns:

```html
<span class="selection-count">2 selected</span>
<button 
  class="button button--primary" 
  hx-post="/api/transactions/submit-selected"
  hx-target="#notification-area"
>Submit to YNAB (2)</button>
```

### 6. Notification Pattern

Display and auto-dismiss notifications:

```html
<div id="notification-area"></div>
```

Server returns notification:

```html
<div 
  class="notification notification--success"
  hx-swap-oob="true"
  hx-trigger="load delay:5s"
  hx-get="/api/clear-notification"
  hx-target="#notification-area"
>
  <div class="notification__icon"></div>
  <p>4 transactions successfully submitted to YNAB</p>
  <button 
    class="notification__close"
    hx-get="/api/clear-notification"
    hx-target="#notification-area"
    hx-swap="outerHTML"
  >×</button>
</div>
```

## Advanced HTMX Techniques

### 1. Out-of-Band Swaps

Update multiple parts of the page in a single response:

```html
<!-- Original response target -->
<div id="primary-target">
  <!-- Updated content -->
</div>

<!-- Out of band swap for statistics -->
<div id="statistics-panel" hx-swap-oob="true">
  <!-- Updated statistics -->
</div>

<!-- Out of band swap for notification -->
<div id="notification-area" hx-swap-oob="true">
  <!-- Notification content -->
</div>
```

### 2. Request Chaining

Chain multiple requests in sequence:

```html
<button 
  hx-post="/api/transactions/submit"
  hx-target="#notification-area"
  hx-on::after-request="htmx.trigger('#statistics-panel', 'refresh')"
>Submit to YNAB</button>

<div 
  id="statistics-panel" 
  hx-get="/api/statistics"
  hx-trigger="refresh from:body"
  hx-target="this"
  hx-swap="outerHTML"
></div>
```

### 3. History Navigation Support

Support browser back/forward navigation:

```html
<button
  hx-get="/api/transactions/filter?text=grocery"
  hx-target="#transaction-table-body"
  hx-push-url="true"
>Filter Groceries</button>
```

### 4. Loading State Management

Show loading indicators during requests:

```html
<button 
  hx-post="/api/import"
  hx-indicator="#global-spinner"
>Import</button>

<div id="global-spinner" class="htmx-indicator">
  <div class="spinner-overlay">
    <div class="spinner"></div>
    <p>Processing...</p>
  </div>
</div>
```

### 5. Polling for Updates

Poll for updates to transaction status:

```html
<div
  id="transaction-status"
  hx-get="/api/transactions/status"
  hx-trigger="every 10s"
  hx-target="this"
  hx-swap="outerHTML"
>
  <!-- Status content -->
</div>
```

### 6. Error Handling

Handle error responses gracefully:

```html
<form
  hx-post="/api/import"
  hx-target="#transaction-table-container"
  hx-swap="outerHTML"
  hx-indicator="#import-spinner"
  hx-on:htmx:response-error="showError(event)"
>
  <!-- Form fields -->
</form>

<script>
  function showError(event) {
    // Create and show error notification
    const errorMessage = event.detail.xhr.responseText || "An error occurred";
    document.getElementById("notification-area").innerHTML = 
      `<div class="notification notification--error">
        <p>${errorMessage}</p>
        <button onclick="this.parentNode.remove()">×</button>
      </div>`;
  }
</script>
```

## Server-Side Implementation with Scalatags

The server-side implementation using Scalatags will render HTML fragments that match these patterns. Here's an example of how to render a transaction row:

```scala
def renderTransactionRow(transaction: TransactionViewModel): Frag =
  tr(
    id := s"transaction-row-${transaction.id}",
    cls := "transaction-row"
  )(
    td(
      cls := "transaction-table__cell"
    )(
      input(
        tpe := "checkbox",
        name := "selected-transactions",
        value := transaction.id,
        attr("hx-post") := "/api/transactions/select",
        attr("hx-vals") := s"""{"id": "${transaction.id}", "selected": "toggle"}""",
        attr("hx-target") := "#selection-status",
        attr("hx-swap") := "innerHTML"
      )
    ),
    td(cls := "transaction-table__cell")(transaction.date.toString),
    td(cls := "transaction-table__cell")(transaction.description),
    td(
      cls := s"transaction-table__cell ${amountClass(transaction.amount)}"
    )(formatAmount(transaction.amount)),
    td(
      cls := "transaction-table__cell transaction-table__cell--category",
      attr("hx-get") := s"/api/transactions/${transaction.id}/edit-category",
      attr("hx-target") := "this",
      attr("hx-swap") := "outerHTML",
      attr("hx-trigger") := "click",
      tabindex := "0",
      role := "button",
      aria.label := s"Edit category ${transaction.category.getOrElse("None")}"
    )(
      transaction.category.getOrElse("Uncategorized")
    ),
    td(cls := "transaction-table__cell")(
      renderStatusIndicator(transaction.status)
    )
  )

private def amountClass(amount: BigDecimal): String =
  if (amount > 0) "amount-positive" else "amount-negative"

private def formatAmount(amount: BigDecimal): String =
  f"$$$amount%.2f"

private def renderStatusIndicator(status: String): Frag =
  div(cls := "status-indicator")(
    span(cls := s"status-indicator__dot status-indicator__dot--$status"),
    span(cls := "status-indicator__label")(status)
  )
```

## Integration with Tapir Endpoints

The HTMX patterns will map to Tapir endpoints that return HTML fragments. Here's an example of how a Tapir endpoint would be defined:

```scala
val editCategoryEndpoint = endpoint.get
  .in("api" / "transactions" / path[String]("transactionId") / "edit-category")
  .out(htmlBodyUtf8)
  .errorOut(stringBody)

val editCategoryServerEndpoint = editCategoryEndpoint.serverLogic { transactionId =>
  for {
    transaction <- ZIO.serviceWithZIO[TransactionService](_.getTransaction(transactionId))
    availableCategories <- ZIO.serviceWithZIO[CategoryService](_.getCategories)
    html = renderCategoryDropdown(transaction, availableCategories).render
  } yield html
}.mapError(_.getMessage)

def renderCategoryDropdown(
  transaction: TransactionViewModel,
  categories: Seq[CategoryViewModel]
): Frag = {
  td(cls := "transaction-table__cell transaction-table__cell--editing")(
    select(
      name := "category",
      attr("hx-post") := s"/api/transactions/${transaction.id}/categorize",
      attr("hx-target") := "this parentNode",
      attr("hx-swap") := "outerHTML",
      attr("hx-trigger") := "change",
      attr("hx-indicator") := s"#cat-spinner-${transaction.id}",
      autofocus
    )(
      option(value := "")("Select category"),
      categories.map { category =>
        option(
          value := category.id,
          if (transaction.category.exists(_ == category.name)) selected else ""
        )(category.name)
      }
    ),
    span(
      id := s"cat-spinner-${transaction.id}", 
      cls := "htmx-indicator spinner-sm"
    )
  )
}
```

## Best Practices

1. **Clear Target Elements**: Always specify explicit targets for HTMX updates
2. **Consistent Swap Strategy**: Use a consistent swap strategy for similar interactions
3. **Minimal Dependencies**: Avoid additional JS libraries when HTMX can handle the interaction
4. **Progressive Enhancement**: Ensure forms work without JavaScript
5. **Consistent Loading Indicators**: Use the htmx-indicator class consistently
6. **Clear IDs and Selectors**: Use descriptive IDs for target elements
7. **Error Handling**: Always account for error responses
8. **State Management**: Store state in HTML attributes or hidden form fields
9. **URL Management**: Use hx-push-url for interactions that change the application state
10. **Accessibility**: Ensure HTMX-enhanced components maintain accessibility