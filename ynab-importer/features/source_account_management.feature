Feature: Source Account Management
  As a user of the YNAB Importer
  I want to manage bank source accounts
  So that I can import transactions from my bank accounts to YNAB

  Background:
    Given the system is properly set up
    And I am logged in as an administrator

  Scenario: View list of source accounts
    When I navigate to the source accounts page
    Then I should see a list of all source accounts
    And I should see details including account name, bank ID, and status

  Scenario: Filter source accounts by status
    Given there are active and inactive source accounts
    When I navigate to the source accounts page
    And I select "Active" from the status filter
    Then I should see only active source accounts
    When I select "Inactive" from the status filter
    Then I should see only inactive source accounts
    When I select "All" from the status filter
    Then I should see all source accounts

  Scenario: Create a new source account
    When I navigate to the source accounts page
    And I click on "Add New Account"
    Then I should see the account creation form
    When I fill in the following details:
      | Field         | Value               |
      | Account Name  | My Checking Account |
      | Account ID    | 123456789           |
      | Bank ID       | 0800                |
      | Currency      | CZK                 |
      | YNAB Account  |                     |
      | Active        | Yes                 |
    And I click on "Create Account"
    Then a new source account should be created with a unique ID
    And I should be redirected to the account details page
    And I should see the message "Account created successfully"

  Scenario: Edit an existing source account
    Given a source account with the following details:
      | Field         | Value               |
      | Account Name  | Old Account Name    |
      | Account ID    | 123456789           |
      | Bank ID       | 0800                |
      | Currency      | CZK                 |
      | YNAB Account  |                     |
      | Active        | Yes                 |
    When I navigate to the source account details page
    And I click on "Edit"
    Then I should see the account edit form
    When I update the name to "New Account Name"
    And I click on "Update Account"
    Then the source account should be updated
    And I should be redirected to the account details page
    And I should see the name has been updated to "New Account Name"

  Scenario: Deactivate a source account
    Given an active source account exists
    When I navigate to the source account details page
    And I click on "Delete"
    And I confirm the deletion
    Then the source account should be marked as inactive
    And I should be redirected to the source accounts page
    And the account should no longer appear in the active accounts list

  Scenario: Link a source account to YNAB
    Given a source account exists
    And YNAB API is configured
    When I navigate to the source account edit page
    And I select a YNAB account from the dropdown
    And I save the changes
    Then the source account should be linked to the YNAB account
    And I should see the YNAB account ID in the account details