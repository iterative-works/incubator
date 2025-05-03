# Task 07: Component Preview Server Implementation

## Objective
Create a dedicated preview server for UI components that allows developers to view and interact with all components in their various states. This will enable efficient visual testing and validation of UI components across all their possible scenarios.

## Background
We need a way to visually verify our UI components in isolation across all their possible states. This will help ensure consistent rendering and behavior without having to navigate through the entire application flow.

## Requirements
- ✅ Implement a test HTTP server that displays UI components in all their possible states
- ✅ Create a sidebar navigation for browsing different components
- ✅ Structure component previews by categories (e.g., transaction import components)
- ✅ Support all component states through parameterized routes
- ✅ Focus initially on the DateRangeSelector component

## Task List

### 1. Create Preview Server Structure
- [x] Create a preview server module in `preview/src/main/scala/works/iterative/incubator/ui/preview`
- [x] Create `PreviewServer.scala` based on the existing `Main.scala` but simplified for testing
- [x] Add ZIO App entry point in `PreviewServerMain.scala`
- [x] Create `PreviewModuleRegistry.scala` to manage component preview modules

### 2. Implement Navigation Infrastructure
- [x] Create a `PreviewAppShell.scala` with a sidebar navigation component
- [x] Implement `SidebarNavigation.scala` component for browsing available previews
- [x] Create a base `PreviewModule` trait that component preview modules will extend

### 3. Create Core Preview Modules
- [x] Implement `HomePreviewModule.scala` as the landing page
- [x] Create `ComponentStatePreviewModule.scala` as a base for component state variations
- [x] Implement environment configuration for the preview server (host/port)

### 4. Implement DateRangeSelector Preview Module
- [x] Create `DateRangeSelectorPreviewModule.scala` for the first component
- [x] Define different states to preview (default, with validation errors, empty dates, etc.)
- [x] Create factory methods for generating different DateRangeSelectorViewModel instances
- [x] Map states to preview routes
- [x] Implement rendering for each state

### 5. Configure Preview Module Registry
- [x] Register the DateRangeSelectorPreviewModule in the PreviewModuleRegistry
- [x] Implement a mechanism to dynamically discover and load preview modules
- [x] Create a homepage that lists all available component categories

### 6. Add Preview Server Configuration
- [x] Create configuration for preview server port and other settings
- [x] Add logging for preview server operations
- [x] Configure the server to run on a different port (8090) from the main application

### 7. Test and Debug
- [x] Test the preview server locally
- [x] Verify all DateRangeSelector states render correctly
- [x] Fix any rendering or navigation issues
- [x] Ensure the sidebar navigation works correctly

### 8. Add Documentation
- [x] Document how to run the preview server
- [x] Document how to add new component previews
- [x] Add usage instructions in preview/README.md

### 9. Prepare for Future Component Previews
- [x] Create templates for adding new component preview modules
- [x] Set up the structure for other components from the transaction import flow
- [x] Ensure the system is extensible for future components

## Implementation Notes
- The preview server runs as a separate standalone application
- It uses HTTP 8090 port by default
- Each component state has its own route
- The navigation sidebar provides easy access to all components and states
- The server is for development and testing purposes only

## Acceptance Criteria
- ✅ The preview server runs without errors
- ✅ The DateRangeSelector component displays correctly in all its states
- ✅ Navigation between components works via the sidebar
- ✅ The implementation follows our architecture and coding standards
- ✅ The system is extensible for adding more component previews later

## How to Run the Preview Server
```
sbtn preview/reStart
```

The server will be available at http://localhost:8090/preview

## Possible future enhancements
- [ ] Implement development-friendly settings (hot reload, etc.)
- [ ] Add state transition testing capabilities
- [ ] Add interactive form submissions for components with user inputs
- [ ] Support dynamic state parameter changes via UI controls