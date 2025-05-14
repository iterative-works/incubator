# Vertical Slice: Category Review & Modification (VS-004)
# Business Value: Enables human verification and correction of AI-assigned categories to ensure accuracy
# UI Components:
# - MinimalTransactionView (focused view for category review only)
# - CategoryEditor
# - BulkCategoryEditor
# - CategoryHistoryView
# Dependencies:
# - Requires "AI-Powered Categorization" slice (VS003)
# - Includes a minimal transaction view for category review purposes (not the full VS002)

@slice:category-review @value:high @phase:1
Feature: Category Review and Modification
  As a finance team member
  I want to review and modify AI-assigned categories
  So that I can ensure transactions are correctly categorized before submission to YNAB

  Background:
    Given I am logged in as a finance team member
    And there are AI-categorized transactions in the system

  # UI Component: CategoryEditor
  @ui-prototype
  Scenario: Validate category editor UI prototype
    Given I am presented with the category editor prototype
    When I interact with the category editor for a transaction
    Then I should see the current category with its confidence score
    And I should see a dropdown of all available YNAB categories organized hierarchically
    And I should see a search box to quickly find categories
    And I should see recently used categories at the top of the list
    And I should see a "Save" button that is enabled when changes are made

  # UI Component: BulkCategoryEditor
  @ui-prototype
  Scenario: Validate bulk category editor UI prototype
    Given I am presented with the bulk category editor prototype
    When I select multiple transactions with different categories
    And I interact with the bulk editor controls
    Then I should see a count of selected transactions
    And I should see an option to apply a single category to all selected transactions
    And I should see a warning that this will override existing categories
    And I should see a "Save All" button that is enabled when changes are made

  # UI Component: MinimalTransactionView
  @ui-prototype
  Scenario: Validate minimal transaction view for category review
    Given I am presented with the minimal transaction view prototype
    Then I should see a focused list of transactions with essential information
    And I should see columns for date, description, amount, category, and confidence score
    And I should see basic filters for low confidence scores and uncategorized transactions
    And I should be able to select transactions for category editing
    
  # User Flow: Single Transaction Recategorization
  @user-flow
  Scenario: Modify the category of a single transaction
    Given I am viewing the minimal transaction list for category review
    When I click on the category field for a transaction with description "Coffee Shop" categorized as "Entertainment"
    Then I should see a dropdown of available YNAB categories
    When I type "food" in the search box
    Then I should see filtered results including "Food & Dining"
    When I select "Food & Dining" from the dropdown
    And I click "Save"
    Then the transaction should update with the new category "Food & Dining"
    And I should see a confirmation message "Category updated successfully"
    And the confidence indicator should be updated to show "Manual" status

  # User Flow: Bulk Transaction Recategorization
  @user-flow
  Scenario: Modify the category of multiple transactions at once
    Given I am viewing the minimal transaction list for category review
    When I select 5 transactions with different descriptions but similar nature
    And I click the "Bulk Edit" button
    Then I should see the bulk category editor
    When I select "Transportation" from the category dropdown
    And I click "Apply to All"
    And I click "Save All"
    Then all 5 transactions should update with the "Transportation" category
    And I should see a confirmation message "5 transactions updated successfully"
    And each modified transaction should show "Manual" as its categorization method

  # Edge Case: Category Consistency Check
  @edge-case
  Scenario: System identifies inconsistent categorization patterns
    Given I have 10 transactions with description containing "GROCERY" in the system
    And 8 of them are categorized as "Food & Dining"
    And 2 of them are categorized as "Gifts"
    When I view the minimal transaction list for category review
    Then the 2 inconsistently categorized transactions should be flagged
    And I should see a suggestion to "Align categories with similar transactions"
    When I click this suggestion
    Then I should see an option to recategorize the outliers to match the majority
    And I can choose to apply or ignore this suggestion

  # Domain Concept: Category Learning
  @domain-discovery
  Scenario: System records category modification for future learning
    Given I modify the category of a transaction from "Entertainment" to "Food & Dining"
    When the system processes this change
    Then it should store this correction in the category learning database
    And it should associate the transaction's key terms with the correct category
    And it should increase the weight of this association based on manual intervention
    And future similar transactions should be more likely to be categorized correctly
