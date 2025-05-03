# Task: Implement ImportButton Component with Tests

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: We have ImportButtonViewModel defined but need the UI component implementation
- **Requirements**: Create a button component that triggers transaction import and shows loading state during the operation

## Reference Information
- [Scalatags UI Development Guide](/ai-context/Guides/Libraries/scalatags-ui-development-guide.md)
- [Scalatags UI Testing Guide](/ai-context/workflows/scalatags-ui-testing-guide.md)
- [Existing view models in budget/ui/transaction_import/models/](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/)

## Specific Request
1. Implement the ImportButton component that:
   - Renders a button with configurable text (default: "Import Transactions")
   - Can be enabled or disabled based on the view model state
   - Includes HTMX attributes to trigger import operation when clicked
   - Shows a loading spinner during the import operation
   - Applies appropriate styling using TailwindCSS classes

2. Create comprehensive tests covering:
   - Component rendering in enabled state
   - Component rendering in disabled state
   - Correct HTMX attributes for triggering import and targeting results
   - Proper loading indicator implementation
   - Correct application of CSS classes based on enabled/disabled state

3. Run tests and fix any errors:
   - Execute `sbtn test` to run the test suite
   - Identify and fix any failures or compilation errors
   - Ensure all tests pass successfully
   - Address any code style issues identified during testing

## Output Format
1. Component implementation:
   ```scala
   // ImportButton.scala in appropriate package
   object ImportButton:
     def render(viewModel: ImportButtonViewModel): Frag = {
       // Implementation
     }
   ```

2. Test implementation:
   ```scala
   // ImportButtonSpec.scala in test package
   object ImportButtonSpec extends ZIOSpecDefault:
     def spec = suite("ImportButton")(
       // Test cases
     )
   ```

## Constraints
- Follow the view model-driven approach from our development guide
- Implement the component as pure functions with no side effects
- Use TailwindCSS utility classes for styling (no custom CSS)
- Include comprehensive test coverage for all scenarios
- Follow accessibility best practices (aria-disabled, focus states)
- Ensure loading state is clearly communicated to users
- The button should connect to the import endpoint using HTMX attributes