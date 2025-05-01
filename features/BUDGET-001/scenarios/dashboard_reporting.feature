# Vertical Slice: Dashboard & Reporting
# Business Value: Provides visibility into system activity and performance metrics
# UI Components:
# - DashboardSummaryCards
# - ActivityTimeline
# - StatisticsCharts
# - PerformanceMetrics
# Dependencies:
# - Requires data from all previous slices

@slice:dashboard-reporting @value:medium
Feature: Dashboard & Reporting
  As a finance team member or administrator
  I want to see an overview of system activity and performance
  So that I can track progress and identify any issues requiring attention

  Background:
    Given I am logged in as a finance team member or administrator
    And the system has processed transactions

  # UI Component: DashboardSummaryCards
  @ui-prototype
  Scenario: Validate dashboard summary cards UI prototype
    Given I am presented with the dashboard summary cards prototype
    When I interact with the summary cards
    Then I should see counts for imported transactions
    And I should see counts for categorized transactions
    And I should see counts for submitted transactions
    And I should see the AI categorization accuracy percentage

  # UI Component: ActivityTimeline
  @ui-prototype
  Scenario: Validate activity timeline UI prototype
    Given I am presented with the activity timeline prototype
    When I interact with the timeline
    Then I should see recent import activities with timestamps
    And I should see recent categorization activities with timestamps
    And I should see recent submission activities with timestamps
    And I should be able to filter activities by type

  # User Flow: View Dashboard Metrics
  @user-flow
  Scenario: View accurate dashboard metrics after system activity
    Given I have completed the following activities:
      | Activity                   | Details                        |
      | Imported transactions      | 20 transactions from Fio Bank  |
      | Run AI categorization      | 20 transactions categorized    |
      | Manually updated categories| 3 transactions recategorized   |
      | Submitted to YNAB          | 15 transactions submitted      |
    When I navigate to the dashboard
    Then I should see "20 Transactions Imported" on the import card
    And I should see "20 Transactions Categorized" on the categorization card
    And I should see "15 Transactions Submitted" on the submission card
    And I should see "85% AI Accuracy" based on manual corrections
    And I should see all these activities listed in the timeline

  # Domain Concept: Performance Metrics Calculation
  @domain-discovery
  Scenario: Verify performance metrics calculation
    Given the system has processed transactions over a period of time
    When the performance metrics are calculated
    Then the average import time should be correctly displayed
    And the average categorization accuracy should be correctly calculated
    And the percentage of successful submissions should be correctly calculated
    And the metrics should update automatically when new data is processed
