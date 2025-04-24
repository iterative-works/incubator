@financial-integration @ynab @submission
Feature: YNAB Transaction Submission
  As a finance team member
  I want to submit categorized transactions to YNAB
  So that my budget tool has accurate and up-to-date financial data

  Background:
    # Note: Authentication is deferred to a future iteration
    # Given I am logged in as an administrator
    Given the system is connected to YNAB API

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