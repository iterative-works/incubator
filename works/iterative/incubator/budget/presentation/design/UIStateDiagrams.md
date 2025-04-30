# UI State Diagrams

This document outlines the state transitions and interactive behaviors of key UI components in the Budget feature.

## Dashboard States

```plantuml
@startuml

state "Dashboard" as dashboard {
  state "Loading" as loading : Initial load state
  state "Empty" as empty : No transactions
  state "Populated" as populated : Transactions loaded
  state "Filtered" as filtered : Filtered transactions
  state "Selection Active" as selection : One or more rows selected
}

[*] --> loading
loading --> empty : No transactions
loading --> populated : Transactions found
populated --> filtered : User applies filter
filtered --> populated : User clears filter
populated --> selection : User selects transactions
selection --> populated : User deselects all
empty --> loading : User initiates import
populated --> loading : User initiates import

@enduml
```

## Import Dialog States

```plantuml
@startuml

state "Import Workflow" as import {
  state "Dialog Closed" as closed
  state "Dialog Open" as open
  state "Validating" as validating
  state "Import Processing" as processing
  state "Import Success" as success
  state "Import Error" as error
}

[*] --> closed
closed --> open : User clicks Import button
open --> validating : User submits form
validating --> open : Validation fails
validating --> processing : Valid form submitted
processing --> success : Import completes successfully
processing --> error : Import fails
success --> closed : Dialog closes automatically
error --> open : Error displayed, form preserved
open --> closed : User cancels

@enduml
```

## Transaction Row States

```plantuml
@startuml

state "Transaction Row" as row {
  state "Normal" as normal : Default display
  state "Selected" as selected : Checkbox checked
  state "Hover" as hover : Mouse over row
  state "Editing Category" as editing : Category dropdown open
  state "Updating" as updating : Category update in progress
}

[*] --> normal
normal --> hover : Mouse over
hover --> normal : Mouse out
normal --> selected : User checks checkbox
selected --> normal : User unchecks checkbox
normal --> editing : User clicks category
editing --> updating : User selects new category
updating --> normal : Update complete

@enduml
```

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