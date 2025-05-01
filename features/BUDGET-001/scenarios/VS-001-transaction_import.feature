# Vertical Slice: Transaction Import from Fio Bank (VS-001)
# Business Value: Enables automatic retrieval of transactions from Fio Bank, eliminating manual export/import processes
# UI Components:
# - ImportForm
# - DateRangeSelector
# - ImportProgressIndicator
# - ImportResultSummary
# Dependencies:
# - Fio Bank API access credentials configured in the system

@slice:transaction-import @value:high @phase:1
Feature: Transaction Import from Fio Bank
  As a finance team member
  I want to automatically import transactions from Fio Bank
  So that I don't have to manually export and import data

  Background:
    Given I am logged in as a finance team member
    And the system is configured with valid Fio Bank API credentials

  # UI Component: ImportForm
  @ui-prototype
  Scenario: Validate import form UI prototype
    Given I am presented with the import form prototype
    When I interact with the form controls
    Then I should see a date range selector with default values for current month
    And I should see an "Import Transactions" button that is enabled
    And I should see a section for import history showing the last 5 imports
    And I should see a loading indicator that appears during import operations

  # UI Component: DateRangeSelector
  @ui-prototype
  Scenario Outline: Validate date range selector with different inputs
    Given I am presented with the date range selector prototype
    When I select "<start_date>" as the start date and "<end_date>" as the end date
    Then I should see "<validation_message>"
    And the "Import Transactions" button should be "<button_state>"

    Examples:
      | start_date | end_date   | validation_message                 | button_state |
      | 2025-04-01 | 2025-04-15 | Valid date range                   | enabled      |
      | 2025-04-15 | 2025-04-01 | End date must be after start date  | disabled     |
      | 2024-01-01 | 2024-04-01 | Date range cannot exceed 90 days   | disabled     |
      |            | 2025-04-15 | Start date is required             | disabled     |
      | 2025-04-01 |            | End date is required               | disabled     |

  # User Flow: Basic Import
  @user-flow
  Scenario: Successfully import transactions for a date range
    Given I am on the transaction import page
    When I select "2025-04-01" as the start date
    And I select "2025-04-15" as the end date
    And I click the "Import Transactions" button
    Then I should see a progress indicator with status "Connecting to Fio Bank"
    And then the status should change to "Retrieving transactions"
    And then the status should change to "Storing transactions"
    And finally I should see a summary showing "15 transactions successfully imported"
    And the transactions should appear in the transaction list with "Imported" status
    And the import history should be updated with this import session

  # User Flow: Import with No Transactions
  @user-flow
  Scenario: Import with no transactions available
    Given I am on the transaction import page
    When I select "2025-06-01" as the start date
    And I select "2025-06-02" as the end date
    And I click the "Import Transactions" button
    Then the system should connect to Fio Bank API
    And after the import completes, I should see a message "No transactions found for the selected date range"
    And the import history should be updated with this import session marked as "No transactions"

  # Edge Case: API Connection Failure
  @edge-case
  Scenario: Handle Fio Bank API connection failure
    Given I am on the transaction import page
    And the Fio Bank API is temporarily unavailable
    When I select "2025-04-01" as the start date
    And I select "2025-04-15" as the end date
    And I click the "Import Transactions" button
    Then I should see an error message "Unable to connect to Fio Bank. Please try again later."
    And I should see a "Retry" button
    And the import should not be recorded in the import history

  # Domain Concept: Transaction De-duplication
  @domain-discovery
  Scenario: Import transactions that were previously imported
    Given I have previously imported transactions for the date range "2025-04-01" to "2025-04-15"
    When I select "2025-04-01" as the start date
    And I select "2025-04-15" as the end date
    And I click the "Import Transactions" button
    Then the system should identify 10 transactions that were previously imported
    And only store 5 new transactions that weren't previously imported
    And display a summary showing "5 new transactions imported, 10 duplicates skipped"
    And only the new transactions should appear in the transaction list with "Imported" status
