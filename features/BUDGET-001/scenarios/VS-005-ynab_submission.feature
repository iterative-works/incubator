# Vertical Slice: YNAB Submission (VS-005)
# Business Value: Completes the automation workflow by sending categorized transactions to YNAB
# UI Components:
# - SubmissionControl
# - AccountMappingSelector
# - SubmissionResultsView
# Dependencies:
# - Requires "Category Review & Modification" slice
# - YNAB API integration

@slice:ynab-submission @value:high @phase:1
Feature: Submit Transactions to YNAB
  As a finance team member
  I want to submit properly categorized transactions to YNAB
  So that my budget data is updated automatically and accurately

  Background:
    Given I am logged in as a finance team member
    And there are categorized transactions ready for submission
    And the system is configured with valid YNAB API credentials

  # UI Component: SubmissionControl
  @ui-prototype
  Scenario: Validate submission control UI prototype
    Given I am presented with the submission control prototype
    When I interact with the control
    Then I should see a "Submit to YNAB" button that is enabled when transactions are selected
    And I should see a selection counter showing how many transactions are selected
    And I should see an account mapping dropdown if multiple YNAB accounts are available
    And I should see a "Remember this mapping" checkbox for account selection

  # UI Component: SubmissionResultsView
  @ui-prototype
  Scenario: Validate submission results view prototype
    Given I am presented with the submission results view prototype
    When I interact with the results view after a submission
    Then I should see a summary count of successfully submitted transactions
    And I should see details for any failed submissions with error messages
    And I should see a link to view the transactions directly in YNAB
    And I should see options to retry failed submissions or return to the transaction list

  # User Flow: Basic Submission Success
  @user-flow
  Scenario: Successfully submit transactions to YNAB
    Given I am viewing the transaction list with categorized transactions
    When I select 5 transactions with "Categorized" status
    And I select "Checking Account" from the YNAB account dropdown
    And I click the "Submit to YNAB" button
    Then I should see a progress indicator with status "Connecting to YNAB"
    And then the status should change to "Submitting transactions"
    And finally I should see a summary showing "5 transactions successfully submitted to YNAB"
    And the transactions should be updated with "Submitted" status
    And I should see a link to "View in YNAB" that opens the YNAB web app

  # User Flow: Partial Submission Success
  @user-flow
  Scenario: Handle partial submission success
    Given I am viewing the transaction list with categorized transactions
    When I select 8 transactions with "Categorized" status
    And I click the "Submit to YNAB" button
    And 2 of the transactions fail to submit due to YNAB validation errors
    Then I should see a summary showing "6 transactions submitted, 2 transactions failed"
    And the 6 successful transactions should be updated with "Submitted" status
    And the 2 failed transactions should remain with "Categorized" status
    And I should see error details for each failed transaction
    And I should see a "Retry Failed" button to attempt resubmission

  # Edge Case: YNAB API Connection Failure
  @edge-case
  Scenario: Handle YNAB API connection failure
    Given I am viewing the transaction list with categorized transactions
    And the YNAB API is temporarily unavailable
    When I select 5 transactions with "Categorized" status
    And I click the "Submit to YNAB" button
    Then I should see an error message "Unable to connect to YNAB. Please try again later."
    And I should see a "Retry" button
    And the transactions should remain with "Categorized" status
    And a system notification should be generated for administrators

  # Domain Concept: YNAB Account Mapping
  @domain-discovery
  Scenario: Verify YNAB account mapping persistence
    Given I have Fio Bank account "Personal Checking" mapped to YNAB account "Checking Account"
    When I import and categorize transactions from "Personal Checking"
    And I select these transactions for submission
    Then the system should automatically select "Checking Account" as the target YNAB account
    And when I submit the transactions
    Then they should be properly associated with "Checking Account" in YNAB
    And the mapping should be saved for future submissions
