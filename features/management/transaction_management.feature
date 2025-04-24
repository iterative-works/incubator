@financial-integration @transaction-management
Feature: Transaction Management
  As a finance team member
  I want to view and filter transactions by various criteria
  So that I can find and work with specific transactions efficiently

  Background:
    # Note: Authentication is deferred to a future iteration
    # Given I am logged in as an administrator
    Given the system contains transactions with different statuses

  @filtering @ui
  Scenario: Filter transactions by status
    Given I have transactions with different statuses
    When I select the filter option "Categorized"
    Then the transaction table should only display transactions with "Categorized" status
    And the count of displayed transactions should match the count of categorized transactions

  @filtering @ui
  Scenario: Search transactions by description
    Given I have multiple transactions with different descriptions
    When I enter "Grocery" in the search field
    Then the transaction table should only display transactions containing "Grocery" in their description

  @sorting @ui
  Scenario: Sort transactions by date
    Given I have transactions with different dates
    When I click on the "Date" column header
    Then the transactions should be sorted by date in ascending order
    When I click on the "Date" column header again
    Then the transactions should be sorted by date in descending order

  @filtering @export
  Scenario: Export filtered transactions
    Given I have filtered the transaction list to show only "Categorized" items
    When I click the "Export" button
    Then a CSV file should be downloaded
    And the CSV file should only contain the transactions with "Categorized" status