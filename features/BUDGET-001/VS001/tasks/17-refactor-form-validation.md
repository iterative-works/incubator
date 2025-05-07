# Task 17: Refactor Transaction Import Form Validation

## Overview

The current implementation of the transaction import form has validation issues where components validate independently, causing inconsistent form state. When one component updates its validation state (e.g., account selector), the import button remains disabled even if the overall form becomes valid.

This task involves refactoring the form validation approach to treat the form as a cohesive entity with centralized validation logic, ensuring consistent state across all components.

## Detailed Requirements

### Issues to Fix
1. Account selector validation doesn't update the import button state
2. Multiple component validations don't share a single validity state
3. Date range selector validation doesn't coordinate with account validation
4. Form validation is fragmented across multiple endpoints

### Implementation Goals
1. Create a centralized form validation service for all form fields
2. Implement a single validation endpoint that processes all validations
3. Maintain overall form validity state that all components can access
4. Update all components to work with the unified validation approach
5. Ensure the import button reflects the overall form validity

## Implementation Plan

### 1. Create Form Validation Service
- Create `TransactionImportFormValidationService` with methods to validate:
  - Account selection
  - Date range validity
  - Combined form state
- Implement error mapping for user-friendly messages

### 2. Update TransactionImportModule
- Replace individual validation endpoints with a single `/transactions/import/validate` endpoint
- Implement server-side logic to validate complete form state
- Return both component-specific HTML and form-wide validity state

### 3. Update View Components
- Modify form components to use the centralized validation:
  - Update `AccountSelector` to include form-level context
  - Update `DateRangeSelector` to include form-level context
  - Add a hidden form state element to track overall validity
- Update `ImportButton` to respond to form validation state

### 4. Update HTMX Attributes
- Modify HTMX attributes to include full form context in validation requests
- Add HTMX selectors to update multiple elements from validation responses
- Ensure proper event handling for form state changes

## Related Documentation
- See [HTMX Form Validation Guide](/ai-context/architecture/guides/htmx_form_validation_guide.md) for the architectural approach
- Refer to [BDD-Driven UI Design Guide](/ai-context/architecture/guides/bdd_driven_ui_mapping_guide.md) for aligning with scenarios

## Acceptance Criteria
1. When selecting an account, the import button should be enabled if the form is valid
2. Date validation should consider both date fields together for validation
3. Form validation should prevent submission when either the account or dates are invalid
4. Validation error messages should appear immediately next to the relevant fields
5. All validation should happen server-side but reflect in the UI immediately
6. The form should maintain a consistent state across all components

## Implementation Details
- Use the Model-View-Presenter pattern with a centralized presenter for form validation
- Follow the Functional Core approach with pure validation logic
- Implement progressive enhancement to ensure the form works without JavaScript

## Testing Recommendations
- Test each validation rule independently
- Test cross-field validations
- Test the full form validation with various combinations of valid/invalid inputs
- Test the UI state consistency after validation
