# Responsive Design Considerations

This document outlines the responsive design strategy for the Budget UI components, ensuring they work across different device sizes.

## Responsive Breakpoints

We'll use the following breakpoints as defined in our Design System:

- **Small (sm)**: 576px and up
- **Medium (md)**: 768px and up
- **Large (lg)**: 992px and up
- **Extra Large (xl)**: 1200px and up

## Core Responsive Principles

1. **Mobile-First Approach**: Design for mobile first, then enhance for larger screens
2. **Fluid Grid System**: Use percentage-based widths rather than fixed pixel values
3. **Flexible Images**: Scale images proportionally to fit container size
4. **Breakpoint-Based Adaptations**: Adjust layout and component visibility at defined breakpoints
5. **Touch-Friendly Targets**: Ensure interactive elements are at least 44x44px on touch devices
6. **Progressive Enhancement**: Core functionality works on all devices, enhanced features on larger screens

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

## HTML/CSS Implementation Approach

### 1. Fluid Container Structure

```html
<div class="container">
  <div class="row">
    <div class="col col--12 col--md-6 col--lg-3">
      <!-- Statistics Card 1 -->
    </div>
    <div class="col col--12 col--md-6 col--lg-3">
      <!-- Statistics Card 2 -->
    </div>
    <!-- Additional cards -->
  </div>
</div>
```

### 2. Media Query Strategy

```css
/* Base styles (mobile first) */
.statistics-panel {
  display: flex;
  flex-direction: column;
}

/* Small devices */
@media (min-width: 576px) {
  .statistics-panel {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }
}

/* Large devices */
@media (min-width: 992px) {
  .statistics-panel {
    grid-template-columns: 1fr 1fr 1fr 1fr;
  }
}
```

### 3. Responsive Tables

```html
<div class="table-responsive">
  <table class="transaction-table">
    <!-- Table content -->
  </table>
</div>
```

```css
.table-responsive {
  overflow-x: auto;
}

@media (min-width: 992px) {
  .table-responsive {
    overflow-x: visible;
  }
}
```

### 4. Responsive Card Pattern

```html
<div class="card">
  <div class="card__content">
    <div class="card__label">Total Transactions</div>
    <div class="card__value">120</div>
    <div class="card__detail card__detail--hidden-sm">
      <!-- Extra details shown only on larger screens -->
    </div>
  </div>
</div>
```

```css
.card__detail--hidden-sm {
  display: none;
}

@media (min-width: 768px) {
  .card__detail--hidden-sm {
    display: block;
  }
}
```

### 5. Touch-Friendly Controls

```css
@media (pointer: coarse) {
  .button, 
  .checkbox, 
  .select,
  .table-cell--interactive {
    min-height: 44px;
    min-width: 44px;
    padding: 12px;
  }
  
  .select option {
    padding: 12px;
  }
}
```

## Mobile-Specific Interaction Patterns

### 1. Expandable Transaction Rows

On mobile devices, transaction rows will be expandable to show additional details:

```html
<tr class="transaction-row" hx-get="/api/transactions/123/details" hx-target="#transaction-details-123" hx-trigger="click">
  <td class="transaction-date">5/12/23</td>
  <td class="transaction-amount">-$45.20</td>
</tr>
<tr id="transaction-details-123" class="transaction-details"></tr>
```

### 2. Modal Dialogs

Adjust modal dialogs to be full-screen on mobile:

```css
.modal {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1000;
}

@media (min-width: 576px) {
  .modal {
    position: fixed;
    top: 10%;
    right: 10%;
    bottom: 10%;
    left: 10%;
    max-width: 600px;
    margin: 0 auto;
  }
}
```

### 3. Fixed Action Bar

For mobile, critical actions like "Submit to YNAB" will appear in a fixed bar at the bottom of the screen:

```html
<div class="action-bar">
  <div class="action-bar__indicator">5 selected</div>
  <button class="button button--primary">Submit to YNAB</button>
</div>
```

```css
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: white;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

@media (min-width: 768px) {
  .action-bar {
    position: static;
    box-shadow: none;
    padding: 0;
  }
}
```

## Accessibility Considerations

1. **Sufficient Color Contrast**: All text and UI elements maintain 4.5:1 contrast ratio
2. **Keyboard Navigation**: All interactive elements accessible via keyboard
3. **Focus Visibility**: Clear focus indicators for keyboard users
4. **Screen Reader Support**: Appropriate ARIA attributes and semantic HTML
5. **Touch Target Size**: Minimum 44x44px target size on touch devices
6. **Resize Compatibility**: Content readable when zoomed up to 200%
7. **Reduced Motion**: Respect user preferences for reduced motion