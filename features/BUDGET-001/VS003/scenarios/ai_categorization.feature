# Vertical Slice: AI-Powered Categorization (VS-003)
# Business Value: Automatically categorizes transactions using AI, significantly reducing manual effort
# UI Components:
# - CategoryizationControl
# - CategoryConfidenceIndicator
# - BatchProcessingStatus
# Dependencies:
# - Requires "Transaction Import from Fio Bank" slice for transaction data
# - OpenAI API integration

@slice:ai-categorization @value:high @phase:1
Feature: AI-Powered Transaction Categorization
  As a finance team member
  I want transactions to be automatically categorized using AI
  So that I don't have to manually assign categories to each transaction

  Background:
    Given I am logged in as a finance team member
    And there are uncategorized transactions in the system

  # UI Component: CategoryizationControl
  @ui-prototype
  Scenario: Validate categorization control UI prototype
    Given I am presented with the categorization control prototype
    When I interact with the control
    Then I should see a "Run AI Categorization" button that is enabled
    And I should see an option to select specific transactions for categorization
    And I should see a confidence threshold slider with presets for "Strict", "Balanced", and "Lenient"
    And I should see an indicator of how many transactions will be processed

  # UI Component: CategoryConfidenceIndicator
  @ui-prototype
  Scenario: Validate category confidence indicator prototype
    Given I am presented with the category confidence indicator prototype
    When I interact with different confidence levels
    Then transactions with >90% confidence should show a green indicator
    And transactions with 70-90% confidence should show a yellow indicator
    And transactions with <70% confidence should show a red indicator
    And hovering over the indicator should show the exact confidence percentage
    And clicking the indicator should show alternative category suggestions

  # User Flow: Batch AI Categorization
  @user-flow
  Scenario: Run AI categorization on all uncategorized transactions
    Given I have 15 uncategorized transactions imported from Fio Bank
    When I click the "Run AI Categorization" button
    Then I should see a progress indicator showing "Analyzing transactions"
    And I should see the number of processed transactions updating in real time
    And when complete, I should see a summary showing "15 transactions categorized"
    And at least 12 of the transactions should have confidence level above 70%
    And transactions should be sorted with lowest confidence first to facilitate review
    And each transaction should show its specific confidence score

  # User Flow: Categorize Selected Transactions
  @user-flow
  Scenario: Run AI categorization on selected transactions only
    Given I have 20 uncategorized transactions in the system
    When I select 5 specific transactions
    And I click the "Run AI Categorization" button
    Then only the 5 selected transactions should be processed
    And the other 15 transactions should remain unchanged
    And I should see a summary showing "5 transactions categorized"
    And the UI should clearly highlight which transactions were categorized

  # Edge Case: API Rate Limiting
  @edge-case
  Scenario: Handle OpenAI API rate limiting
    Given I have 100 uncategorized transactions in the system
    And the OpenAI API has a rate limit that will be exceeded
    When I click the "Run AI Categorization" button
    Then the system should process transactions in batches to respect API limits
    And I should see a message "Processing in batches to respect API limits"
    And all transactions should eventually be categorized
    And I should see progress updates throughout the process

  # Domain Concept: Learning from Corrections
  @domain-discovery
  Scenario: System learns from manual category corrections
    Given I have previously corrected 5 AI-categorized transactions
    When I import 10 new similar transactions
    And run AI categorization on them
    Then the system should use my previous corrections to improve categorization
    And the similar transactions should be categorized according to my corrections
    And the confidence score for these transactions should be higher
    And I should see an indication that "Personalized categorization applied" for affected transactions
