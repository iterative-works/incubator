@financial-integration @fio-bank @import
Feature: Fio Bank Transaction Import
  As a finance team member
  I want to import transactions from Fio Bank automatically
  So that I can save time and have accurate financial data for processing

  Background:
    # Note: Authentication is deferred to a future iteration
    # Given I am logged in as an administrator
    Given the system is connected to Fio Bank API

  @import @happy-path
  Scenario: Successfully import transactions from Fio Bank
    When I initiate an import for the date range "2025-04-01" to "2025-04-15"
    Then the system should connect to Fio Bank API
    And retrieve all transactions for the specified date range
    And store them in the database
    And I should see "10" transactions in the transaction table
    And all transactions should have "Imported" status

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