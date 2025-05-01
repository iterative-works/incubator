# Budget UI Design Documentation

This directory contains the design documentation and prototypes for the Budget feature UI components. These design artifacts serve as the foundation for implementing the UI layer in our Scalatags/HTMX/TailwindCSS architecture.

## Purpose

The Budget UI designs implement user interface components that support the Fio Bank to YNAB integration scenarios, focusing on transaction management, categorization, and submission to YNAB. The designs follow our UI Prototype Development Guide and adhere to the Functional MVP pattern.

## Design Artifacts

### Core Documentation

| Document | Purpose |
|----------|---------|
| [DesignSystem.md](./DesignSystem.md) | Defines colors, typography, spacing, and component styles |
| [TailwindConfig.js](./TailwindConfig.js) | TailwindCSS configuration that implements our design system |
| [TailwindStyles.scala](./TailwindStyles.scala) | Reusable TailwindCSS class combinations for our components |
| [TailwindComponentExamples.md](./TailwindComponentExamples.md) | Component examples with TailwindCSS implementation |
| [ComponentInventory.md](./ComponentInventory.md) | Catalogs all UI components with their properties and scenario coverage |
| [Wireframes.md](./Wireframes.md) | Visual representations of key screens and components |
| [ScenarioComponentMapping.md](./ScenarioComponentMapping.md) | Maps scenario steps to UI components |
| [UIStateDiagrams.md](./UIStateDiagrams.md) | State transitions for interactive components |
| [ResponsiveDesign.md](./ResponsiveDesign.md) | Adaptations for different device sizes |
| [Accessibility.md](./Accessibility.md) | Accessibility standards and implementations |
| [HTMXPatterns.md](./HTMXPatterns.md) | Interaction patterns using HTMX |

## Key Components

This design package includes the following UI components, all styled with TailwindCSS:

### Container Components

- **DashboardView**: Main application container hosting all dashboard content
- **StatisticsPanel**: Displays transaction summary statistics
- **TransactionTable**: Interactive table for viewing and manipulating transactions
- **ImportDialog**: Modal dialog for configuring and initiating transaction imports

### Interactive Components

- **ImportButton**: Triggers the import workflow
- **DatePicker**: Selects date ranges for imports
- **CategoryDropdown**: Selects transaction categories
- **SelectionControls**: Selects multiple transactions
- **SubmitButton**: Submits transactions to YNAB

### Indicator Components

- **StatusIndicator**: Displays transaction status
- **LoadingIndicator**: Shows processing status
- **NotificationComponent**: Displays success/error messages

### TailwindCSS Implementation

All components are implemented using TailwindCSS utility classes through:

1. **TailwindConfig.js**: Custom configuration that defines our design system tokens
2. **TailwindStyles.scala**: Centralized Scala object that defines reusable class combinations
3. **Component-Specific Style Objects**: Nested objects within TailwindStyles for component-specific patterns

This approach ensures:

- Consistent styling across all components
- Type-safe class references through the Scala object
- Easy theme customization by modifying the TailwindConfig
- Responsive design through TailwindCSS responsive utilities
- Accessibility through standardized focus and interactive states

## Scenario Coverage

These design artifacts fully cover the following scenarios:

1. **Dashboard displays transaction summary statistics**: StatisticsPanel component
2. **User can initiate a new transaction import with date range**: ImportButton, ImportDialog, DatePicker components
3. **Transaction list provides sorting and filtering**: TransactionTable component with sorting and filtering
4. **User can edit transaction category via dropdown**: CategoryDropdown component
5. **Bulk selection and submission of transactions**: SelectionControls, SubmitButton components
6. **Error messages are displayed for validation failures**: NotificationComponent with error states

## Design Decisions

1. **Progressive Enhancement with HTMX**: Core functionality works without JavaScript, with HTMX providing enhanced interactions
2. **Server-Driven UI**: All UI updates come from the server as HTML fragments
3. **Component-Based Architecture**: UI is composed of reusable, self-contained components
4. **Accessibility-First**: All components designed with accessibility in mind
5. **Responsive by Default**: Components adapt to different screen sizes using TailwindCSS utilities
6. **Utility-First CSS with TailwindCSS**: Using TailwindCSS for styling following a utility-first approach
7. **Centralized Style Patterns**: Common style combinations defined in a central Scala object
8. **Consistent Design System**: All components follow a unified design language codified in TailwindConfig

## Implementation Notes

This design package serves as the blueprint for implementing the Budget UI using:

- **Scalatags**: For server-side HTML generation
- **HTMX**: For dynamic client-side updates without custom JavaScript
- **TailwindCSS**: For utility-first styling with consistent design tokens
- **ZIO**: For handling server-side effects
- **Tapir**: For defining type-safe endpoints

### TailwindCSS Integration

The TailwindCSS integration follows these principles:

1. **Design System as Code**: Our design system tokens are defined in TailwindConfig.js
2. **Type-Safe Styling**: Style combinations are defined in TailwindStyles.scala as string constants
3. **Component-Specific Styles**: Each component has its own style object within TailwindStyles
4. **Responsive Design**: Using TailwindCSS responsive prefixes (sm:, md:, lg:, xl:)
5. **Accessibility**: Including focus states and proper contrast in our utility combinations

The next implementation phase will convert these designs into working Scala code following our Functional MVP pattern.

## Related Documentation

- [UI Prototype Development Guide](../../../ai-context/architecture/guides/ui_prototype_development_guide.md)
- [Functional Core Architecture Guide](../../../doc/architecture.md)
- [Development Workflow](../../../ai-context/workflows/development_workflow.md)