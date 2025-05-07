# Task: Create Preview Module for AccountSelector Component

## Context
- **Project background**: We're developing a budget management application with component preview capabilities
- **Technical context**: Using Scala, Scalatags for UI components, ZIO for effects, and TailwindCSS for styling
- **Current implementation**: AccountSelector component is implemented in `/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/AccountSelector.scala`
- **View Model**: AccountSelector uses the AccountSelectorViewModel defined in `/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/AccountSelectorViewModel.scala`
- **Requirements**: Create a preview module to demonstrate the component in various states for development and testing purposes

## Reference Information
- [Component Preview Implementation Guide](/ai-context/architecture/guides/component_preview_guide.md)
- [Example: DateRangeSelectorPreviewModule](/preview/src/main/scala/works/iterative/incubator/ui/preview/DateRangeSelectorPreviewModule.scala)
- [Component Definition](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/components/AccountSelector.scala)
- [View Model Definition](/bounded-contexts/budget/src/main/scala/works/iterative/incubator/budget/ui/transaction_import/models/AccountSelectorViewModel.scala)

## Specific Request
1. Create a new preview module named `AccountSelectorPreviewModule` that:
   - Extends the `ComponentStatePreviewModule` trait
   - Uses the `AccountSelectorViewModel` as its type parameter
   - Defines a logical base path for the component (e.g., `List("transaction-import", "account-selector")`)
   - Sets an appropriate title for display
   - Creates 3-5 different component states demonstrating all important variations
   - Implements all required methods correctly

2. Define key component states including:
   - Default state showing the normal component appearance with multiple accounts
   - Empty state with no accounts available
   - Selected state showing a selected account
   - Error state showing validation error handling
   - Disabled state showing the component when disabled

3. Register the preview module in the PreviewModuleRegistry:
   - Add the module instance to the registry
   - Create the appropriate adapter
   - Add the module to the list of modules to be registered

4. Add the component to the SidebarNavigation:
   - Add it to the "Transaction Import" category in SidebarNavigation.scala
   - Ensure it has a descriptive name and the correct path

5. Add the component to the HomePreviewModule:
   - Add it to the "Available Components" list in HomePreviewModule.scala
   - Include a brief description of its purpose

6. Verify the implementation:
   - Compile the code with `sbtn preview/compile`
   - Start the preview server with `sbtn preview/reStart`
   - Check the component preview in the browser at `http://localhost:8090/preview/transaction-import/account-selector`
   - Verify all states render correctly and navigation works as expected
   - Confirm component appears in both sidebar navigation and home page

## Output Format
1. Preview Module implementation:
   ```scala
   // AccountSelectorPreviewModule.scala in preview/src/main/scala/works/iterative/incubator/ui/preview/
   package works.iterative.incubator.ui.preview

   // imports...

   class AccountSelectorPreviewModule(
       val appShell: PreviewAppShell,
       val baseUri: BaseUri
   ) extends ComponentStatePreviewModule[AccountSelectorViewModel]:
       // Implementation
   end AccountSelectorPreviewModule
   ```

2. PreviewModuleRegistry, SidebarNavigation, and HomePreviewModule updates:
   ```scala
   // PreviewModuleRegistry.scala additions
   // Add to private val declarations
   private val accountSelectorPreviewModule = AccountSelectorPreviewModule(appShell, baseUri)

   // Add adapter
   private val accountSelectorWebModule =
       TapirWebModuleAdapter.adapt[PreviewEnv](
           options = Http4sServerOptions.default,
           module = accountSelectorPreviewModule
       )

   // Add to modules list
   val modules: List[WebFeatureModule[RIO[PreviewEnv, *]]] = List(
       // existing modules...
       accountSelectorWebModule
   )

   // SidebarNavigation.scala - Add to Transaction Import category
   NavCategory("Transaction Import", List(
       NavItem("Account Selector", "/preview/transaction-import/account-selector"),
       NavItem("Date Range Selector", "/preview/transaction-import/date-range-selector"),
       NavItem("Import Button", "/preview/transaction-import/import-button"),
       NavItem("Status Indicator", "/preview/transaction-import/status-indicator")
       // Add more components as they're implemented
   ))

   // HomePreviewModule.scala - Add to Available Components list
   li(a(
       href := "/preview/transaction-import/account-selector",
       cls := "text-blue-600 hover:underline",
       "AccountSelector - Allows users to select an account for transaction import"
   ))
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
