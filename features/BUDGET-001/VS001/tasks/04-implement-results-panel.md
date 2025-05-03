# Task: Implement ResultsPanel Component with Tests

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: We have ImportResults model defined but need a UI component to display import results
- **Requirements**: Create a results panel component that displays the outcome of transaction import operations with appropriate styling and details

## Reference Information
- [Scalatags UI Development Guide](/ai-context/Guides/Libraries/scalatags-ui-development-guide.md)
- [Scalatags UI Testing Guide](/ai-context/workflows/scalatags-ui-testing-guide.md)
- [Existing view models in budget/ui/transaction_import/models/](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/)

## Specific Request
1. Implement the ResultsPanel component that:
   - Displays a summary of import results (transaction count, success/failure status)
   - Shows error messages when applicable
   - Provides details about transaction processing (new transactions, duplicates skipped)
   - Uses appropriate colors for success and error states
   - Includes a "View Transactions" button to navigate to the transaction list
   - Optionally includes a "Retry" button for failed imports

2. Create comprehensive tests covering:
   - Component rendering for successful imports with transaction counts
   - Component rendering for imports with no transactions
   - Component rendering for failed imports with error messages
   - Component rendering for imports with duplicate transactions
   - Correct application of CSS classes based on success/error states
   - Button rendering and HTMX attributes

3. Run tests and fix any errors:
   - Execute `sbtn test` to run the test suite
   - Identify and fix any failures or compilation errors
   - Ensure all tests pass successfully
   - Address any code style issues identified during testing

## Output Format
1. Component implementation:
   ```scala
   // ResultsPanel.scala in appropriate package
   object ResultsPanel:
     def render(results: ImportResults): Frag = {
       // Implementation
     }
   ```

2. Test implementation:
   ```scala
   // ResultsPanelSpec.scala in test package
   object ResultsPanelSpec extends ZIOSpecDefault:
     def spec = suite("ResultsPanel")(
       // Test cases
     )
   ```

## Constraints
- Follow the view model-driven approach from our development guide
- Implement the component as pure functions with no side effects
- Use TailwindCSS utility classes for styling (no custom CSS)
- Include comprehensive test coverage for all result scenarios
- Follow accessibility best practices
- Support the specific result messages from the feature file:
  - "15 transactions successfully imported"
  - "No transactions found for the selected date range"
  - "5 new transactions imported, 10 duplicates skipped"
  - Error messages like "Unable to connect to Fio Bank. Please try again later."