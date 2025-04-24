@financial-integration @shared-kernel
Feature: Transaction Processing Core
  As a finance team member
  I want the system to track transaction states properly
  So that I can understand and manage the transaction lifecycle

  Background:
    # Note: Authentication is deferred to a future iteration
    # Given I am logged in as an administrator

  @core @state-management
  Scenario: Transaction state transition from Import to Categorization
    Given transactions have been freshly imported with "Imported" status
    When the categorization process is triggered
    Then the transaction processing state should change to "In Categorization"
    And after categorization completes successfully
    Then the transaction processing state should change to "Categorized"

  @core @error-handling
  Scenario: Recover from failed processing state
    Given a transaction is in "Failed" state
    When I select the transaction and click "Retry Processing"
    Then the system should attempt to process the transaction again
    And if successful, the transaction should move to the appropriate next state
    
  @core @audit
  Scenario: Maintain complete transaction audit log
    Given a transaction has been imported
    When the transaction goes through categorization and submission
    Then the system should record each state change in the audit log
    And the audit log should include timestamps and user information when applicable
    And I should be able to view the complete history of the transaction