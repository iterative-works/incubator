# Component Preview Server

The Component Preview Server provides a dedicated environment for viewing and testing UI components in isolation, with all their possible states and variations.

## Features

- View components in isolation across all their possible states
- Navigation sidebar for browsing different components
- Detailed information about component view models
- Fast visual verification during development

## Running the Preview Server

To run the preview server in development mode:

```
sbtn preview/reStart
```

The server will be available at http://localhost:8090/preview

## Available Environment Variables

- `PREVIEW_HOST`: The hostname for the preview server (default: `localhost`)
- `PREVIEW_PORT`: The port number for the preview server (default: `8090`)
- `BASEURI`: Base URI for the application (default: `/`)
- `VITE_BASE`: Base URL for Vite development server (default: `http://localhost:5173/`)
- `VITE_DISTPATH`: Path to Vite distribution files (default: `./target/vite`)

## Adding New Components to Preview

1. Create a new preview module class in `preview/src/main/scala/works/iterative/incubator/ui/preview/`
2. Define the component states you want to preview
3. Implement the required methods for rendering the component in different states
4. Register your new module in `PreviewModuleRegistry.scala`

## Component States

Each component can have multiple states for preview:

- Default state (normal operation)
- Error states (validation errors, etc.)
- Empty/null states
- Special states specific to the component

## Example

```scala
class MyComponentPreviewModule(val appShell: PreviewAppShell, val baseUri: BaseUri)
  extends ComponentStatePreviewModule[MyComponentViewModel]:

  override def basePath: List[String] = List("my-category", "my-component")
  
  val defaultState = ComponentState(
    name = "default",
    description = "Default state of the component",
    viewModel = MyComponentViewModel(/* default values */)
  )
  
  val errorState = ComponentState(
    name = "error",
    description = "Component with error",
    viewModel = MyComponentViewModel(error = Some("This is an error message"))
  )
  
  override def states: List[ComponentState[MyComponentViewModel]] = List(
    defaultState,
    errorState
  )
  
  // Implementations of renderStatesList and renderState
```