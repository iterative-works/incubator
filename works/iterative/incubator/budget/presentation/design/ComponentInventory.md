# Budget UI Component Inventory

This document catalogs all UI components needed to implement the Budget feature scenarios. Each component is defined with its type, purpose, properties, and scenario coverage.

## Container Components

### DashboardView
- **Type**: Container
- **Purpose**: Main application view that hosts all dashboard content
- **Properties**:
  - `title` - Page title 
  - `children` - Content components
- **Scenarios**: UI-1.1, UI-2.1, UI-3.1, UI-4.1, UI-5.1, UI-6.1
- **Markup Pattern**:
```html
<div class="dashboard">
  <h1 class="dashboard__title">{{title}}</h1>
  <div class="dashboard__content">
    {{children}}
  </div>
</div>
```

### StatisticsPanel
- **Type**: Display
- **Purpose**: Show transaction summary statistics
- **Properties**:
  - `totalTransactions` - Count of transactions
  - `totalAmount` - Sum of all transactions
  - `pendingCount` - Count of pending transactions
  - `categorizedCount` - Count of categorized transactions
  - `uncategorizedCount` - Count of uncategorized transactions
  - `submittedCount` - Count of submitted transactions
- **Scenarios**: UI-1.2
- **Markup Pattern**:
```html
<div class="statistics-panel">
  <div class="statistics-panel__card">
    <h3 class="statistics-panel__label">Total Transactions</h3>
    <span class="statistics-panel__value">{{totalTransactions}}</span>
  </div>
  <div class="statistics-panel__card">
    <h3 class="statistics-panel__label">Total Amount</h3>
    <span class="statistics-panel__value statistics-panel__value--{{amountClass}}">{{totalAmount}}</span>
  </div>
  <!-- Additional statistics cards -->
</div>
```

### TransactionTable
- **Type**: Interactive Table
- **Purpose**: Display and interact with transactions
- **Properties**:
  - `transactions` - List of transaction view models
  - `sortField` - Currently sorted field
  - `sortDirection` - Direction of sort (asc/desc)
  - `filterText` - Current filter text
  - `selectedIds` - List of selected transaction IDs
- **Scenarios**: UI-1.5, UI-3.2-3.5, UI-4.2-4.5, UI-5.2-5.5, UI-6.2-6.4
- **Markup Pattern**:
```html
<div class="transaction-table-container">
  <div class="transaction-table__controls">
    <input 
      class="transaction-table__filter" 
      type="text" 
      placeholder="Filter transactions..."
      hx-trigger="keyup changed delay:500ms"
      hx-get="/api/transactions/filter"
      hx-target="#transaction-table-body"
      hx-include="[name='filter-text']"
    />
    <button class="button button--primary">Submit Selected</button>
  </div>
  
  <table class="transaction-table">
    <thead>
      <tr>
        <th class="transaction-table__header--checkbox">
          <input type="checkbox" class="checkbox" />
        </th>
        <th 
          class="transaction-table__header {{sortClasses.date}}" 
          hx-get="/api/transactions/sort?field=date"
          hx-target="#transaction-table-body"
        >Date</th>
        <th 
          class="transaction-table__header {{sortClasses.description}}"
          hx-get="/api/transactions/sort?field=description"
          hx-target="#transaction-table-body"
        >Description</th>
        <!-- Additional headers -->
      </tr>
    </thead>
    <tbody id="transaction-table-body">
      <!-- Transaction rows -->
    </tbody>
  </table>
</div>
```

### ImportDialog
- **Type**: Modal Dialog
- **Purpose**: Configure and initiate transaction imports
- **Properties**:
  - `startDate` - Import start date
  - `endDate` - Import end date
  - `isOpen` - Dialog visibility state
- **Scenarios**: UI-2.3-2.6
- **Markup Pattern**:
```html
<div class="modal" id="import-dialog">
  <div class="modal__content">
    <h2 class="modal__title">Import Transactions</h2>
    <form class="modal__form">
      <div class="form-group">
        <label for="start-date">Start Date</label>
        <input type="date" id="start-date" name="start-date" class="input" />
      </div>
      <div class="form-group">
        <label for="end-date">End Date</label>
        <input type="date" id="end-date" name="end-date" class="input" />
      </div>
      <div class="modal__actions">
        <button type="button" class="button button--secondary" hx-get="/api/cancel-import">Cancel</button>
        <button type="submit" class="button button--primary" hx-post="/api/import" hx-include="[name='start-date'],[name='end-date']">Import</button>
      </div>
    </form>
  </div>
</div>
```

## Interactive Components

### ImportButton
- **Type**: Action Button
- **Purpose**: Trigger import workflow
- **Properties**:
  - `label` - Button text
- **Scenarios**: UI-1.4, UI-2.2
- **Markup Pattern**:
```html
<button 
  class="button button--primary" 
  hx-get="/api/import-dialog"
  hx-target="#dialog-container"
  hx-swap="innerHTML"
>
  {{label}}
</button>
```

### DatePicker
- **Type**: Form Control
- **Purpose**: Select date ranges
- **Properties**:
  - `id` - Input identifier
  - `name` - Form field name
  - `label` - Field label
  - `value` - Selected date
- **Scenarios**: UI-2.4, UI-2.7
- **Markup Pattern**:
```html
<div class="form-group">
  <label for="{{id}}">{{label}}</label>
  <input 
    type="date" 
    id="{{id}}" 
    name="{{name}}" 
    class="input" 
    value="{{value}}" 
  />
</div>
```

### CategoryDropdown
- **Type**: Form Control
- **Purpose**: Select transaction categories
- **Properties**:
  - `id` - Input identifier
  - `transactionId` - ID of transaction being categorized
  - `categories` - Available categories
  - `selectedCategory` - Currently selected category
- **Scenarios**: UI-4.3-4.4
- **Markup Pattern**:
```html
<select 
  id="{{id}}" 
  class="select" 
  hx-post="/api/transactions/{{transactionId}}/categorize"
  hx-target="#transaction-row-{{transactionId}}"
  hx-swap="outerHTML"
>
  <option value="">Select category</option>
  {{#each categories}}
    <option value="{{id}}" {{#if selected}}selected{{/if}}>{{name}}</option>
  {{/each}}
</select>
```

### SelectionControls
- **Type**: Checkbox Group
- **Purpose**: Select multiple transactions
- **Properties**:
  - `transactionIds` - List of all transaction IDs
  - `selectedIds` - List of selected transaction IDs
- **Scenarios**: UI-5.2
- **Markup Pattern**:
```html
<!-- Master checkbox in table header -->
<input 
  type="checkbox" 
  class="checkbox" 
  id="select-all" 
  hx-post="/api/transactions/select-all"
  hx-target="#selection-controls"
  hx-swap="outerHTML"
/>

<!-- Per-row checkboxes -->
<td class="transaction-table__cell">
  <input 
    type="checkbox" 
    class="checkbox" 
    name="selected-transactions" 
    value="{{transactionId}}" 
    {{#if selected}}checked{{/if}}
    hx-post="/api/transactions/toggle-selection"
    hx-vals='{"id": "{{transactionId}}"}'
    hx-target="#submit-button"
    hx-swap="outerHTML"
  />
</td>
```

### SubmitButton
- **Type**: Action Button
- **Purpose**: Submit transactions to YNAB
- **Properties**:
  - `label` - Button text
  - `selectedCount` - Number of selected transactions
  - `isEnabled` - Whether button is clickable
- **Scenarios**: UI-5.3
- **Markup Pattern**:
```html
<button 
  id="submit-button"
  class="button {{#if isEnabled}}button--primary{{else}}button--disabled{{/if}}" 
  hx-post="/api/transactions/submit"
  hx-target="#notification-area"
  {{#unless isEnabled}}disabled{{/unless}}
>
  {{label}} {{#if selectedCount}}({{selectedCount}}){{/if}}
</button>
```

## Indicator Components

### StatusIndicator
- **Type**: Visual Indicator
- **Purpose**: Display transaction status
- **Properties**:
  - `status` - Transaction status (pending, processed, submitted, error)
  - `label` - Text label for status
- **Scenarios**: UI-1.5, UI-5.5
- **Markup Pattern**:
```html
<div class="status-indicator">
  <span class="status-indicator__dot status-indicator__dot--{{status}}"></span>
  <span class="status-indicator__label">{{label}}</span>
</div>
```

### LoadingIndicator
- **Type**: Visual Indicator
- **Purpose**: Show processing status
- **Properties**:
  - `message` - Loading message text
- **Scenarios**: UI-2.9
- **Markup Pattern**:
```html
<div class="loading-indicator">
  <div class="loading-indicator__spinner"></div>
  <p class="loading-indicator__message">{{message}}</p>
</div>
```

### NotificationComponent
- **Type**: Alert
- **Purpose**: Show success/error messages
- **Properties**:
  - `type` - Type of notification (success, error, info, warning)
  - `message` - Notification text
  - `isDismissible` - Whether user can dismiss the notification
- **Scenarios**: UI-2.10, UI-4.5, UI-5.4, UI-6.3
- **Markup Pattern**:
```html
<div 
  id="notification-area"
  class="notification notification--{{type}}" 
  {{#if autoDismiss}}
    hx-swap-oob="true"
    hx-trigger="load delay:5s"
    hx-get="/api/clear-notification"
    hx-target="#notification-area"
  {{/if}}
>
  <div class="notification__icon"></div>
  <p class="notification__message">{{message}}</p>
  {{#if isDismissible}}
    <button 
      class="notification__close" 
      hx-get="/api/clear-notification"
      hx-target="#notification-area"
      hx-swap="outerHTML"
    >×</button>
  {{/if}}
</div>
```