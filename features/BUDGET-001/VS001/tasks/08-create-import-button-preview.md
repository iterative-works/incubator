# Task: Create Preview Module for ImportButton Component

## Context
- **Project background**: We're developing a budget management application with component preview capabilities
- **Technical context**: Using Scala, Scalatags for UI components, ZIO for effects, and TailwindCSS for styling
- **Current implementation**: ImportButton component is implemented in `/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ImportButton.scala`
- **View Model**: ImportButton uses the ImportButtonViewModel defined in `/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/ImportButtonViewModel.scala`
- **Requirements**: Create a preview module to demonstrate the component in various states for development and testing purposes

## Reference Information
- [Component Preview Implementation Guide](/ai-context/architecture/guides/component_preview_guide.md)
- [Example: DateRangeSelectorPreviewModule](/preview/src/main/scala/works/iterative/incubator/ui/preview/DateRangeSelectorPreviewModule.scala)
- [Component Definition](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/ImportButton.scala)
- [View Model Definition](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/ImportButtonViewModel.scala)

## Specific Request
1. Create a new preview module named `ImportButtonPreviewModule` that:
   - Extends the `ComponentStatePreviewModule` trait
   - Uses the `ImportButtonViewModel` as its type parameter
   - Defines a logical base path for the component (e.g., `List("budget", "import-button")`)
   - Sets an appropriate title for display
   - Creates 3-5 different component states demonstrating all important variations
   - Implements all required methods correctly

2. Define key component states including:
   - Default state showing the normal component appearance
   - Disabled state when the button cannot be clicked
   - Loading state showing the spinner animation
   - Edge case state showing both disabled and loading
   - Any other important visual or functional variations

3. Register the preview module in the PreviewModuleRegistry:
   - Add the module instance to the registry
   - Create the appropriate adapter
   - Add the module to the list of modules to be registered

4. Verify the implementation:
   - Compile the code with `sbtn preview/compile`
   - Start the preview server with `sbtn preview/reStart`
   - Check the component preview in the browser at `http://localhost:8080/preview/budget/import-button`
   - Verify all states render correctly and navigation works as expected

## Output Format
1. Preview Module implementation:
   ```scala
   // ImportButtonPreviewModule.scala in preview/src/main/scala/works/iterative/incubator/ui/preview/
   package works.iterative.incubator.ui.preview
   
   // imports...
   
   class ImportButtonPreviewModule(
       val appShell: PreviewAppShell,
       val baseUri: BaseUri
   ) extends ComponentStatePreviewModule[ImportButtonViewModel]:
       // Implementation
   end ImportButtonPreviewModule
   ```

2. PreviewModuleRegistry updates:
   ```scala
   // PreviewModuleRegistry.scala additions
   // Add to private val declarations
   private val importButtonPreviewModule = ImportButtonPreviewModule(appShell, baseUri)
   
   // Add adapter
   private val importButtonWebModule =
       TapirWebModuleAdapter.adapt[PreviewEnv](
           options = Http4sServerOptions.default,
           module = importButtonPreviewModule
       )
       
   // Add to modules list
   val modules: List[WebFeatureModule[RIO[PreviewEnv, *]]] = List(
       // existing modules...
       importButtonWebModule
   )
   ```

## Constraints
- Follow the established pattern from the DateRangeSelectorPreviewModule example
- Ensure each state has a unique name, clear description, and appropriate view model data
- Include view model property display for debugging in the state detail view
- Make state descriptions clear and informative for other developers
- Use TailwindCSS utility classes consistent with the rest of the preview UI
- Ensure consistent navigation and user experience across preview pages
- Create states that demonstrate all important component behaviors and edge cases

## Best Practices
- Use realistic data in preview states rather than placeholder values
- Include states that demonstrate common error conditions and validation scenarios
- Show calculated properties from the view model in the detailed state view
- Group related UI components logically through the base path structure
- Provide clear navigation between states for easy testing
- Include sufficient explanatory text about the component's purpose and behavior