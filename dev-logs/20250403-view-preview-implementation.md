# View Preview Implementation - 2025-04-03

## Problem

We need to improve our UI development workflow by enabling developers to:
1. Quickly iterate on view components without starting the full application stack
2. Test UI components with various test data scenarios
3. Ensure proper asset loading (CSS, JavaScript) for accurate previews
4. Reduce the feedback loop for UI development

## Solution: Standalone View Preview Server

We've implemented a standalone preview server specifically for UI development:

1. **ViewPreviewMain** - A minimal application that:
   - Starts a lightweight HTTP server
   - Includes only the components needed for UI rendering
   - Uses the same asset pipeline for consistent styling
   - Provides routes for viewing UI components with test data

2. **TestDataProvider** - An in-memory data source that:
   - Supplies realistic test data for UI components
   - Provides different scenarios (empty state, with errors, etc.)
   - Eliminates dependency on database and services

3. **Example View Implementations** - Simplified UI components that:
   - Render different visual states and layouts
   - Use the same Scalatags/Tailwind structure as production code
   - Demonstrate responsive design and user interactions

## Implementation Details

### Key Components

1. **ViewPreviewMain.scala**
   - Minimal ZIO application with required layers
   - Uses the same Vite asset pipeline as the main app

2. **ViewPreviewModule.scala**
   - Routes for viewing different UI components
   - Navigation between view examples
   - Index of all available previews

3. **TestDataProvider.scala**
   - Supplies example data for all view components
   - Different scenarios for each component type
   - Support for edge cases and error states

4. **Example View Implementations**
   - SourceAccountViewExample - Different account listing views
   - TransactionViewExample - Transaction listing and detail views

5. **ExampleData.scala**
   - Container for sharing test data across components
   - Standardizes data formats for development

### Implementation Challenges & Solutions

1. **Model Structure Alignment**
   - Adjusted example data to match actual domain model structure
   - Made view components work with real model entities

2. **Asset Pipeline Integration**
   - Reused existing Vite configuration for consistent styling
   - Ensured CSS and JavaScript assets are properly loaded

3. **Scalatags Conflicts**
   - Resolved naming conflicts between Http4s and Scalatags
   - Used import aliases to disambiguate HTML tag names

4. **Isolated Environment**
   - Created a minimal AppEnv only for previewing UI components
   - Separated preview functionality from production code

### How to Use

1. Run the preview server:
   ```
   sbtn "runMain works.iterative.incubator.view.dev.ViewPreviewMain"
   ```

2. Open in browser:
   ```
   http://localhost:8080/preview
   ```

3. Browse available view examples and scenarios from the index page:
   - Source Accounts: default, empty, with-errors, form
   - Transactions: default, empty, with-pending, with-warnings

4. Navigate between different views and test various data scenarios

## Benefits

- **Development Speed**: Faster iteration on UI components
- **Focused Development**: Work on UI without database dependencies
- **Visual Testing**: Test different data scenarios and edge cases
- **Component Library**: Build toward a reusable UI component catalog

## Future Improvements

- Integration with Vite hot reloading for even faster feedback
- More comprehensive test data scenarios
- Documentation for component APIs alongside previews
- Ability to modify test data parameters through the UI