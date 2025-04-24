@financial-integration @categorization
Feature: Transaction Categorization
  As a finance team member
  I want transactions to be automatically categorized and reviewable
  So that I can ensure accurate budget categorization with minimal effort

  Background:
    # Note: Authentication is deferred to a future iteration
    # Given I am logged in as an administrator
    Given the system is connected to Fio Bank and YNAB APIs

  @categorization @ai
  Scenario: AI categorization of imported transactions
    Given "10" transactions have been imported from Fio Bank
    When the AI categorization process completes
    Then each transaction should have an assigned YNAB category
    And the transaction status should update to "Categorized"
    And the categorization accuracy should be at least "80%"

  @ui @categorization
  Scenario: Manual modification of transaction category
    Given transactions have been categorized by AI
    When I select the transaction with description "Supermarket Purchase"
    And I change its category from "Groceries" to "Dining Out"
    Then the transaction should update with the new category
    And the change should be saved in the database
    And the modification should be logged in the audit trail

  @bulk-actions
  Scenario: Bulk category modification
    Given transactions have been categorized by AI
    When I select "3" transactions
    And I apply the category "Transportation" to all selected transactions
    Then all selected transactions should update with the new category
    And the changes should be saved in the database