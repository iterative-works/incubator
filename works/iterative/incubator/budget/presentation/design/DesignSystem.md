# Budget UI Design System

## Color Palette

### Primary Colors
- **Primary Blue**: `#3366CC` - Used for primary actions, key UI elements
- **Secondary Blue**: `#5588EE` - Used for secondary actions, hover states
- **Accent Green**: `#44BB77` - Used for success states, positive values
- **Accent Red**: `#FF4444` - Used for error states, negative values
- **Neutral Dark**: `#333333` - Used for main text
- **Neutral Gray**: `#888888` - Used for secondary text, borders
- **Neutral Light**: `#EEEEEE` - Used for background, disabled states
- **White**: `#FFFFFF` - Used for card backgrounds, text on dark backgrounds

### Semantic Colors
- **Success**: `#44BB77` - Positive outcomes, confirmations
- **Warning**: `#FFAA44` - Alerts, warnings
- **Error**: `#FF4444` - Errors, destructive actions
- **Info**: `#5588EE` - Informational messages

## Typography

### Font Families
- **Primary Font**: System UI font stack
  - `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif`
- **Monospace Font**: For transaction amounts, code
  - `SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace`

### Type Scale
- **Heading 1**: 24px/1.2 (Dashboard title)
- **Heading 2**: 20px/1.3 (Section headers)
- **Heading 3**: 18px/1.4 (Card titles)
- **Body Text**: 16px/1.5 (Default text size)
- **Small Text**: 14px/1.5 (Secondary information, metadata)
- **Micro Text**: 12px/1.4 (Captions, footnotes)

### Font Weights
- **Regular**: 400
- **Medium**: 500
- **Bold**: 700

## Spacing

### Grid
- **Base Unit**: 8px
- **Spacing Scale**:
  - **2xs**: 4px (0.5x base)
  - **xs**: 8px (1x base)
  - **sm**: 16px (2x base)
  - **md**: 24px (3x base)
  - **lg**: 32px (4x base)
  - **xl**: 48px (6x base)
  - **2xl**: 64px (8x base)

### Layout
- **Container Max Width**: 1200px
- **Card Padding**: 16px (sm)
- **Section Margin**: 32px (lg)

## Components

### Buttons
- **Height**: 40px
- **Padding**: 8px 16px (xs sm)
- **Border Radius**: 4px
- **Font Weight**: Medium (500)
- **Transition**: 0.2s ease-in-out

#### Button Variants
- **Primary**: Background: Primary Blue, Text: White
- **Secondary**: Border: Neutral Gray, Text: Neutral Dark
- **Danger**: Background: Accent Red, Text: White
- **Success**: Background: Accent Green, Text: White

### Forms

#### Input Fields
- **Height**: 40px
- **Padding**: 8px 12px
- **Border**: 1px solid Neutral Gray
- **Border Radius**: 4px
- **Focus**: 2px outline Primary Blue

#### Select/Dropdown
- **Height**: 40px
- **Padding**: 8px 12px
- **Border**: 1px solid Neutral Gray
- **Border Radius**: 4px
- **Indicator**: 16px chevron icon

### Cards
- **Background**: White
- **Border Radius**: 8px
- **Shadow**: 0 2px 8px rgba(0,0,0,0.1)
- **Padding**: 16px (sm)

### Tables
- **Row Height**: 48px
- **Cell Padding**: 8px 12px
- **Header Background**: Neutral Light
- **Border**: 1px solid Neutral Light
- **Hover Background**: Neutral Light (slightly darker)
- **Selected Background**: Light Primary Blue (10% opacity)

### Status Indicators
- **Size**: 8px (circle)
- **Pending**: Neutral Gray
- **Processing**: Primary Blue
- **Complete**: Accent Green
- **Error**: Accent Red

### Modals
- **Backdrop**: Black at 50% opacity
- **Background**: White
- **Border Radius**: 8px
- **Width**: Max 600px
- **Padding**: 24px (md)

## Responsive Breakpoints
- **Small**: 576px and up
- **Medium**: 768px and up
- **Large**: 992px and up
- **Extra Large**: 1200px and up

## Accessibility
- Minimum color contrast ratio of 4.5:1
- Focus states clearly visible
- Interactive elements sized appropriately for touch
- Screen reader friendly labeling