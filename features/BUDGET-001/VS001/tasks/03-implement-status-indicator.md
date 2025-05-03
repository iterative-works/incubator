# Task: Implement StatusIndicator Component with Tests

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: We have StatusIndicatorViewModel defined but need the UI component implementation
- **Requirements**: Create a status indicator component that shows the current state of the import operation with appropriate styling

## Reference Information
- [Scalatags UI Development Guide](/ai-context/Guides/Libraries/scalatags-ui-development-guide.md)
- [Scalatags UI Testing Guide](/ai-context/workflows/scalatags-ui-testing-guide.md)
- [Existing view models in budget/ui/transaction_import/models/](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/)

## Specific Request
1. Implement the StatusIndicator component that:
   - Displays different statuses with appropriate styling (success, error, warning, info, loading)
   - Shows an appropriate icon for each status type
   - Includes animation for loading status
   - Applies appropriate color schemes using TailwindCSS classes
   - Maintains consistent layout across different status types

2. Create comprehensive tests covering:
   - Component rendering for each status type (success, error, warning, info, loading)
   - Correct application of CSS classes based on status
   - Proper icon selection for each status
   - Animation presence for loading status
   - Accessibility features (aria roles, etc.)

3. Run tests and fix any errors:
   - Execute `sbtn test` to run the test suite
   - Identify and fix any failures or compilation errors
   - Ensure all tests pass successfully
   - Address any code style issues identified during testing

## Output Format
1. Component implementation:
   ```scala
   // StatusIndicator.scala in appropriate package
   object StatusIndicator:
     def render(viewModel: StatusIndicatorViewModel): Frag = {
       // Implementation
     }
   ```

2. Test implementation:
   ```scala
   // StatusIndicatorSpec.scala in test package
   object StatusIndicatorSpec extends ZIOSpecDefault:
     def spec = suite("StatusIndicator")(
       // Test cases
     )
   ```

## Constraints
- Follow the view model-driven approach from our development guide
- Implement the component as pure functions with no side effects
- Use TailwindCSS utility classes for styling (no custom CSS)
- Include comprehensive test coverage for all status types
- Follow accessibility best practices (proper contrast, aria roles)
- Ensure status is clearly communicated both visually and semantically
- Support the specific status messages in the transaction import feature:
  - "Connecting to Fio Bank"
  - "Retrieving transactions"
  - "Storing transactions"
  - Error messages like "Unable to connect to Fio Bank. Please try again later."