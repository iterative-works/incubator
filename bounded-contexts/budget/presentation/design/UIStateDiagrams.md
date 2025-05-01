# UI State Diagrams

This document outlines the state transitions and interactive behaviors of key UI components in the Budget feature. All state management happens on the server-side following our server-rendered Scalatags + HTMX architecture, with TailwindCSS providing the styling.

## HTMX-Driven State Management

All state transitions in our application are implemented through HTMX attributes that trigger server requests. When a state change occurs:

1. HTMX sends a request to the server with the relevant data
2. The server processes the request and determines the new state
3. The server renders a new HTML fragment using Scalatags with TailwindCSS classes
4. HTMX replaces the targeted DOM element with the new HTML

This ensures all UI state is managed by the server, making our application more robust and easier to reason about.

## Dashboard States

In our server-rendered architecture, the Dashboard has several logical states that the server manages and renders accordingly:

```plantuml
@startuml

state "Dashboard" as dashboard {
  state "Initial Page Load" as initial : Complete page rendered by server
  state "Empty" as empty : No transactions (rendered server-side)
  state "Populated" as populated : Transactions loaded (rendered server-side)
  state "Filtered" as filtered : Server-filtered transactions
  state "Selection Active" as selection : Selection state tracked on server
  state "Import In Progress" as importing : Import request being processed
}

[*] --> initial
initial --> empty : Server determines no transactions exist
initial --> populated : Server includes transactions in initial render
populated --> filtered : User filters (hx-get to /api/transactions/filter)
filtered --> populated : User clears filter (hx-get to /api/transactions)
populated --> selection : User selects transactions (hx-post to /api/transactions/select)
selection --> populated : User deselects all (hx-post to /api/transactions/select)
empty --> importing : User initiates import (hx-post to /api/import)
populated --> importing : User initiates import (hx-post to /api/import)
importing --> populated : Import completes (server returns updated HTML)
importing --> empty : Import fails or returns no transactions

@enduml
```

**Note on Loading States in Server-Rendered HTMX Architecture:**

In our HTMX architecture, "loading" isn't a distinct server-side state but rather is handled through:

1. **Initial Page Load**: The server renders the complete page initially
   
2. **HTMX Loading Indicators**: When subsequent requests are made, HTMX shows loading indicators using:
   ```html
   <div class="htmx-indicator">Loading...</div>
   ```
   
3. **Targeted Updates**: The server processes the request and returns only the HTML fragment needed to update the specific part of the page

This approach differs from SPA frameworks where loading states are explicitly managed in client-side state.

## Import Dialog States

In our server-rendered HTMX architecture, modals and dialogs are HTML fragments loaded from the server and injected into the DOM:

```plantuml
@startuml

state "Import Dialog" as import {
  state "Not Present in DOM" as notPresent : Dialog HTML not in page
  state "Present in DOM" as present : Dialog HTML inserted
  state "Server-Side Validation" as validating : Server validates form data
  state "Processing Import" as processing : Import in progress with indicator
}

[*] --> notPresent
notPresent --> present : User clicks Import button\n(hx-get to /api/import-dialog)
present --> validating : User submits form\n(hx-post to /api/import)
validating --> present : Validation fails\n(server returns form with errors)
validating --> processing : Valid form submitted\n(server starts import)
processing --> notPresent : Import completes\n(server returns updated transaction table)
present --> notPresent : User cancels\n(hx-get to /api/cancel-import)

@enduml
```

**Server-Side Rendering Approach:**

1. When the Import button is clicked, HTMX requests the dialog HTML from the server
2. The server renders the dialog using Scalatags and TailwindCSS
3. HTMX inserts the dialog HTML into the target container
4. Form submission is handled by HTMX, sending data to the server
5. All validation happens server-side
6. The server determines the appropriate response:
   - Form with error messages if validation fails
   - Updated transaction table HTML if import succeeds
   - Error notification if import fails
7. The dialog is removed from the DOM when the operation completes or is canceled

## Transaction Row States

```plantuml
@startuml

state "Transaction Row" as row {
  state "Normal" as normal : Default display
  state "Selected" as selected : Checkbox checked
  state "Hover" as hover : TailwindCSS hover state
  state "Editing Category" as editing : Category dropdown open
  state "Updating" as updating : Category update in progress (hx-indicator shown)
}

[*] --> normal
normal --> hover : Mouse over (CSS only, no server request)
hover --> normal : Mouse out (CSS only, no server request)
normal --> selected : User checks checkbox (hx-post to /api/transactions/select)
selected --> normal : User unchecks checkbox (hx-post to /api/transactions/select)
normal --> editing : User clicks category (hx-get to /api/transactions/{id}/edit-category)
editing --> updating : User selects new category (hx-post to /api/transactions/{id}/categorize)
updating --> normal : Update complete (Server returns updated HTML)

@enduml
```

Note: In our server-rendered architecture, most state changes involve:
1. HTMX sends request to the server
2. Server processes the request
3. Server renders a new HTML fragment with Scalatags + TailwindCSS
4. HTMX updates the DOM with the new HTML

Pure CSS states like hover are handled client-side through TailwindCSS hover: classes without server requests.

## Transaction Submission States

```plantuml
@startuml

state "Transaction Submission" as submission {
  state "No Selection" as noSelection : Submit button disabled
  state "Selection Valid" as validSelection : All selected have categories
  state "Selection Invalid" as invalidSelection : Some selected missing categories
  state "Submitting" as submitting : Submission in progress
  state "Submit Success" as submitSuccess : Transactions submitted
  state "Submit Error" as submitError : Submission failed
}

[*] --> noSelection
noSelection --> validSelection : User selects categorized transactions
noSelection --> invalidSelection : User selects uncategorized transactions
validSelection --> submitting : User clicks Submit button
invalidSelection --> submitting : User clicks Submit (should show validation)
submitting --> submitSuccess : Submission completes
submitting --> submitError : Submission fails
submitSuccess --> noSelection : Clear selection
submitError --> validSelection : Maintain selection

@enduml
```

## Notification States

```plantuml
@startuml

state "Notification Component" as notification {
  state "Hidden" as hidden : No notification
  state "Success" as success : Green success message
  state "Error" as error : Red error message
  state "Info" as info : Blue info message
  state "Warning" as warning : Orange warning message
}

[*] --> hidden
hidden --> success : Successful operation
hidden --> error : Failed operation
hidden --> info : Informational message
hidden --> warning : Warning message
success --> hidden : Auto-dismiss or user dismisses
error --> hidden : User dismisses
info --> hidden : Auto-dismiss or user dismisses
warning --> hidden : User dismisses

@enduml
```

## Transaction Filter and Sort States

```plantuml
@startuml

state "Transaction List" as list {
  state "Unfiltered" as unfiltered : Default sort (date desc)
  state "Filtered" as filtered : Text filter applied
  state "Date Sorted" as dateSorted : Sorted by date
  state "Amount Sorted" as amountSorted : Sorted by amount
  state "Description Sorted" as descSorted : Sorted by description
  state "Category Sorted" as catSorted : Sorted by category
  state "Status Sorted" as statusSorted : Sorted by status
}

[*] --> unfiltered
unfiltered --> filtered : User enters filter text
filtered --> unfiltered : User clears filter
unfiltered --> dateSorted : User clicks Date header
unfiltered --> amountSorted : User clicks Amount header
unfiltered --> descSorted : User clicks Description header
unfiltered --> catSorted : User clicks Category header
unfiltered --> statusSorted : User clicks Status header
filtered --> dateSorted : User clicks Date header (with filter)
filtered --> amountSorted : User clicks Amount header (with filter)
filtered --> descSorted : User clicks Description header (with filter)
filtered --> catSorted : User clicks Category header (with filter)
filtered --> statusSorted : User clicks Status header (with filter)

@enduml
```

## Combined User Flow

```plantuml
@startuml

title Budget Feature - User Flow

(*) --> "Dashboard\n(Initial Load)"
--> "View Statistics"

if "Need to import?" then
  -->[yes] "Click Import Button"
  --> "Open Import Dialog"
  --> "Enter Date Range"
  --> "Submit Import"
  --> "Wait for Import"
  if "Import Successful?" then
    -->[yes] "View Updated Transactions"
    --> "Dashboard\n(Populated)"
  else
    -->[no] "View Error Message"
    --> "Dashboard\n(Previous State)"
  endif
else
  -->[no] "Dashboard\n(Populated)"
endif

"Dashboard\n(Populated)" --> "Filter Transactions"
"Dashboard\n(Populated)" --> "Sort Transactions"
"Dashboard\n(Populated)" --> "Edit Categories"

"Edit Categories" --> "Select Category from Dropdown"
--> "View Updated Transaction"
--> "Dashboard\n(Populated)"

"Dashboard\n(Populated)" --> "Select Transactions"
--> "Click Submit to YNAB"
if "All Selected Have Categories?" then
  -->[yes] "Submit Transactions"
  if "Submission Successful?" then
    -->[yes] "View Success Message"
    --> "View Updated Status Indicators"
  else
    -->[no] "View Error Message"
  endif
else
  -->[no] "View Validation Error"
  --> "Dashboard\n(Selection Active)"
endif

"View Updated Status Indicators" --> "Dashboard\n(Populated)"

@enduml
```

## Server-Rendered Architecture Implementation

Our UI state diagrams represent logical state transitions, but the technical implementation leverages our server-rendered Scalatags + HTMX + TailwindCSS architecture.

### Technical Implementation

```plantuml
@startuml
title Server-Rendered State Transition

actor User
participant "Browser UI" as UI
participant HTMX
participant "Server (Http4s)" as Server
participant "Tapir Endpoint" as Endpoint
participant "Service" as Service
participant "View Renderer\n(Scalatags)" as Renderer

User -> UI: User interaction (click, select, etc.)
activate UI

UI -> HTMX: Triggers HTMX request
activate HTMX

HTMX -> Server: HTTP request (GET/POST)
activate Server

Server -> Endpoint: Route to endpoint
activate Endpoint

Endpoint -> Service: Call service method
activate Service

Service -> Service: Process request,\nupdate state
Service --> Endpoint: Return updated data
deactivate Service

Endpoint -> Renderer: Pass data to view renderer
activate Renderer

Renderer -> Renderer: Generate HTML with\nScalatags + TailwindCSS
Renderer --> Endpoint: Return HTML fragment
deactivate Renderer

Endpoint --> Server: Return HTML response
deactivate Endpoint

Server --> HTMX: HTTP response with HTML
deactivate Server

HTMX -> UI: Update targeted DOM element
deactivate HTMX

UI --> User: Display updated UI state
deactivate UI

@enduml
```

### Key Aspects of Our Architecture

1. **Server-Side State**: All application state is maintained on the server
2. **HTML Over the Wire**: State changes are communicated as HTML fragments, not JSON
3. **Targeted Updates**: HTMX only updates the specific DOM elements that need to change
4. **Progressive Enhancement**: The application works without JavaScript, but is enhanced with HTMX
5. **TailwindCSS Styling**: Visual presentation is handled by TailwindCSS utility classes
6. **Type-Safe Templates**: Scalatags provides type-safe HTML generation on the server

This architecture provides several benefits:
- Simpler client-side code with less JavaScript
- Consistent state management on the server
- More robust progressive enhancement
- Improved performance through smaller, targeted updates
- Type safety through Scala's type system