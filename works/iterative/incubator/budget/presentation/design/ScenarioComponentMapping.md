# Scenario to Component Mapping

This document maps each scenario step to specific UI components and interactions, ensuring complete coverage of all requirements.

## Scenario: Dashboard displays transaction summary statistics

| Scenario Step | UI Component | Interaction |
|---------------|-------------|-------------|
| Given the user is logged in | N/A (Auth handled separately) | N/A |
| When the user navigates to the dashboard | DashboardView | URL navigation to /dashboard |
| Then the dashboard displays summary statistics | StatisticsPanel | Displays transaction counts and amounts |
| And the dashboard contains an Import button | ImportButton | Button rendered on the dashboard |
| And the dashboard displays a transaction table with status indicators | TransactionTable, StatusIndicator | Table with status column showing indicators |

## Scenario: User can initiate a new transaction import with date range

| Scenario Step | UI Component | Interaction |
|---------------|-------------|-------------|
| Given the user is on the dashboard screen | DashboardView | URL navigation to /dashboard |
| When the user clicks the Import button | ImportButton | User clicks button |
| Then an import dialog appears | ImportDialog | Dialog appears with modal overlay |
| And the dialog contains start date and end date fields | DatePicker (2x) | Two date pickers rendered in dialog |
| And the dialog contains an Import button | ImportButton (in dialog) | Button rendered within dialog |
| When the user selects a date range | DatePicker (2x) | User selects dates |
| And clicks the import dialog Import button | ImportButton (in dialog) | User clicks button |
| Then the system delegates to the import service | N/A (Backend) | HTMX POST to /api/import |
| And the user sees a loading indicator | LoadingIndicator | Loading spinner appears |
| And the user is notified when the import is complete | NotificationComponent | Success notification appears |
| And the transaction table is updated with the new transactions | TransactionTable | Table refreshes with new data |

## Scenario: Transaction list provides sorting and filtering

| Scenario Step | UI Component | Interaction |
|---------------|-------------|-------------|
| Given the user is on the dashboard screen | DashboardView | URL navigation to /dashboard |
| And the transaction table contains transactions | TransactionTable | Table populated with data |
| When the user clicks on a column header | TransactionTable header | User clicks column header |
| Then the transactions are sorted by that column | TransactionTable | Table refreshes with sorted data |
| When the user enters text in the filter field | TransactionTable filter | User types in filter input |
| Then the transactions are filtered by that text | TransactionTable | Table refreshes with filtered data |

## Scenario: User can edit transaction category via dropdown

| Scenario Step | UI Component | Interaction |
|---------------|-------------|-------------|
| Given the user is on the dashboard screen | DashboardView | URL navigation to /dashboard |
| And the transaction table contains transactions | TransactionTable | Table populated with data |
| When the user clicks on a transaction's category cell | TransactionTable category cell | User clicks on category cell |
| Then a dropdown with available categories appears | CategoryDropdown | Dropdown appears in place of text |
| When the user selects a different category | CategoryDropdown | User selects option from dropdown |
| Then the transaction's category is updated | TransactionTable | Category cell updates with new value |
| And the user sees a confirmation message | NotificationComponent | Success notification appears |

## Scenario: Bulk selection and submission of transactions

| Scenario Step | UI Component | Interaction |
|---------------|-------------|-------------|
| Given the user is on the dashboard screen | DashboardView | URL navigation to /dashboard |
| And the transaction table contains categorized transactions | TransactionTable | Table populated with categorized data |
| When the user selects multiple transactions | SelectionControls | User checks multiple checkboxes |
| And clicks the Submit to YNAB button | SubmitButton | User clicks submit button |
| Then the selected transactions are submitted to YNAB | N/A (Backend) | HTMX POST to /api/transactions/submit |
| And the user sees a success message | NotificationComponent | Success notification appears |
| And the transaction status indicators are updated | StatusIndicator | Status indicators change to "Submitted" |

## Scenario: Error messages are displayed for validation failures

| Scenario Step | UI Component | Interaction |
|---------------|-------------|-------------|
| Given the user is on the dashboard screen | DashboardView | URL navigation to /dashboard |
| And the transaction table contains some uncategorized transactions | TransactionTable | Table shows some transactions without categories |
| When the user selects those transactions | SelectionControls | User checks checkboxes for uncategorized transactions |
| And clicks the Submit to YNAB button | SubmitButton | User clicks submit button |
| Then the user sees an error message | NotificationComponent | Error notification appears |
| And the transactions remain unsubmitted | StatusIndicator | Status indicators remain unchanged |

## Cross-Cutting Interaction Patterns

### HTMX Interactions

| Component | HTMX Pattern | Endpoint | Target | Swap Strategy |
|-----------|--------------|----------|--------|---------------|
| ImportButton | hx-get | /api/import-dialog | #dialog-container | innerHTML |
| ImportDialog Form | hx-post | /api/import | #transaction-table-container | outerHTML |
| TransactionTable Headers | hx-get | /api/transactions/sort | #transaction-table-body | innerHTML |
| TransactionTable Filter | hx-get | /api/transactions/filter | #transaction-table-body | innerHTML |
| CategoryDropdown | hx-post | /api/transactions/{id}/categorize | #transaction-row-{id} | outerHTML |
| SelectionControls | hx-post | /api/transactions/toggle-selection | #submit-button | outerHTML |
| SubmitButton | hx-post | /api/transactions/submit | #notification-area | innerHTML |
| NotificationComponent | hx-get | /api/clear-notification | #notification-area | outerHTML |

### Response Types

| Endpoint | Response Type | Purpose |
|----------|--------------|---------|
| /api/import-dialog | HTML | Return rendered import dialog |
| /api/import | HTML | Return updated transaction table |
| /api/transactions/sort | HTML | Return sorted transaction rows |
| /api/transactions/filter | HTML | Return filtered transaction rows |
| /api/transactions/{id}/categorize | HTML | Return updated transaction row |
| /api/transactions/toggle-selection | HTML | Return updated submit button |
| /api/transactions/submit | HTML | Return notification component |
| /api/clear-notification | HTML | Return empty notification area |

### State Management

All state is managed server-side with client view state reflected through HTML responses. The key state elements include:

1. **Transaction List State**:
   - Current sort field and direction
   - Current filter text
   - Selected transaction IDs
   
2. **Import Dialog State**:
   - Visibility
   - Start/end dates
   - Validation state
   
3. **Notification State**:
   - Type (success/error/info)
   - Message
   - Auto-dismiss flag
   
4. **Transaction State**:
   - Current data
   - Category selections
   - Status indicators

Each HTMX interaction preserves and updates these state elements through server-side processing and HTML responses.