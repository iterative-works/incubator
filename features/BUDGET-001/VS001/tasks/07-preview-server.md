# Task 07: Component Preview Server Implementation

## Objective
Create a dedicated preview server for UI components that allows developers to view and interact with all components in their various states. This will enable efficient visual testing and validation of UI components across all their possible scenarios.

## Background
We need a way to visually verify our UI components in isolation across all their possible states. This will help ensure consistent rendering and behavior without having to navigate through the entire application flow.

## Requirements
- Implement a test HTTP server that displays UI components in all their possible states
- Create a sidebar navigation for browsing different components
- Structure component previews by categories (e.g., transaction import components)
- Support all component states through parameterized routes
- Focus initially on the DateRangeSelector component

## Task List

### 1. Create Preview Server Structure
- [ ] Create a test server module in `src/test/scala/works/iterative/incubator/ui/preview`
- [ ] Create `PreviewServer.scala` based on the existing `Main.scala` but simplified for testing
- [ ] Add ZIO App entry point in `PreviewServerMain.scala`
- [ ] Create `PreviewModuleRegistry.scala` to manage component preview modules

### 2. Implement Navigation Infrastructure
- [ ] Create a `PreviewAppShell.scala` with a sidebar navigation component
- [ ] Implement `SidebarNavigation.scala` component for browsing available previews
- [ ] Create a base `PreviewModule` trait that component preview modules will extend

### 3. Create Core Preview Modules
- [ ] Implement `ComponentCategoryModule.scala` to group components by category
- [ ] Create `ComponentStatePreviewModule.scala` as a base for component state variations
- [ ] Create `PreviewRoute.scala` utility for defining preview routes

### 4. Implement DateRangeSelector Preview Module
- [ ] Create `DateRangeSelectorPreviewModule.scala` for the first component
- [ ] Define different states to preview (default, with validation errors, empty dates, etc.)
- [ ] Create factory methods for generating different DateRangeSelectorViewModel instances
- [ ] Map states to preview routes
- [ ] Implement rendering for each state

### 5. Configure Preview Module Registry
- [ ] Register the DateRangeSelectorPreviewModule in the PreviewModuleRegistry
- [ ] Implement a mechanism to dynamically discover and load preview modules
- [ ] Create a homepage that lists all available component categories

### 6. Add Test Server Configuration
- [ ] Create configuration for preview server port and other settings
- [ ] Add logging for preview server operations

### 7. Test and Debug
- [ ] Test the preview server locally
- [ ] Verify all DateRangeSelector states render correctly
- [ ] Fix any rendering or navigation issues
- [ ] Ensure the sidebar navigation works correctly

### 8. Add Documentation
- [ ] Document how to run the preview server
- [ ] Document how to add new component previews
- [ ] Add usage instructions to the project README

### 9. Prepare for Future Component Previews
- [ ] Create templates for adding new component preview modules
- [ ] Set up the structure for other components from the transaction import flow
- [ ] Ensure the system is extensible for future components

## Implementation Notes
- The preview server will be a simplified version of our production server
- We'll leverage the existing tapir endpoints pattern for consistency
- Each component state will have its own route
- We'll focus on isolated component testing, not integration between components
- The server is for development and testing purposes only

## Acceptance Criteria
- The preview server runs without errors
- The DateRangeSelector component displays correctly in all its states
- Navigation between components works via the sidebar
- The implementation follows our architecture and coding standards
- The system is extensible for adding more component previews later

## Possible future enhancements
- [ ] Implement development-friendly settings (hot reload, etc.)
