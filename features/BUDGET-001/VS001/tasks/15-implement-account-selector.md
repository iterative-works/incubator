# Task: Implement AccountSelector Component with Tests

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: We have AccountSelectorViewModel defined and the AccountSelector component implementation, but we need to verify it and add tests
- **Requirements**: Update and test the account selector component that allows users to select a bank account for transaction imports

## Implementation Guides
The implementation should follow these architectural guides:

- [Scalatags UI Development Guide](/ai-context/Guides/Libraries/scalatags-ui-development-guide.md)
- [Scalatags UI Testing Guide](/ai-context/workflows/scalatags-ui-testing-guide.md)
- [Existing DateRangeSelectorSpec as reference](/bounded-contexts/budget/src/test/scala/works/iterative/incubator/budget/ui/transaction_import/components/DateRangeSelectorSpec.scala)

It is essential to thoroughly read and understand these guides before starting the implementation. The guides contain the necessary information about how to structure the code, handle component design, manage styling, and follow our architectural principles.

## Specific Request
1. Verify the AccountSelector component implementation that:
   - Renders a dropdown with available accounts
   - Shows a default "Select an account" placeholder when no account is selected
   - Properly highlights the selected account in the dropdown
   - Shows validation errors when validation fails
   - Applies appropriate styling using TailwindCSS classes
   - Includes HTMX attributes for real-time validation

2. Create comprehensive tests covering:
   - Component rendering with no selected account
   - Component rendering with a selected account
   - Component rendering with validation errors
   - Proper display of account options from the view model
   - Correct application of CSS classes based on validation state
   - Proper structure of the generated HTML (select element with options)
   - HTMX attribute setup for interactive validation

3. Run tests and fix any errors:
   - Execute `sbtn budget/test` to run the test suite
   - Identify and fix any failures or compilation errors in the component
   - Ensure all tests pass successfully
   - Address any code style issues identified during testing

## Implementation Structure
Your implementation should follow the project's component structure:

```
bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/
  └── AccountSelector.scala (verify existing implementation)

bounded-contexts/budget/src/test/scala/works/iterative/incubator/budget/ui/transaction_import/components/
  └── AccountSelectorSpec.scala (create new test file)
```

## Test Implementation
Create test cases for the AccountSelector component that verify:
1. The correct rendering of the component with various view model states
2. The presence of all required UI elements (heading, dropdown, validation message)
3. The correct application of validation classes based on the view model state
4. Proper display of account options based on the view model
5. Proper handling of the selected account state
6. Error message display for validation failures

Example test structure:
```scala
object AccountSelectorSpec extends ZIOSpecDefault:
  def spec = suite("AccountSelector")(
    test("should render with no account selected") {
      // Test implementation
    },
    test("should render with an account selected") {
      // Test implementation
    },
    test("should render with validation error") {
      // Test implementation
    },
    test("should render all account options from the view model") {
      // Test implementation
    },
    test("should have proper HTMX attributes for validation") {
      // Test implementation
    }
  )
```

## Constraints
- Follow the view model-driven approach from our development guide
- Implement any fixes needed for the component as pure functions with no side effects
- Use TailwindCSS utility classes for styling (no custom CSS)
- Include comprehensive test coverage for all scenarios
- Follow accessibility best practices (proper labeling, ARIA attributes)
- Ensure error states are clearly communicated to users with appropriate colors and messaging