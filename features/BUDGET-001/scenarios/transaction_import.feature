# Vertical Slice: Transaction Import from Fio Bank
# Business Value: Enables automatic retrieval of transactions from Fio Bank, eliminating manual export/import processes
# UI Components:
# - ImportForm
# - DateRangeSelector
# - ImportProgressIndicator
# - ImportResultSummary
# Dependencies:
# - Fio Bank API access credentials configured in the system

@slice:transaction-import @value:high
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
    Then I should see a date range selector
    And I should see an import button
    And I should see a section for import history

  # UI Component: DateRangeSelector
  @ui-prototype
  Scenario Outline: Validate date range selector with different inputs
    Given I am presented with the date range selector prototype
    When I select "<start_date>" as the start date and "<end_date>" as the end date
    Then I should see "<validation_message>"

    Examples:
      | start_date | end_date   | validation_message                 |
      | 2025-04-01 | 2025-04-15 | Valid date range                   |
      | 2025-04-15 | 2025-04-01 | End date must be after start date  |
      | 2024-01-01 | 2024-04-01 | Date range cannot exceed 90 days   |

  # User Flow: Basic Import
  @user-flow
  Scenario: Successfully import transactions for a date range
    Given I am on the transaction import page
    When I select "2025-04-01" as the start date
    And I select "2025-04-15" as the end date
    And I click the "Import Transactions" button
    Then the system should connect to Fio Bank API
    And retrieve all transactions for the specified date range
    And store them in the database
    And display a summary showing "15 transactions successfully imported"
    And the transactions should appear in the transaction list with "Imported" status
