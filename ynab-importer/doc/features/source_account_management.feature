Feature: Source Account Management
  As a user of the YNAB importer
  I want to manage my bank source accounts
  So that I can import transactions from different bank accounts and map them to YNAB accounts

  Background:
    Given I am logged into the application
    And I navigate to the source accounts page

  Scenario: View list of source accounts
    When I access the source accounts page
    Then I should see a list of all configured source accounts
    And each account should display its name, account ID, bank ID, and status
    And I should see a button to add a new account

  Scenario: Add a new source account
    When I click the "Add New Account" button
    Then I should be directed to the account creation form
    When I fill in the following details:
      | Field           | Value      |
      | Account Name    | My Checking|
      | Account ID      | 12345678   |
      | Bank ID         | 0800       |
      | Currency        | CZK        |
      | YNAB Account ID | ynab-acct-1|
      | Active          | yes        |
    And I click "Create Account"
    Then I should be redirected to the source accounts list
    And I should see the new account in the list

  Scenario: View source account details
    Given at least one source account exists
    When I click on the name of a source account
    Then I should see the account details page
    And I should see the following information:
      | Field           | Description                 |
      | ID              | The account's system ID     |
      | Account Name    | User-defined account name   |
      | Account ID      | Bank account number         |
      | Bank ID         | Bank identification code    |
      | Currency        | Account currency code       |
      | YNAB Account ID | Linked YNAB account (if any)|
      | Status          | Whether account is active   |
      | Last Sync       | Time of last synchronization|
    And I should see buttons to edit or delete the account

  Scenario: Edit a source account
    Given at least one source account exists
    When I click on the name of a source account
    And I click the "Edit" button
    Then I should see the account edit form with pre-filled values
    When I change the account name to "Updated Account Name"
    And I click "Update Account"
    Then I should be redirected to the account details page
    And I should see the updated account name

  Scenario: Deactivate a source account
    Given at least one source account exists
    When I click on the name of a source account
    And I click the "Edit" button
    And I uncheck the "Active" checkbox
    And I click "Update Account"
    Then I should be redirected to the account details page
    And I should see the account status is "Inactive"

  Scenario: Filter source accounts list
    Given multiple source accounts exist with different statuses
    When I select "Active" from the status filter dropdown
    Then I should only see accounts with "Active" status
    When I select "Inactive" from the status filter dropdown
    Then I should only see accounts with "Inactive" status
    When I select "All" from the status filter dropdown
    Then I should see all accounts regardless of status

  Scenario: Search for a source account
    Given multiple source accounts exist
    When I enter a search term in the search box
    Then I should only see accounts that match the search term in their name or account ID

  Scenario: Handle validation errors when creating an account
    When I click the "Add New Account" button
    And I leave required fields empty
    And I click "Create Account"
    Then I should see validation error messages for the empty fields
    And I should remain on the account creation form

  Scenario: Delete a source account
    Given at least one source account exists
    When I click on the name of a source account
    And I click the "Delete" button
    Then I should see a confirmation dialog
    When I confirm the deletion
    Then I should be redirected to the source accounts list
    And the deleted account should no longer appear in the list