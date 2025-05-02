# Vertical Slice: Duplicate Prevention (VS-006)
# Business Value: Ensures data integrity by preventing duplicate transactions in YNAB
# UI Components:
# - DuplicateWarningDialog
# - SubmissionValidationIndicator
# - DuplicateMatchingRules
# Dependencies:
# - Requires "YNAB Submission" slice

@slice:duplicate-prevention @value:high @phase:2
Feature: Duplicate Transaction Prevention
  As a finance team member
  I want the system to prevent duplicate submissions to YNAB
  So that my budget data remains accurate without manual checking

  Background:
    Given I am logged in as a finance team member
    And there are transactions in various states in the system

  # UI Component: DuplicateWarningDialog
  @ui-prototype
  Scenario: Validate duplicate warning dialog UI prototype
    Given I am presented with the duplicate warning dialog prototype
    When I interact with the dialog
    Then I should see a clear warning message about potential duplicates
    And I should see a table comparing the current and existing transactions
    And I should see options to "Cancel," "Submit Anyway," or "Skip Duplicates"
    And I should see a checkbox to "Remember this choice" for future submissions

  # UI Component: SubmissionValidationIndicator
  @ui-prototype
  Scenario: Validate submission validation indicator prototype
    Given I am presented with the submission validation indicator prototype
    When I select transactions for submission
    Then each transaction should show a validation status icon
    And transactions with potential duplicates should show a warning icon
    And transactions with validation errors should show an error icon
    And transactions that are ready for submission should show a ready icon
    And hovering over any icon should show detailed status information

  # User Flow: Prevent Simple Duplicates
  @user-flow
  Scenario: Prevent submission of exact duplicate transactions
    Given I have previously submitted transactions to YNAB
    And those transactions are still in the system with "Submitted" status
    When I select these same transactions again
    And I click "Submit to YNAB"
    Then the system should identify them as exact duplicates
    And show a warning dialog with details of the duplication
    And the "Submit" button should be disabled by default
    And I should see an option to "Skip All Duplicates"
    When I click "Skip All Duplicates"
    Then no transactions should be submitted
    And I should see a message "Submission canceled - all transactions were duplicates"

  # User Flow: Handle Potential Duplicates
  @user-flow
  Scenario: Detect and handle potential duplicate transactions
    Given I have imported new transactions that are similar but not identical to previously submitted ones
    When I select these transactions for submission
    And I click "Submit to YNAB"
    Then the system should identify potential duplicates based on date, amount, and merchant
    And show a warning dialog with comparison details
    And I should see options to "Review Each" or "Submit All Anyway"
    When I click "Review Each"
    Then I should be presented with each potential duplicate for individual review
    And I can choose "Skip" or "Submit" for each one
    And after reviewing all, only the approved transactions should be submitted to YNAB

  # Edge Case: Same-Day Similar Transactions
  @edge-case
  Scenario: Correctly handle legitimate same-day similar transactions
    Given I have multiple legitimate transactions from the same merchant on the same day
    When I select these transactions for submission
    And I click "Submit to YNAB"
    Then the system should flag them as potential duplicates
    But should provide context that they are likely legitimate transactions
    And should show subtle differences like timestamps or reference numbers
    And should provide an option to "Submit All With Confirmation"
    When I select this option
    Then all transactions should be submitted to YNAB
    And the system should remember this pattern to improve future duplicate detection

  # Domain Concept: Duplicate Detection Rules
  @domain-discovery
  Scenario: Customize duplicate detection sensitivity
    Given I am in the system settings area
    When I access the "Duplicate Detection" settings
    Then I should see rule configuration options for what constitutes a duplicate
    And I should see options for "Strict," "Standard," and "Relaxed" detection
    And I should be able to customize the weight given to date, amount, and description matching
    And I should be able to set a time window for considering potential duplicates
    And I can test these settings with sample transactions to see how they would be evaluated
