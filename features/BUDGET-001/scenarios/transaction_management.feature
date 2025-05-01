# Vertical Slice: Transaction Management & Categorization
# Business Value: Enables viewing and efficient categorization of transactions, with AI assistance to reduce manual effort
# UI Components:
# - TransactionList
# - CategorySelector
# - TransactionFilters
# - BulkActionControls
# Dependencies:
# - Requires "Transaction Import from Fio Bank" slice for transaction data
# - OpenAI API integration for AI categorization

@slice:transaction-management @value:high
Feature: Transaction Management & Categorization
  As a finance team member
  I want to view imported transactions and efficiently categorize them
  So that I can prepare them for submission to YNAB with minimal manual effort

  Background:
    Given I am logged in as a finance team member
    And there are transactions imported from Fio Bank in the system

  # UI Component: TransactionList
  @ui-prototype
  Scenario: Validate transaction list UI prototype
    Given I am presented with the transaction list prototype
    When I interact with the list of sample transactions
    Then I should be able to see transaction details including date, amount, and description
    And I should be able to select individual transactions
    And I should see status indicators for each transaction
    And I should see the assigned category for each transaction

  # UI Component: CategorySelector
  @ui-prototype
  Scenario Outline: Validate category selector with different inputs
    Given I am presented with the category selector prototype
    When I search for "<search_term>"
    Then I should see "<expected_categories>" in the results

    Examples:
      | search_term | expected_categories                     |
      | food        | Food & Dining, Groceries                |
      | bill        | Bills & Utilities, Rent                 |
      | zz          | No categories found                     |

  # User Flow: AI Categorization
  @user-flow
  Scenario: Automatically categorize imported transactions
    Given I have 15 uncategorized transactions imported from Fio Bank
    When I click the "Run AI Categorization" button
    Then the system should analyze each transaction description
    And assign appropriate YNAB categories based on AI analysis
    And update the transaction status to "Categorized"
    And display a summary showing "15 transactions categorized"
    And at least 12 of the transactions should have appropriate categories (80% accuracy)

  # User Flow: Manual Category Modification
  @user-flow
  Scenario: Manually modify an AI-assigned category
    Given I am viewing the transaction list with categorized transactions
    When I select a transaction with description "Grocery Store Purchase"
    And I see it was categorized as "Entertainment"
    And I click on the category field
    Then I should see a dropdown of available YNAB categories
    When I select "Food & Dining" from the dropdown
    And I click "Save"
    Then the transaction should update with the new category "Food & Dining"
    And the UI should show a confirmation of the change

  # Edge Case: Handle Special Characters
  @edge-case
  Scenario: Properly display and process transactions with special characters
    Given I have imported transactions with special characters in descriptions
    When I view the transaction list
    Then all transactions should be displayed correctly with their special characters
    And when I select a transaction with special characters
    And modify its category
    Then the system should save the changes without losing or corrupting the special characters
