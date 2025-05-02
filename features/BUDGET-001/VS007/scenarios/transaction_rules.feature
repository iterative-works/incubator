# Vertical Slice: Transaction Rules Creation (VS-007)
# Business Value: Enables creation of custom rules for automatic categorization, improving accuracy over time
# UI Components:
# - RuleBuilder
# - RuleList
# - RuleTestingWorkbench
# - RulePriorityManager
# Dependencies:
# - Requires "Transaction Management UI" slice
# - Requires "AI-Powered Categorization" slice

@slice:transaction-rules @value:medium @phase:3
Feature: Transaction Rules Creation
  As a finance team member
  I want to create and manage custom rules for transaction categorization
  So that I can automate recurring patterns and improve categorization accuracy

  Background:
    Given I am logged in as a finance team member
    And there are transactions of various types in the system

  # UI Component: RuleBuilder
  @ui-prototype
  Scenario: Validate rule builder UI prototype
    Given I am presented with the rule builder prototype
    When I interact with the rule creation form
    Then I should see a condition builder with options for "contains," "starts with," "ends with," "equals," "matches regex"
    And I should see fields to select which transaction attributes to match (description, amount, date, merchant)
    And I should see the ability to combine multiple conditions with AND/OR operators
    And I should see a target category selector for the rule outcome
    And I should see a rule priority slider from "Low" to "High"
    And I should see a "Test Rule" button to validate before saving

  # UI Component: RuleList
  @ui-prototype
  Scenario: Validate rule list UI prototype
    Given I am presented with the rule list prototype
    When I interact with the list of existing rules
    Then I should see each rule with its conditions summarized
    And I should see the target category for each rule
    And I should see the rule priority and status (active/inactive)
    And I should see controls to edit, delete, or toggle each rule
    And I should see a count of how many times each rule has been applied
    And I should see a search/filter function to find specific rules

  # User Flow: Create Simple Rule
  @user-flow
  Scenario: Create a simple categorization rule
    Given I am on the rules management page
    When I click "Create New Rule"
    Then I should see the rule builder form
    When I enter "Rule for Coffee Shops" as the rule name
    And I add a condition where "Description" "contains" "coffee"
    And I select "Food & Dining" as the target category
    And I set priority to "Medium"
    And I click "Save Rule"
    Then I should see a confirmation message "Rule created successfully"
    And the new rule should appear in the rule list
    And when new transactions with "coffee" in the description are imported
    Then they should be automatically categorized as "Food & Dining"

  # User Flow: Create Complex Rule with Multiple Conditions
  @user-flow
  Scenario: Create a complex rule with multiple conditions
    Given I am on the rules management page
    When I click "Create New Rule"
    Then I should see the rule builder form
    When I enter "Utility Bills Rule" as the rule name
    And I add a condition where "Description" "contains" "electric"
    And I add another condition using OR where "Description" "contains" "utility"
    And I add another condition using AND where "Amount" "is greater than" "50"
    And I select "Bills & Utilities" as the target category
    And I set priority to "High"
    And I click "Test Rule"
    Then I should see matching test transactions from my history
    When I click "Save Rule"
    Then the complex rule should be saved
    And future transactions matching all conditions should be categorized accordingly

  # User Flow: Edit Existing Rule
  @user-flow
  Scenario: Modify an existing categorization rule
    Given I have previously created a rule for "grocery" transactions
    When I view the rule list
    And I click "Edit" on the grocery rule
    Then I should see the rule builder with the existing rule configuration
    When I add an additional condition for merchants containing "supermarket"
    And I change the priority from "Medium" to "High"
    And I click "Save Changes"
    Then I should see a confirmation message "Rule updated successfully"
    And the modified rule should be applied to future matching transactions
    And previously categorized transactions should remain unchanged

  # Edge Case: Rule Conflict Resolution
  @edge-case
  Scenario: Handle conflicts between multiple matching rules
    Given I have created the following rules:
      | Rule Name        | Condition                  | Category            | Priority |
      | Coffee Rule      | Description contains coffee | Food & Dining      | Medium   |
      | Starbucks Rule   | Merchant equals Starbucks  | Personal Spending   | High     |
    When a new transaction with merchant "Starbucks" and description "Coffee" is imported
    Then both rules should match the transaction
    But the "Starbucks Rule" should be applied due to higher priority
    And the transaction should be categorized as "Personal Spending"
    And the rule application log should record both matching rules
    And indicate which rule took precedence and why

  # Domain Concept: Rule Analytics
  @domain-discovery
  Scenario: Analyze rule effectiveness
    Given I have multiple categorization rules that have been active for some time
    When I navigate to the "Rule Analytics" section
    Then I should see statistics for each rule including:
      | Metric                     | Description                                      |
      | Application count          | Number of times the rule was applied             |
      | Override rate              | Percentage of rule applications later overridden |
      | Average confidence score   | AI confidence score when rule was applied        |
      | Last applied               | Date/time the rule was last used                 |
    And I should see recommendations for rule improvements
    And I should see unused rules that could be archived
    And I should be able to refine rules directly from the analytics view
