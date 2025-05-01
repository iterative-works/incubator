# Accessibility Considerations

This document outlines accessibility standards and implementation strategies for the Budget UI components, ensuring our application is usable by everyone, including people with disabilities.

## Core Accessibility Principles

1. **Perceivable**: Information and UI components must be presentable to users in ways they can perceive
2. **Operable**: UI components and navigation must be operable
3. **Understandable**: Information and operation of the UI must be understandable
4. **Robust**: Content must be robust enough to be interpreted by a variety of user agents, including assistive technologies

## WCAG 2.1 Compliance Targets

We aim to meet WCAG 2.1 Level AA compliance across all UI components.

## Component-Specific Accessibility Implementations

### DashboardView

- Use proper heading hierarchy (`h1` for page title, `h2` for sections, etc.)
- Provide a "skip to content" link for keyboard users
- Ensure logical tab order through the page
- Implement proper landmark regions (header, main, navigation)

```html
<header role="banner">
  <h1>Budget Dashboard</h1>
</header>
<main id="main-content" role="main">
  <div class="statistics-panel" aria-labelledby="stats-heading">
    <h2 id="stats-heading">Transaction Statistics</h2>
    <!-- Statistics content -->
  </div>
  <div class="transaction-section" aria-labelledby="transactions-heading">
    <h2 id="transactions-heading">Transactions</h2>
    <!-- Transaction table -->
  </div>
</main>
```

### StatisticsPanel

- Use appropriate color contrast for all text (minimum 4.5:1 ratio)
- Provide text alternatives for any non-text content
- Ensure information is not conveyed by color alone
- Add descriptive labels for screen readers

```html
<div class="statistics-card">
  <h3 id="total-trans-heading">Total Transactions</h3>
  <span 
    class="statistics-value" 
    aria-labelledby="total-trans-heading"
  >120</span>
</div>

<div class="statistics-card">
  <h3 id="categorized-heading">Categorized Transactions</h3>
  <span 
    class="statistics-value" 
    aria-labelledby="categorized-heading"
    aria-describedby="categorized-desc"
  >87</span>
  <span id="categorized-desc" class="statistics-percentage">72%</span>
</div>
```

### TransactionTable

- Use proper HTML table structure with appropriate headers
- Include column header scope attributes
- Add captions or descriptions
- Ensure sort indicators are accessible
- Provide keyboard support for all interactions
- Announce sort changes to screen readers

```html
<table class="transaction-table" aria-describedby="table-desc">
  <caption class="visually-hidden">Transactions with their date, description, amount, and category</caption>
  <thead>
    <tr>
      <th scope="col">
        <input 
          type="checkbox" 
          id="select-all" 
          aria-label="Select all transactions"
        />
      </th>
      <th 
        scope="col" 
        aria-sort="descending"
        role="columnheader"
        aria-label="Date, sorted descending"
      >
        Date 
        <span class="sort-indicator" aria-hidden="true">↓</span>
      </th>
      <th 
        scope="col" 
        role="columnheader"
      >Description</th>
      <!-- Other headers -->
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>
        <input 
          type="checkbox" 
          id="tx-1-select" 
          aria-label="Select transaction from Grocery Store"
        />
      </td>
      <td>May 12, 2023</td>
      <td>Grocery Store</td>
      <td>-$45.20</td>
      <td>Food</td>
    </tr>
    <!-- Other rows -->
  </tbody>
</table>
```

### ImportDialog

- Provide descriptive labels for all form fields
- Ensure error validation messages are associated with inputs
- Support keyboard navigation and control
- Trap focus within modal when open
- Return focus when modal closes
- Allow ESC key to close modal

```html
<div 
  role="dialog" 
  aria-labelledby="import-dialog-title" 
  aria-describedby="import-dialog-desc"
  aria-modal="true"
>
  <h2 id="import-dialog-title">Import Transactions</h2>
  <p id="import-dialog-desc">Select a date range to import transactions from Fio Bank.</p>
  
  <form>
    <div class="form-group">
      <label for="start-date" id="start-date-label">Start Date</label>
      <input 
        type="date" 
        id="start-date" 
        name="start-date" 
        aria-labelledby="start-date-label" 
        aria-required="true"
        required
      />
    </div>
    
    <!-- Similar pattern for end date -->
    
    <div class="button-group">
      <button 
        type="button" 
        class="button button--secondary"
        aria-label="Cancel import"
      >Cancel</button>
      <button 
        type="submit" 
        class="button button--primary"
        aria-label="Import transactions for selected date range"
      >Import</button>
    </div>
  </form>
</div>
```

### CategoryDropdown

- Ensure keyboard operability
- Provide clear focus indicators
- Use proper labeling
- Handle edge cases like no categories
- Support screen reader announcement of selection changes

```html
<label for="tx-1-category" id="tx-1-category-label">Category</label>
<select 
  id="tx-1-category" 
  name="category" 
  aria-labelledby="tx-1-category-label"
  aria-describedby="tx-1-category-help"
>
  <option value="">Select category</option>
  <option value="food">Food</option>
  <option value="transportation">Transportation</option>
  <!-- Other options -->
</select>
<span id="tx-1-category-help" class="visually-hidden">
  Select a category for the transaction from Grocery Store on May 12, 2023
</span>
```

### NotificationComponent

- Use appropriate ARIA roles for alerts
- Ensure timed notifications persist long enough to be read
- Provide keyboard access to dismissible notifications
- Use semantic colors with appropriate contrast

```html
<div 
  role="alert" 
  aria-live="assertive"
  aria-atomic="true" 
  class="notification notification--success"
>
  <div class="notification__icon" aria-hidden="true"></div>
  <p class="notification__message">4 transactions successfully submitted to YNAB</p>
  <button 
    class="notification__close" 
    aria-label="Dismiss notification"
  >×</button>
</div>
```

### LoadingIndicator

- Use appropriate ARIA attributes for live regions
- Provide clear text alternatives
- Ensure animation respects reduced motion preferences

```html
<div 
  class="loading-indicator" 
  role="status"
  aria-live="polite"
  aria-busy="true"
>
  <div 
    class="loading-indicator__spinner" 
    aria-hidden="true"
  ></div>
  <p class="loading-indicator__message">Importing transactions, please wait...</p>
</div>
```

### StatusIndicator

- Ensure status is not conveyed by color alone
- Provide text labels with color indicators
- Use ARIA attributes for complex status indicators

```html
<div class="status-indicator">
  <span 
    class="status-indicator__dot status-indicator__dot--submitted" 
    aria-hidden="true"
  ></span>
  <span class="status-indicator__label">Submitted to YNAB</span>
</div>
```

## Common Accessibility Patterns

### Focus Management with HTMX

Managing focus is particularly important in our HTMX-based application, since page sections are updated dynamically without full page loads.

```html
<button
  hx-get="/api/transactions/123/edit"
  hx-target="#transaction-123"
  hx-swap="outerHTML"
  hx-push-url="false"
  hx-trigger="click"
  hx-on::after-request="document.getElementById('transaction-123-category').focus()"
>
  Edit
</button>
```

### Progressive Enhancement

Our UI should work even without JavaScript, with HTMX enhancing the experience:

```html
<!-- Base form that works without JS -->
<form action="/api/transactions/filter" method="get">
  <input type="text" name="filter" placeholder="Filter transactions...">
  <button type="submit">Apply Filter</button>
</form>

<!-- HTMX enhancement for dynamic updates -->
<form 
  hx-get="/api/transactions/filter" 
  hx-target="#transaction-table-body"
  hx-trigger="input changed delay:500ms"
>
  <input type="text" name="filter" placeholder="Filter transactions...">
  <button type="submit" class="visually-hidden">Apply Filter</button>
</form>
```

### Keyboard Shortcuts

For power users, we'll implement keyboard shortcuts for common actions:

```html
<div 
  class="transaction-table-container"
  role="application"
  aria-describedby="keyboard-help"
>
  <!-- Table content -->
  
  <div id="keyboard-help" class="visually-hidden">
    Press Alt+I to open import dialog, Space to select/deselect a transaction, 
    Alt+S to submit selected transactions
  </div>
</div>
```

```javascript
document.addEventListener('keydown', function(e) {
  if (e.altKey && e.key === 'i') {
    // Trigger import dialog
    document.getElementById('import-button').click();
  } else if (e.altKey && e.key === 's') {
    // Trigger submit
    document.getElementById('submit-button').click();
  }
});
```

## Implementation Checklist

- [ ] Semantic HTML structure
- [ ] ARIA attributes for complex components
- [ ] Keyboard navigation support
- [ ] Focus management for interactive elements
- [ ] Color contrast compliance (minimum 4.5:1)
- [ ] Alternative text for non-text content
- [ ] Responsive design supporting zoom up to 200%
- [ ] Reduced motion support
- [ ] Error handling and feedback
- [ ] Form validation accessibility
- [ ] Screen reader testing
- [ ] Keyboard-only testing
- [ ] Color blindness simulation testing

## Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/TR/WCAG21/)
- [WAI-ARIA Best Practices](https://www.w3.org/TR/wai-aria-practices-1.1/)
- [Inclusive Components Library](https://inclusive-components.design/)
- [A11y Project Checklist](https://www.a11yproject.com/checklist/)