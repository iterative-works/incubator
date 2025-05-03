# Task: Implement DateRangeSelector Component with Tests

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: We have DateRangeSelectorViewModel defined but need the UI component implementation
- **Requirements**: Create a reusable date range selector component that validates date ranges and provides immediate feedback

## Reference Information
- [Scalatags UI Development Guide](/ai-context/Guides/Libraries/scalatags-ui-development-guide.md)
- [Scalatags UI Testing Guide](/ai-context/workflows/scalatags-ui-testing-guide.md)
- [Existing view models in budget/ui/transaction_import/models/](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/)

## Specific Request
1. Implement the DateRangeSelector component that:
   - Renders a form with start and end date inputs with appropriate labels
   - Validates that start date is before end date
   - Validates that date range doesn't exceed 90 days
   - Shows validation errors when validation fails
   - Applies appropriate styling using TailwindCSS classes
   - Includes HTMX attributes for real-time validation

2. Create comprehensive tests covering:
   - Component rendering with valid date ranges
   - Component rendering with invalid date ranges (start > end)
   - Component rendering with date ranges exceeding 90 days
   - Proper display of validation errors for each error case
   - Correct application of CSS classes based on validation state
   - HTMX attribute setup

3. Run tests and fix any errors:
   - Execute `sbtn test` to run the test suite
   - Identify and fix any failures or compilation errors
   - Ensure all tests pass successfully
   - Address any code style issues identified during testing

## Output Format
1. Component implementation:
   ```scala
   // DateRangeSelector.scala in appropriate package
   object DateRangeSelector:
     def render(viewModel: DateRangeSelectorViewModel): Frag = {
       // Implementation
     }
   ```

2. Test implementation:
   ```scala
   // DateRangeSelectorSpec.scala in test package
   object DateRangeSelectorSpec extends ZIOSpecDefault:
     def spec = suite("DateRangeSelector")(
       // Test cases
     )
   ```

## Constraints
- Follow the view model-driven approach from our development guide
- Implement the component as pure functions with no side effects
- Use TailwindCSS utility classes for styling (no custom CSS)
- Include comprehensive test coverage for all scenarios in the feature file
- Follow accessibility best practices (proper labeling, ARIA attributes)
- Ensure error states are clearly communicated to users with appropriate colors and messaging