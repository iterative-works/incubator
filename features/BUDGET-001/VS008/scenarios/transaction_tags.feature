# Vertical Slice: Transaction Tags Support (VS-008)
# Business Value: Enables more granular classification and reporting by supporting YNAB tags
# UI Components:
# - TagSelector
# - TagManager
# - TagFilterControl
# - BulkTagging
# Dependencies:
# - Requires "Transaction Management UI" slice
# - Requires "YNAB Submission" slice

@slice:transaction-tags @value:low @phase:3
Feature: Transaction Tags Support
  As a finance team member
  I want to assign and manage tags for transactions
  So that I can classify transactions beyond categories and improve reporting

  Background:
    Given I am logged in as a finance team member
    And there are categorized transactions in the system
    And YNAB tags have been imported into the system

  # UI Component: TagSelector
  @ui-prototype
  Scenario: Validate tag selector UI prototype
    Given I am presented with the tag selector prototype
    When I interact with the tag selector for a transaction
    Then I should see a multi-select dropdown of available tags
    And I should see tags grouped by usage frequency
    And I should see a search function to find specific tags
    And I should see color indicators matching YNAB tag colors
    And I should see a "+" button to create new tags
    And I should be able to add multiple tags to a single transaction

  # UI Component: TagManager
  @ui-prototype
  Scenario: Validate tag manager UI prototype
    Given I am presented with the tag manager prototype
    When I interact with the tag management controls
    Then I should see a list of all available tags with usage counts
    And I should see options to create, edit, or delete tags
    And I should see a color picker for tag colors
    And I should see a sync status with YNAB for each tag
    And I should see options to merge similar tags
    And I should see a "Sync with YNAB" button to update tag definitions

  # User Flow: Add Tags to Transaction
  @user-flow
  Scenario: Add multiple tags to a single transaction
    Given I am viewing a list of categorized transactions
    When I click on the tags field for a transaction with description "Office Lunch Meeting"
    Then I should see the tag selector dropdown
    When I select the tags "Business", "Reimbursable", and "Team Building"
    And I click "Apply"
    Then the transaction should update with all three tags displayed
    And I should see a confirmation message "Tags updated for 1 transaction"
    And when this transaction is submitted to YNAB, the tags should be included
    And the tags should be visible in both my system and YNAB

  # User Flow: Bulk Tag Application
  @user-flow
  Scenario: Apply tags to multiple transactions at once
    Given I am viewing a list of categorized transactions
    When I select 8 transactions related to a business trip
    And I click "Bulk Edit" and select "Edit Tags"
    Then I should see the bulk tag editor
    When I select "Business" and "Reimbursable" tags
    And I toggle "Add to existing tags" to ON
    And I click "Apply to All"
    Then all 8 transactions should have the "Business" and "Reimbursable" tags added
    And any existing tags on those transactions should be preserved
    And I should see a confirmation message "Tags updated for 8 transactions"

  # User Flow: Filter by Tags
  @user-flow
  Scenario: Find transactions using tag filters
    Given I have transactions with various tags in the system
    When I expand the filters panel on the transaction list
    And I select "Reimbursable" from the tag filter dropdown
    Then the list should update to show only transactions with the "Reimbursable" tag
    When I additionally select "Business" from the tag filter
    Then the list should update to show transactions with both "Reimbursable" AND "Business" tags
    When I toggle the filter mode to "Any Selected Tag"
    Then the list should update to show transactions with EITHER "Reimbursable" OR "Business" tags
    And I should see a count of matching transactions

  # Edge Case: Tag Synchronization with YNAB
  @edge-case
  Scenario: Handle tag synchronization conflicts with YNAB
    Given I have created local tags that don't exist in YNAB
    And I have transactions tagged with these local tags
    When I click "Sync Tags with YNAB"
    Then the system should identify tags that don't exist in YNAB
    And I should see a dialog showing which tags need to be created in YNAB
    And I should see options to "Create in YNAB", "Map to Existing", or "Keep Local"
    When I choose different options for different tags
    And complete the synchronization
    Then tags should be properly synchronized according to my choices
    And transactions should maintain their correct tag associations

  # Domain Concept: Tag Analytics
  @domain-discovery
  Scenario: Analyze tag usage patterns
    Given I have been using tags on transactions for several months
    When I navigate to the "Tag Analytics" section
    Then I should see statistics for each tag including:
      | Metric                | Description                                       |
      | Usage frequency       | Number of transactions with this tag              |
      | Total amount          | Sum of amounts for transactions with this tag     |
      | Common categories     | Categories most frequently used with this tag     |
      | Common co-tags        | Other tags that frequently appear with this tag   |
    And I should see trend charts showing tag usage over time
    And I should see spending by tag compared to budget allocations
    And I should be able to export tag-based reports for expense tracking
