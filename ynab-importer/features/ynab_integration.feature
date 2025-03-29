Feature: YNAB Integration
  As a user of the YNAB Importer
  I want to connect my bank accounts with YNAB accounts
  So that I can automate my transaction imports with proper categorization

  Background:
    Given the system is properly set up
    And I am logged in as an administrator
    And at least one source account exists

  Scenario: Connect to YNAB API
    Given I have a YNAB personal access token
    When I navigate to the settings page
    And I enter my YNAB API token
    And I save the settings
    Then the system should verify the connection
    And I should see a message that the connection was successful
    And I should see my YNAB budgets listed

  Scenario: Select a YNAB budget
    Given I have connected to the YNAB API
    When I navigate to the settings page
    And I select a budget from the list of available YNAB budgets
    And I save the settings
    Then the system should load the accounts from that budget
    And I should see a message confirming the budget selection

  Scenario: Link a source account to a YNAB account
    Given I have connected to the YNAB API
    And I have selected a budget
    When I navigate to the source account edit page
    Then I should see a dropdown of available YNAB accounts
    When I select a YNAB account from the dropdown
    And I save the account
    Then the source account should be linked to the YNAB account
    And I should see the YNAB account name on the source account details page

  Scenario: Fetch YNAB categories for AI processing
    Given I have connected to the YNAB API
    And I have selected a budget
    When the system synchronizes with YNAB
    Then all YNAB categories should be imported
    And category groups should be preserved
    And hidden categories should be marked as such

  Scenario: Submit a transaction to YNAB
    Given I have a source account linked to a YNAB account
    And I have a processed bank transaction
    When I approve the transaction for submission
    Then the system should format the transaction according to YNAB requirements
    And submit it to the YNAB API
    And update the transaction status to "Submitted"
    And record the YNAB transaction ID

  Scenario: Batch submit multiple transactions
    Given I have multiple processed transactions ready for submission
    When I select all transactions
    And I choose to batch submit to YNAB
    Then each transaction should be submitted to YNAB
    And I should see a summary of the results
    And failed submissions should be clearly identified

  Scenario: Handle YNAB API errors
    Given I have a transaction ready for submission
    When the YNAB API returns an error
    Then the system should log the error details
    And display a user-friendly error message
    And allow me to retry the submission
    And the transaction status should remain unchanged

  Scenario: Verify transaction submission
    Given I have submitted a transaction to YNAB
    When I navigate to the transaction details
    Then I should see the YNAB transaction ID
    And the submission timestamp
    And a link to view the transaction in YNAB

  Scenario: Refresh YNAB account information
    Given I have accounts linked to YNAB
    When I choose to refresh YNAB data
    Then the system should update all account information from YNAB
    And update all category information from YNAB
    And show the last synchronization time