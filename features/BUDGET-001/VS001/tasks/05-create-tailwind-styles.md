# Task: Create TailwindClasses Utility Class for Component Styling

## Context
- **Project background**: We're developing a budget management application with transaction import capabilities from Fio Bank
- **Technical context**: Using Scalatags for UI components, ZIO for effects, HTMX for interactions, and TailwindCSS for styling
- **Current implementation**: Components are using inline TailwindCSS classes, leading to inconsistency and duplication
- **Requirements**: Create a centralized TailwindStyles utility class to standardize styling across components

## Reference Information
- [Scalatags UI Development Guide](/ai-context/Guides/Libraries/scalatags-ui-development-guide.md)
- [Scalatags UI Testing Guide](/ai-context/workflows/scalatags-ui-testing-guide.md)
- [Existing web UI components](/web-ui/src/main/scala/works/iterative/incubator/components/)

## Specific Request
1. Implement the TailwindStyles utility object that:
   - Defines standard class combinations for common UI elements
   - Groups related styles into semantic categories (layout, typography, buttons, forms, etc.)
   - Provides component-specific style objects for specialized styling
   - Allows for composition of styles through string interpolation
   - Follows a consistent naming convention for all style variables

2. Create tests to verify:
   - Style string construction and composition
   - Consistency in class naming and ordering
   - Integration with component rendering
   - Boundary cases (empty styles, conditional styles)

3. Run tests and fix any errors:
   - Execute `sbtn test` to run the test suite
   - Identify and fix any failures or compilation errors
   - Ensure all tests pass successfully
   - Address any code style issues identified during testing

## Output Format
1. Utility class implementation:
   ```scala
   // TailwindStyles.scala in appropriate package
   object TailwindStyles:
     // Layout styles
     val container = "container mx-auto px-4 py-6"
     val card = "bg-white rounded-lg shadow-md p-6 mb-4"
     
     // Typography styles
     val heading1 = "text-2xl font-bold mb-4"
     val heading2 = "text-xl font-bold mb-3"
     
     // Button styles
     val buttonBase = "px-4 py-2 rounded-md font-medium focus:outline-none focus:ring-2 focus:ring-offset-2"
     val buttonPrimary = s"$buttonBase bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500"
     
     // Component-specific styles
     object ImportStyles:
       // Import-specific styles
     
     // Additional style categories and components as needed
   ```

2. Test implementation:
   ```scala
   // TailwindStylesSpec.scala in test package
   object TailwindStylesSpec extends ZIOSpecDefault:
     def spec = suite("TailwindStyles")(
       // Test cases
     )
   ```

## Constraints
- Follow the utility-first approach of TailwindCSS
- Use semantic naming for style variables (based on purpose, not appearance)
- Group related styles into logical categories
- Nest component-specific styles in subobjects
- Enable style composition through string interpolation
- Ensure styles are consistent with our overall design system
- Document style variables with meaningful comments
- Include styles for all UI components in our transaction import feature
- Prioritize reusability and maintainability