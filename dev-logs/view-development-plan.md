# View Development Plan

## Problem
When developing UI components, we need to:
1. Quickly iterate on view development without running the entire application
2. Test views with realistic data in various states
3. Ensure full asset support (HTML, CSS, JavaScript) for accurate previews

## Solution: Standalone View Preview Server

Create a minimal standalone server specifically for UI previews that:
- Serves only the view components we want to develop
- Uses the same asset pipeline (Vite) for consistent styling and JS
- Runs with in-memory test data instead of real database services
- Provides routes to view different data scenarios

## Implementation Plan

### 1. Create ViewPreviewMain

Create a standalone Main class for the preview server:

```
src/main/scala/works/iterative/incubator/view/dev/ViewPreviewMain.scala
```

This class will:
- Start a minimal HTTP server
- Include only the essential ZIO layers needed for views and assets
- Set up routes for viewing UI components with test data
- Serve assets through the existing Vite pipeline

### 2. Create Example View Components

Build separate example view implementations for testing:

```
src/main/scala/works/iterative/incubator/view/dev/examples/
```

With implementations for each component we want to preview:
- Simplified versions of module views
- Various test data scenarios
- Different visual states (empty, populated, error, etc.)

### 3. Create Test Data Provider

Create a service that provides in-memory test data:

```
src/main/scala/works/iterative/incubator/view/dev/TestDataProvider.scala
```

This will supply test data for:
- Source accounts in various states
- Transactions with different states
- Other domain objects needed by views

### 4. Create Preview Module

Create a dedicated module for view previews:

```
src/main/scala/works/iterative/incubator/view/dev/ViewPreviewModule.scala
```

This module will:
- Register all preview routes
- Link to the test data provider
- Organize views by component type
- Include index and navigation between views

## Technical Details

### Asset Handling

To ensure proper asset loading:
- Reuse the existing `ScalatagsViteSupport` layer
- Include the `AssetsModule` for proper JS/CSS serving
- Use the same `ScalatagsAppShell` for consistent layout
- Configure Vite paths properly in the preview server

### Minimal ZIO Environment

Create a simplified environment for the preview server:
- Include only layers needed for views and assets
- Replace database services with in-memory test data
- Eliminate authentication and other complex services

### Example Implementation

For the preview server:
1. Start with one UI component (e.g., SourceAccountModule)
2. Create test data generator for that component
3. Create preview routes for different view scenarios
4. Ensure assets are correctly loaded
5. Add more components incrementally

## Expected Benefits

- Faster UI development cycle
- Easier testing of edge cases and visual states
- No database or service dependencies
- Full asset support for realistic previews
- Starting point for component documentation

## Action Items

1. Create basic ViewPreviewMain with minimal dependencies
2. Set up test data provider for one component
3. Create routes for viewing that component
4. Verify asset loading works correctly
5. Extend to additional components
6. Document usage for other developers