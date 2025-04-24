Feature: Transaction Import, Categorization, and Submission Workflow
  As a finance team member
  I want to automatically import, categorize and submit financial transactions
  So that I can maintain accurate financial records with minimal manual effort

  @domain
  Rule: Domain-level transaction processing workflow
    # These scenarios test pure business logic without external dependencies

    Background:
      Given a transaction processing system exists
      And a valid user account is available

    @domain
    Scenario: Transaction import workflow creates proper domain records
      When the import workflow receives 5 transaction records from a provider
      Then 5 transaction domain entities should be created
      And each transaction should have "Imported" status
      And an "ImportCompleted" domain event should be published with count 5

    @domain
    Scenario: Transaction categorization applies rules correctly
      Given 3 uncategorized transactions exist in the system
      When the categorization service processes the transactions
      Then each transaction should have a category assigned
      And each categorization should have a confidence score
      And transactions should have "Categorized" status
      And a "TransactionsCategorized" domain event should be published

    @domain
    Scenario: Manual category override updates transaction correctly
      Given a transaction with ID "tx-123" exists with category "Groceries"
      When a user updates the category to "Dining Out" for transaction "tx-123"
      Then the transaction "tx-123" should have category "Dining Out"
      And a "CategoryUpdated" domain event should be published
      And the event should contain transaction ID "tx-123"

    @domain
    Scenario: Bulk category update processes multiple transactions
      Given 5 transactions exist with category "Uncategorized"
      When a user updates the category to "Transportation" for transactions with description containing "UBER"
      Then all matching transactions should have category "Transportation"
      And a "BulkCategoryUpdated" domain event should be published
      And the event should contain the count of updated transactions

    @domain
    Scenario: Transaction submission workflow marks records as submitted
      Given 3 categorized transactions exist
      When the submission workflow processes these transactions
      Then each transaction should have "Submitted" status
      And a "TransactionsSubmitted" domain event should be published with count 3

    @domain
    Scenario: Duplicate transaction detection prevents reprocessing
      Given a transaction with external ID "fio-12345" has been processed
      When the import workflow receives a transaction with external ID "fio-12345"
      Then the transaction should be marked as duplicate
      And no new transaction entity should be created
      And a "DuplicateTransactionDetected" domain event should be published

    @domain
    Scenario: Transactions require categories before submission
      Given 3 transactions exist with "Imported" status
      And 1 transaction has no category assigned
      When the submission workflow attempts to process these transactions
      Then the submission should fail with error "All transactions must be categorized"
      And a "SubmissionFailed" domain event should be published
      And no transaction status should change to "Submitted"

    @domain
    Scenario: Transaction status statistics are calculated correctly
      Given the system contains 10 imported transactions
      And 7 transactions have been categorized
      And 5 transactions have been submitted
      When the statistics service calculates status metrics
      Then the following metrics should be reported:
        | metric           | value |
        | total            | 10    |
        | categorized      | 7     |
        | submitted        | 5     |

    @domain @performance
    Scenario: Transaction import and categorization meets performance requirements (NFR2)
      Given 100 transactions are available for import and categorization
      When the import and categorization process is initiated
      Then the entire process should complete within 60 seconds
      And all 100 transactions should be imported and categorized
      And meet the performance criteria specified in NFR2

  @ui
  Rule: User interface supports transaction management workflows

    Background:
      Given a user is logged in to the application
      And the transaction domain service contains sample data

    @ui
    Scenario: Dashboard displays transaction summary statistics
      When the user navigates to the dashboard
      Then the dashboard should display the following summary statistics:
        | statistic               | value |
        | Total Transactions      | 25    |
        | Categorized             | 15    |
        | Submitted               | 5     |
      And the dashboard should contain an "Import" button
      And the dashboard should display a transaction table with status indicators
      
    @ui
    Scenario: Dashboard statistics update dynamically
      Given the user is on the dashboard screen
      And the dashboard shows transaction statistics
      When the user completes an action that changes transaction status
      Then the dashboard statistics should update automatically
      And reflect the current state of transactions

    @ui
    Scenario: User can initiate a new transaction import with date range
      Given the user is on the dashboard screen
      When the user clicks the "Import" button
      Then an import dialog should appear
      And the dialog should contain "Start Date" and "End Date" fields
      And the dialog should contain an "Import" button
      
      When the user selects start date "2025-04-01" and end date "2025-04-15"
      And the user clicks the import dialog "Import" button
      Then the system should delegate to the domain import service with these dates
      And the user should see a loading indicator
      And the user should be notified when import is complete
      And the transaction table should update with new transactions

    @ui
    Scenario: Transaction list provides sorting and filtering
      Given the user is on the transaction management screen
      And 20 transactions of various types exist in the system
      When the user filters by status "Categorized"
      Then only transactions with "Categorized" status should be displayed
      
      When the user sorts by "Amount" in "Descending" order
      Then the transactions should be ordered from highest to lowest amount
      
      When the user searches for "Grocery"
      Then only transactions containing "Grocery" in the description should be displayed

    @ui
    Scenario: User can edit transaction category via dropdown
      Given the user is on the transaction management screen
      And a transaction with ID "tx-456" is displayed with category "Groceries"
      When the user clicks the category cell for transaction "tx-456"
      Then a dropdown of available categories should appear
      
      When the user selects "Dining Out" from the dropdown
      Then the system should delegate to the domain service to update the category
      And the displayed category should change to "Dining Out"
      And a success message should be displayed

    @ui
    Scenario: Bulk selection and submission of transactions
      Given the user is on the transaction management screen
      And 5 categorized transactions are displayed
      When the user selects the checkboxes for 3 transactions
      And the user clicks "Submit Selected to YNAB"
      Then the system should delegate to the domain submission service
      And the selected transactions should update to "Submitted" status
      And a success notification should show "3 transactions submitted to YNAB"

    @ui
    Scenario: Error messages are displayed for validation failures
      Given the user is on the transaction management screen
      And transactions with mixed statuses are displayed
      When the user selects transactions including some without categories
      And the user clicks "Submit Selected to YNAB"
      Then an error message should be displayed
      And the message should state "All selected transactions must be categorized"
      And no transactions should change status

    @ui @performance
    Scenario: User interface actions meet response time requirements
      Given the system contains transaction data
      When the user performs any UI action
      Then the action should complete within 2 seconds
      And maintain the response time requirements specified in Performance Considerations

  @integration
  Rule: System integrates correctly with external services

    Background:
      Given the system is configured with valid API credentials

    @integration
    Scenario: System connects successfully to Fio Bank API
      When the Fio Bank provider attempts to establish a connection
      Then the connection should succeed
      And the provider should authenticate successfully
      And the system should receive a valid session token

    @integration
    Scenario: Fio Bank transactions are retrieved and transformed correctly
      Given a Fio Bank API connection is established
      When the provider requests transactions from "2025-04-01" to "2025-04-15"
      Then the Fio Bank API should return transaction data
      And the provider should transform the data to the system's transaction model
      And each transaction should have the following fields populated:
        | field        | example                     |
        | id           | fio-trans-123               |
        | date         | 2025-04-10                  |
        | amount       | 42.50                       |
        | description  | PAYMENT TO GROCERY STORE    |
        | counterparty | Grocery Store Ltd           |
        | account_id   | fio-account-456             |

    @integration
    Scenario: AI service categorizes transactions with required accuracy
      Given 100 transactions with descriptions and known correct categories are available
      When the categorization provider processes these transactions
      Then at least 80 transactions should be assigned their correct category
      And each categorization should include a confidence score
      And the system should meet the 80% accuracy requirement specified in NFR1

    @integration
    Scenario: System handles Fio Bank API failures gracefully
      Given the Fio Bank API is unreachable
      When the provider attempts to retrieve transactions
      Then the provider should retry the connection 3 times
      And the system should record an integration error
      And users should be notified of the connection issue

    @integration
    Scenario: System connects successfully to YNAB API
      When the YNAB provider attempts to establish a connection
      Then the connection should succeed
      And the provider should authenticate successfully
      And the system should receive a valid YNAB token

    @integration
    Scenario: Transactions are submitted correctly to YNAB
      Given 3 transactions are ready for submission
      When the YNAB provider submits these transactions
      Then each transaction should be converted to YNAB format
      And the YNAB API should accept all transactions
      And each transaction should receive a YNAB transaction ID
      And the system should store these IDs with the transactions

    @integration
    Scenario: System handles YNAB API rate limiting
      Given the YNAB API returns a rate limit error
      When the YNAB provider attempts to submit transactions
      Then the provider should implement backoff strategy
      And retry the submission within the rate limits
      And all transactions should eventually be submitted

    @integration @performance
    Scenario: Import process meets throughput requirements
      Given transactions need to be imported from the transaction provider
      When the import process is running
      Then it should handle at least 10 transactions per second
      And meet the throughput requirements specified in Performance Considerations

  @e2e
  Rule: End-to-End workflows function correctly across the system

    Background:
      Given a user is logged in to the application
      And valid connections to Fio Bank and YNAB are configured

    @e2e
    Scenario: Complete transaction import-categorize-submit workflow
      When the user imports transactions from Fio Bank for period "2025-04-01" to "2025-04-15"
      Then the system should retrieve transactions from Fio Bank
      And save them to the database
      And display them in the transaction table
      
      When the user requests automated categorization
      Then the system should categorize the transactions using the AI service
      And update the transaction display with categories
      
      When the user reviews and approves the categories
      And selects all transactions
      And clicks "Submit to YNAB"
      Then the system should submit the transactions to YNAB
      And display a success confirmation
      And update transaction status to "Submitted"

    @e2e
    Scenario: User modifies categories and submits transactions
      Given transactions have been imported from Fio Bank
      And the system has automatically categorized them
      When the user changes the category of transaction "tx-789" from "Groceries" to "Household"
      And the user changes the category of transaction "tx-790" from "Entertainment" to "Dining Out"
      And the user selects both modified transactions
      And clicks "Submit to YNAB"
      Then the system should submit both transactions to YNAB with the updated categories
      And YNAB should record the transactions with the correct categories
      And the transactions should be marked as "Submitted" in the system

    @e2e
    Scenario: System prevents duplicate transaction submission
      Given transactions have been imported and submitted to YNAB
      When the user attempts to submit the same transactions again
      Then the system should detect the duplicate submission attempt
      And prevent the submission
      And display a warning message
      And maintain the "Submitted" status for those transactions

    @e2e @performance
    Scenario: Complete import-categorize-submit workflow meets all performance criteria
      Given external systems contain transactions for the test period
      When the user performs a complete import-categorize-submit workflow
      Then each step should meet its individual performance requirement:
        | step         | requirement                                   |
        | import       | Handle at least 10 transactions per second    |
        | categorize   | Process 100 transactions within 60 seconds    |
        | UI responses | Complete all actions within 2 seconds         |
      And the system should maintain responsiveness throughout the process

    @e2e
    Scenario: System handles network interruption during workflow
      Given the user has started an import process
      When the network connection is temporarily interrupted
      Then the system should detect the connection failure
      And retry the operation when the connection is restored
      And notify the user about the connection issue
      And complete the process once connectivity is reestablished