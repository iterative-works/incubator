# Vertical Slice: Transaction Management UI (VS-002)
# Business Value: Provides a comprehensive interface for viewing, searching, and managing transactions
# UI Components:
# - TransactionList
# - AdvancedFilters
# - SortingControls
# - BatchOperations
# Dependencies:
# - Requires "Transaction Import from Fio Bank" slice for transaction data

@slice:transaction-management @value:high @phase:2
Feature: Transaction Management UI
  As a finance team member
  I want a comprehensive interface to view and manage imported transactions
  So that I can efficiently work with large sets of financial data

  Background:
    Given I am logged in as a finance team member
    And there are transactions imported from Fio Bank in the system

  # UI Component: TransactionList
  @ui-prototype
  Scenario: Validate transaction list UI prototype
    Given I am presented with the transaction list prototype
    When I interact with the list of sample transactions
    Then I should see a responsive table with columns for date, amount, description, counterparty, category, and status
    And I should see pagination controls that allow navigation between pages
    And I should see a "Show All" option that loads all transactions with infinite scroll
    And I should see a visual indicator for the transaction status (Imported, Categorized, Submitted)
    And I should see positive amounts in green and negative amounts in red

  # UI Component: AdvancedFilters
  @ui-prototype
  Scenario: Validate advanced filters UI prototype
    Given I am presented with the advanced filters prototype
    When I expand the filters panel
    Then I should see filter options for date range, amount range, status, and category
    And I should see a text search field that searches across all transaction fields
    And I should see options to save my filter combinations
    And I should see a way to quickly clear all filters
    And applying filters should immediately update the transaction list

  # User Flow: Search and Filter Transactions
  @user-flow
  Scenario: Find specific transactions using search and filters
    Given I have 100 transactions in the system with various dates, amounts, and categories
    When I enter "coffee" in the search field
    Then the list should filter to show only transactions with "coffee" in any field
    When I additionally filter by date range "2025-04-01" to "2025-04-15"
    Then the list should update to show only coffee-related transactions within that date range
    When I additionally filter by amount range "$5" to "$20"
    Then the list should update to show only matching transactions
    And I should see a count of "3 transactions found" matching all criteria
    And I should be able to clear all filters with a single click

  # User Flow: Sort Transactions
  @user-flow
  Scenario: Sort transactions by different columns
    Given I am viewing the transaction list with multiple transactions
    When I click on the "Date" column header
    Then the transactions should sort by date in descending order (newest first)
    When I click on the "Date" column header again
    Then the transactions should sort by date in ascending order (oldest first)
    When I click on the "Amount" column header
    Then the transactions should sort by amount in descending order (largest first)
    And the current sort column and direction should be visually indicated
    And the sort preference should persist if I leave and return to the page

  # User Flow: Batch Operations
  @user-flow
  Scenario: Perform batch operations on selected transactions
    Given I am viewing the transaction list with multiple transactions
    When I select 10 specific transactions using the checkboxes
    Then I should see a batch operations panel appear
    And I should see options to "Delete", "Categorize", and "Submit to YNAB"
    When I click the "Delete" option
    Then I should see a confirmation dialog asking to confirm deletion of 10 transactions
    When I confirm the deletion
    Then the selected transactions should be removed from the list
    And I should see a confirmation message "10 transactions deleted successfully"

  # Edge Case: Handle Large Transaction Sets
  @edge-case
  Scenario: Efficiently manage very large transaction sets
    Given I have imported 1000+ transactions from multiple months
    When I access the transaction management screen
    Then the system should load the most recent 50 transactions by default
    And pagination should be available to access older transactions
    And all filtering and sorting operations should complete within 2 seconds
    And I should have an option to export the current filtered view to CSV
    And memory usage should remain stable even when viewing large transaction sets

  # Domain Concept: Transaction Grouping
  @domain-discovery
  Scenario: Group similar transactions for batch operations
    Given I have multiple similar transactions from the same merchant
    When I view the transaction list
    Then the system should identify transaction groups based on merchant and description patterns
    And I should see an option to "Show grouped transactions"
    When I enable this option
    Then similar transactions should be visually grouped together
    And I should see a count for each group
    And I should be able to expand/collapse each group
    And I should be able to select all transactions in a group with a single click
