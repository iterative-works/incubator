@financial-integration @fio-bank @ynab
Feature: Fio Bank to YNAB Integration
  As a finance team member
  I want to automate the import and categorization of Fio Bank transactions into YNAB
  So that I can save time and improve the accuracy of my financial data

  Background:
    # Note: Authentication is deferred to a future iteration
    # Given I am logged in as an administrator
    Given the system is connected to Fio Bank and YNAB APIs

  @import @happy-path
  Scenario: Successfully import transactions from Fio Bank
    When I initiate an import for the date range "2025-04-01" to "2025-04-15"
    Then the system should connect to Fio Bank API
    And retrieve all transactions for the specified date range
    And store them in the database
    And I should see "10" transactions in the transaction table
    And all transactions should have "Imported" status

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

  @submission @happy-path
  Scenario: Submit transactions to YNAB
    Given I have "5" reviewed and categorized transactions
    When I select all transactions and click "Submit to YNAB"
    Then the system should connect to YNAB API
    And submit the selected transactions with their categories
    And update their status to "Submitted"
    And I should see a confirmation with the submission results

  @error-handling @submission
  Scenario: Handle YNAB API connection failure
    Given I have "5" reviewed and categorized transactions
    And the YNAB API is unavailable
    When I select all transactions and click "Submit to YNAB"
    Then I should see an error message indicating connection failure
    And the transactions should maintain their "Categorized" status
    And the system should offer to retry the submission

  @duplicate-prevention
  Scenario: Prevent duplicate submission of transactions
    Given "5" transactions have been successfully submitted to YNAB
    When I attempt to submit the same transactions again
    Then the system should identify them as duplicates
    And prevent the resubmission
    And display a notification "Duplicate submission prevented"

  @filtering @ui
  Scenario: Filter transactions by status
    Given I have transactions with different statuses
    When I select the filter option "Categorized"
    Then the transaction table should only display transactions with "Categorized" status
    And the count of displayed transactions should match the count of categorized transactions

  @data-validation @import
  Scenario Outline: Validate transaction date range
    When I attempt to import transactions with date range "<start_date>" to "<end_date>"
    Then I should see the validation message "<message>"

    Examples:
      | start_date  | end_date    | message                                    |
      | 2025-04-01  | 2025-04-15  | Import initiated successfully              |
      | 2025-04-15  | 2025-04-01  | End date must be after start date          |
      | 2024-04-01  | 2024-04-30  | Date range cannot exceed 30 days           |
      |             | 2025-04-15  | Start date is required                     |
      | 2025-04-01  |             | End date is required                       |

  @security @deferred
  Scenario: Unauthorized access attempt (Deferred to Future Iteration)
    # Note: Authentication implementation has been deferred to a future iteration.
    # For the MVS, we'll use a deployment-specific security solution.
    #
    # Given I am not logged in
    # When I attempt to access the transaction management page
    # Then I should be redirected to the login page
    # And I should see a message "Authentication required"
