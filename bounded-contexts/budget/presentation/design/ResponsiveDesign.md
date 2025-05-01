# Responsive Design Considerations with TailwindCSS

This document outlines the responsive design strategy for the Budget UI components using TailwindCSS, ensuring they work across different device sizes.

## TailwindCSS Responsive Breakpoints

We'll use the following breakpoints as defined in our TailwindConfig.js:

- **Default**: Mobile first (< 576px)
- **sm**: 576px and up
- **md**: 768px and up
- **lg**: 992px and up
- **xl**: 1200px and up

These breakpoints are accessed in Tailwind using responsive prefixes:

```html
<div class="w-full md:w-1/2 lg:w-1/4">
  <!-- Full width on mobile, half width on medium screens, quarter width on large screens -->
</div>
```

## Core Responsive Principles with TailwindCSS

1. **Mobile-First Approach**: Tailwind is mobile-first by default; we start with base styles and add responsive variants with prefixes
   
2. **Utility-First Classes**: Use Tailwind's utility classes for responsive design instead of custom media queries:
   ```html
   <!-- Instead of custom CSS media queries -->
   <div class="flex flex-col sm:flex-row">
     <!-- Elements stack vertically on mobile, horizontally on sm and up -->
   </div>
   ```

3. **Responsive Variants**: Apply different styles at different breakpoints using responsive prefixes:
   ```html
   <div class="hidden md:block">
     <!-- Hidden on mobile, visible from medium screens up -->
   </div>
   ```

4. **Fluid Container**: Use Tailwind's container and max-width utilities:
   ```html
   <div class="container mx-auto px-4">
     <!-- Centered container with padding -->
   </div>
   ```

5. **Responsive Grid**: Use Tailwind's grid system with responsive columns:
   ```html
   <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
     <!-- 1 column on mobile, 2 on small screens, 4 on large screens -->
   </div>
   ```

6. **Touch-Friendly Targets**: Use sizing utilities to ensure large enough tap targets:
   ```html
   <button class="h-12 w-12 p-3">
     <!-- 48px height and width with padding -->
   </button>
   ```

## Component-Specific Adaptations

### DashboardView

**Mobile (< 576px)**:
- Full-width layout with stacked components
- Statistics cards arranged in a 1x4 vertical grid
- Transaction table uses horizontal scrolling for all columns

**Small (576px - 767px)**:
- Statistics cards arranged in a 2x2 grid
- Transaction table still scrolls horizontally

**Medium (768px - 991px)**:
- Statistics cards arranged in a 2x2 grid
- Transaction table shows core columns (date, description, amount) without scrolling
- Optional columns require horizontal scrolling

**Large (992px+)**:
- Statistics cards arranged in a 4x1 horizontal grid
- Transaction table shows all columns without scrolling

### StatisticsPanel

**Mobile (< 576px)**:
- Cards stacked vertically
- Full width cards
- Simplified content showing only key figures

**Small+ (576px+)**:
- Cards in grid layout
- Progressive addition of detail information

### TransactionTable

**Mobile (< 576px)**:
- Core columns only (date, description, amount)
- Collapsed view with expandable rows for additional details
- Full-width filter input
- Sort options in a dropdown rather than column headers

**Small (576px - 767px)**:
- Add category column
- Keep expandable rows for status and actions
- Column header sorting

**Medium+ (768px+)**:
- Show all columns
- Action buttons in toolbar

### ImportDialog

**Mobile (< 576px)**:
- Full screen modal
- Stacked form controls
- Fixed position actions at bottom

**Small+ (576px+)**:
- Centered dialog with max-width
- Side-by-side date controls
- Actions in dialog footer

### CategoryDropdown

**Mobile (< 576px)**:
- Full-screen overlay for category selection
- Larger touch targets

**Small+ (576px+)**:
- In-place dropdown
- Standard select sizes

## TailwindCSS Implementation Patterns

### 1. Responsive Container

Using Tailwind's container and responsive padding:

```html
<div class="container mx-auto px-4">
  <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
    <div class="bg-white rounded shadow p-4">
      <!-- Statistics Card 1 -->
    </div>
    <div class="bg-white rounded shadow p-4">
      <!-- Statistics Card 2 -->
    </div>
    <!-- Additional cards -->
  </div>
</div>
```

### 2. Responsive Layout with Flexbox

Using Tailwind's responsive flex utilities:

```html
<!-- Mobile: vertical, Desktop: horizontal -->
<div class="flex flex-col md:flex-row md:justify-between md:items-center">
  <h2 class="text-xl font-bold mb-4 md:mb-0">Transactions</h2>
  <div class="flex space-x-2">
    <button class="bg-blue-600 text-white px-4 py-2 rounded">Import</button>
    <button class="bg-green-600 text-white px-4 py-2 rounded">Submit</button>
  </div>
</div>
```

### 3. Responsive Tables

Using Tailwind for responsive tables:

```html
<div class="overflow-x-auto lg:overflow-visible">
  <table class="min-w-full">
    <thead class="bg-gray-100">
      <tr>
        <th class="px-4 py-2 text-left">Date</th>
        <!-- Only show description on medium screens and up -->
        <th class="hidden md:table-cell px-4 py-2 text-left">Description</th>
        <th class="px-4 py-2 text-left">Amount</th>
        <!-- Only show category and status on large screens and up -->
        <th class="hidden lg:table-cell px-4 py-2 text-left">Category</th>
        <th class="hidden lg:table-cell px-4 py-2 text-left">Status</th>
      </tr>
    </thead>
    <tbody>
      <!-- Table rows -->
    </tbody>
  </table>
</div>
```

### 4. Conditional Visibility

Using Tailwind's responsive display utilities:

```html
<div class="bg-white rounded shadow p-4">
  <h3 class="text-sm text-gray-500">Total Transactions</h3>
  <p class="text-2xl font-bold">120</p>
  <!-- Extra details only visible on medium screens and up -->
  <p class="hidden md:block text-sm text-gray-400 mt-2">
    Last updated today at 2:30 PM
  </p>
</div>
```

### 5. Touch-Friendly Controls

Using Tailwind's sizing and padding utilities:

```html
<!-- Regular button with larger touch target on mobile -->
<button class="bg-blue-600 text-white rounded h-12 px-6 sm:h-10 sm:px-4">
  Submit
</button>

<!-- Checkbox with larger touch area -->
<label class="flex items-center cursor-pointer p-2">
  <input type="checkbox" class="h-6 w-6 sm:h-4 sm:w-4 rounded border-gray-300 text-blue-600" />
  <span class="ml-2">Select</span>
</label>
```

### 6. Using TailwindStyles Constants in Scala

```scala
import works.iterative.incubator.budget.presentation.design.TailwindStyles

def renderStatisticsPanel(stats: StatisticsViewModel): Frag =
  div(cls := TailwindStyles.StatisticsPanel.container)(
    // Four cards with responsive layout handled by the container class
    renderStatisticsCard("Total Transactions", stats.totalTransactions.toString),
    renderStatisticsCard("Categorized", stats.categorizedTransactions.toString, Some(s"${stats.categorizedPercentage}%")),
    renderStatisticsCard("Uncategorized", stats.uncategorizedTransactions.toString, Some(s"${stats.uncategorizedPercentage}%")),
    renderStatisticsCard("Submitted", stats.submittedTransactions.toString, Some(s"${stats.submittedPercentage}%"))
  )

def renderStatisticsCard(label: String, value: String, percentage: Option[String] = None): Frag =
  div(cls := TailwindStyles.StatisticsPanel.card)(
    span(cls := TailwindStyles.StatisticsPanel.label)(label),
    span(cls := TailwindStyles.StatisticsPanel.value)(value),
    percentage.map(p => span(cls := TailwindStyles.StatisticsPanel.percentage)(s"($p)"))
  )
```

## Mobile-Specific Interaction Patterns with TailwindCSS

### 1. Expandable Transaction Rows

Using Tailwind for expandable rows on mobile devices:

```html
<!-- Main row with minimal info on mobile -->
<tr 
  class="border-b hover:bg-gray-50" 
  hx-get="/api/transactions/123/details" 
  hx-target="#transaction-details-123" 
  hx-trigger="click"
>
  <td class="py-2 px-4">5/12/23</td>
  <td class="py-2 px-4 font-medium text-red-600">-$45.20</td>
  <!-- More columns only visible on larger screens -->
  <td class="hidden lg:table-cell py-2 px-4">Grocery Store</td>
  <td class="hidden lg:table-cell py-2 px-4">Food</td>
  <td class="hidden lg:table-cell py-2 px-4">
    <span class="inline-flex items-center px-2 py-1 text-xs rounded-full bg-green-100 text-green-800">
      <span class="h-2 w-2 rounded-full bg-green-600 mr-1"></span>
      Submitted
    </span>
  </td>
</tr>
<!-- Details row that's populated via HTMX on mobile -->
<tr id="transaction-details-123" class="bg-gray-50 lg:hidden"></tr>
```

### 2. Modal Dialogs

Using Tailwind's responsive utilities for modals:

```html
<!-- Full screen on mobile, centered with max-width on larger screens -->
<div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-40">
  <div class="bg-white w-full h-full sm:w-auto sm:h-auto sm:max-w-lg sm:rounded-lg sm:max-h-[80vh] overflow-y-auto">
    <div class="p-4 border-b">
      <h2 class="text-xl font-medium">Import Transactions</h2>
    </div>
    
    <div class="p-6">
      <!-- Form content -->
    </div>
    
    <!-- Actions at the bottom on mobile, in a footer on larger screens -->
    <div class="border-t p-4 sm:flex sm:justify-end sm:space-x-2">
      <button class="w-full sm:w-auto mb-2 sm:mb-0 py-2 px-4 bg-gray-200 hover:bg-gray-300 rounded">
        Cancel
      </button>
      <button class="w-full sm:w-auto py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded">
        Import
      </button>
    </div>
  </div>
</div>
```

### 3. Fixed Action Bar

Using Tailwind for a fixed action bar on mobile, regular positioning on desktop:

```html
<!-- Fixed at bottom on mobile, static positioning on md+ screens -->
<div class="fixed md:static bottom-0 left-0 right-0 bg-white border-t md:border-0 shadow-lg md:shadow-none p-4 md:p-0 flex justify-between items-center z-10">
  <div class="text-sm">
    <span class="font-medium">5</span> selected
  </div>
  <button class="bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded">
    Submit to YNAB
  </button>
</div>

<!-- Add padding to the bottom of the content on mobile to prevent the action bar from covering content -->
<div class="pb-16 md:pb-0">
  <!-- Main content -->
</div>
```

### 4. Responsive Navigation

Using Tailwind for a hamburger menu on mobile, horizontal navigation on desktop:

```html
<!-- Mobile menu button -->
<button class="md:hidden p-2" id="mobile-menu-button">
  <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7"></path>
  </svg>
</button>

<!-- Navigation links - vertical on mobile, horizontal on desktop -->
<nav class="hidden md:flex md:items-center">
  <a href="/dashboard" class="block md:inline-block py-2 px-4 text-blue-600">Dashboard</a>
  <a href="/accounts" class="block md:inline-block py-2 px-4">Accounts</a>
  <a href="/settings" class="block md:inline-block py-2 px-4">Settings</a>
</nav>
```

## Accessibility with TailwindCSS

Our TailwindCSS implementation ensures accessibility through:

1. **Sufficient Color Contrast**: Using Tailwind's color system with appropriate contrast ratios
   ```html
   <!-- Good contrast for text on background -->
   <p class="text-gray-900 bg-white">Main text</p>
   <p class="text-white bg-blue-800">Inverted text</p>
   ```

2. **Keyboard Navigation**: Adding Tailwind's focus styles for keyboard users
   ```html
   <button class="px-4 py-2 bg-blue-600 text-white focus:outline-none focus:ring-2 focus:ring-blue-400 focus:ring-opacity-50">
     Submit
   </button>
   ```

3. **Focus Visibility**: Using Tailwind's focus utilities consistently
   ```html
   <!-- Form field with clear focus indicators -->
   <input 
     class="border border-gray-300 rounded-md px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
     type="text"
   />
   ```

4. **Screen Reader Support**: Combining Tailwind with proper ARIA attributes and semantic HTML
   ```html
   <!-- Screen reader text using Tailwind's sr-only utility -->
   <button class="p-2 bg-blue-600 rounded-full">
     <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
       <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
     </svg>
     <span class="sr-only">Add new transaction</span>
   </button>
   ```

5. **Touch Target Size**: Using Tailwind's sizing utilities for appropriate touch targets
   ```html
   <!-- Properly sized touch target -->
   <button class="h-12 w-12 flex items-center justify-center">
     <!-- Button content -->
   </button>
   ```

6. **Resize Compatibility**: Using Tailwind's responsive utilities and relative sizing
   ```html
   <!-- Text that scales well -->
   <p class="text-base sm:text-lg lg:text-xl">
     This text will be appropriately sized on all screens
   </p>
   ```

7. **Reduced Motion**: Using Tailwind's prefers-reduced-motion utility
   ```html
   <!-- Animation that respects user preferences -->
   <div class="animate-spin motion-reduce:animate-none">
     <!-- Animated element -->
   </div>
   ```

These patterns are applied consistently throughout our UI components to ensure accessibility while maintaining our design system.