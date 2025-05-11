# Task 18: Implement Fio Bank Infrastructure Adapter

## Component Type
Infrastructure Adapter

## Component Name
`FioBankTransactionService`

## Package Location
`works.iterative.incubator.budget.infrastructure.adapter.fio`

## Purpose
Create an infrastructure adapter that connects to the Fio Bank API, fetches transactions for a given date range, and transforms them into our domain model. This component will implement the `BankTransactionService` interface and provide Fio Bank-specific functionality.

## Key Behaviors
1. Validate date ranges according to Fio Bank constraints (maximum 90 days)
2. Connect to the Fio Bank API using proper authentication
3. Fetch transactions for a specified date range
4. Transform Fio Bank API responses into domain `Transaction` models
5. Handle API errors and transform them into domain-specific errors
6. Support secure token storage and management for account-specific tokens

## Dependencies
1. `BankTransactionService` interface (domain service)
2. `Transaction` domain model
3. `AccountId` domain model
4. `ImportBatchId` domain model
5. `TransactionImportError` error model
6. STTP library for HTTP requests
7. zio-json library for JSON processing

## Acceptance Criteria
1. Successfully validates date ranges according to Fio Bank rules (max 90 days)
2. Properly connects to Fio Bank API with authentication
3. Correctly translates Fio Bank transaction data to domain model
4. Handles and appropriately transforms all API errors
5. Securely manages account-specific API tokens
6. Follows functional programming principles with ZIO effect management
7. Includes proper logging and error reporting

## Implementation Guide
Follow the Ports and Adapters pattern with a focus on:
1. Clean separation between API client (technical concerns) and service adapter (domain translation)
2. Pure functional approach with explicit error handling via ZIO effects
3. Secure handling of sensitive credentials with account-specific tokens
4. Thorough error mapping from API-level to domain-level errors

## Reference Implementation
We have existing Fio client code that can be leveraged for this implementation:

1. **FioClient**: https://github.com/iterative-works/incubator/blob/7b75cb102a1e9e254be374c820c717048bb699f5/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/client/FioClient.scala
   - Contains the core API interaction logic
   - Implements methods for fetching transactions by date range and for new transactions
   - Uses STTP for HTTP requests
   - Includes error handling for API interactions

2. **FioCodecs**: https://github.com/iterative-works/incubator/blob/7b75cb102a1e9e254be374c820c717048bb699f5/bounded-contexts/fio/src/main/scala/works/iterative/incubator/fio/infrastructure/client/FioCodecs.scala
   - Contains the JSON decoders using zio-json
   - Defines the data model for API responses
   - Handles various value types in the transaction data

## Relevant Scenarios
This component supports these scenarios from the feature file:
```gherkin
Scenario: Successfully import transactions for a date range
  Given I am on the transaction import page
  When I select "2025-04-01" as the start date
  And I select "2025-04-15" as the end date
  And I click the "Import Transactions" button
  Then I should see a progress indicator with status "Connecting to Fio Bank"
  And then the status should change to "Retrieving transactions"
  And then the status should change to "Storing transactions"
  And finally I should see a summary showing "15 transactions successfully imported"
  And the transactions should appear in the transaction list with "Imported" status
  And the import history should be updated with this import session

Scenario: Import with no transactions available
  Given I am on the transaction import page
  When I select "2025-06-01" as the start date
  And I select "2025-06-02" as the end date
  And I click the "Import Transactions" button
  Then the system should connect to Fio Bank API
  And after the import completes, I should see a message "No transactions found for the selected date range"
  And the import history should be updated with this import session marked as "No transactions"

Scenario: Handle Fio Bank API connection failure
  Given I am on the transaction import page
  And the Fio Bank API is temporarily unavailable
  When I select "2025-04-01" as the start date
  And I select "2025-04-15" as the end date
  And I click the "Import Transactions" button
  Then I should see an error message "Unable to connect to Fio Bank. Please try again later."
  And I should see a "Retry" button
  And the import should not be recorded in the import history
```

## Technical Details
### Token Storage
1. **Account-specific Token Management**:
   - Store tokens in a separate `fio_account` table with a relationship to source accounts
   - Database schema should include:
     ```sql
     CREATE TABLE fio_account (
         id BIGSERIAL PRIMARY KEY,
         source_account_id BIGINT NOT NULL REFERENCES source_account(id),
         token VARCHAR(100) NOT NULL,
         last_sync_time TIMESTAMP,
         last_fetched_id BIGINT,
         UNIQUE (source_account_id)
     );
     ```
   - Implement token encryption using AES-256
   - Create a `FioTokenManager` service for secure token access and caching

2. **Token Usage in Service**:
   - Modify the `FioTransactionService` to fetch the appropriate token for the given account ID
   - Keep token retrieval and management within the Fio adapter package
   - Include audit logging for all token access operations

### API Endpoints
Based on the existing FioClient implementation and Fio Bank API v1.0.0:

1. Date Range Endpoint: `/periods/${token}/${dateFrom}/${dateTo}/transactions.json`
   - For retrieving transactions within a specific date range
   - Example URL: `https://fioapi.fio.cz/v1/rest/periods/TOKEN/2025-04-01/2025-04-15/transactions.json`
   - Date format must be YYYY-MM-DD

2. New Transactions Endpoint: `/last/${token}/transactions.json`
   - For retrieving all new transactions since the last fetch
   - Example URL: `https://fioapi.fio.cz/v1/rest/last/TOKEN/transactions.json`
   - The API automatically handles tracking the last seen transaction

3. Set Last Date Endpoint: `/set-last-date/${token}/${date}/`
   - For setting a bookmark date for future transaction fetching
   - Example URL: `https://fioapi.fio.cz/v1/rest/set-last-date/TOKEN/2025-04-15/`
   - Useful for forcing the API to only return transactions after a certain date

### API Error Handling

The Fio API can return various HTTP status codes that need to be handled:

1. `200 OK` - Successful response
2. `409 Conflict` - Rate limiting error (too many requests)
3. `400 Bad Request` - Invalid parameters
4. `401 Unauthorized` - Invalid token
5. `500 Internal Server Error` - Server-side error

Rate limiting is particularly important - the API allows only 1 request per 30 seconds per token. Our implementation must include appropriate retry mechanisms with backoff.

### API Response Structure
```json
{
  "accountStatement": {
    "info": {
      "accountId": "2200000001",
      "bankId": "2010",
      "currency": "CZK",
      "iban": "CZ5420100000002200000001",
      "bic": "FIOBCZPPXXX",
      "openingBalance": 257.69,
      "closingBalance": 47257.69,
      "dateStart": "2025-03-14+0100",
      "dateEnd": "2025-03-15+0100"
    },
    "transactionList": {
      "transaction": [
        {
          "column22": { "value": 26962199069, "name": "ID pohybu", "id": 22 },
          "column0": { "value": "2025-03-14+0100", "name": "Datum", "id": 0 },
          "column1": { "value": 50000.0, "name": "Objem", "id": 1 },
          "column14": { "value": "CZK", "name": "Měna", "id": 14 },
          "column2": { "value": "2800000002", "name": "Protiúčet", "id": 2 },
          "column10": { "value": "Novák, Jan", "name": "Název protiúčtu", "id": 10 },
          "column3": { "value": "2010", "name": "Kód banky", "id": 3 },
          "column12": { "value": "Fio banka, a.s.", "name": "Název banky", "id": 12 },
          "column8": { "value": "Příjem převodem uvnitř banky", "name": "Typ", "id": 8 }
        }
      ]
    }
  }
}
```

### Column Mapping
The following are the fields from the Fio Bank API that need to be mapped to our domain model:

| Column ID | Fio Field Name | Data Type | Maps To Domain Field | Notes |
|-----------|----------------|-----------|----------------------|-------|
| 0 | Datum | String | Transaction.date | Format: "YYYY-MM-DD+HHMM", requires parsing |
| 1 | Objem | Number | Transaction.amount | Transaction amount (can be positive or negative) |
| 2 | Protiúčet | String | Transaction.counterAccount | Counter account number (may be empty) |
| 3 | Kód banky | String | Part of Transaction.counterAccount | Bank code to append to counter account |
| 4 | KS | String | Transaction.reference | Constant symbol |
| 5 | VS | String | Transaction.reference | Variable symbol |
| 6 | SS | String | Transaction.reference | Specific symbol |
| 7 | Uživatelská identifikace | String | - | User identification (optional) |
| 8 | Typ | String | Part of Transaction.description | Transaction type description |
| 10 | Název protiúčtu | String | Transaction.counterparty | Counter account name |
| 12 | Název banky | String | - | Bank name (optional) |
| 14 | Měna | String | Transaction.amount.currency | Currency code (e.g., "CZK") |
| 17 | ID pokynu | Number | - | Instruction ID (optional) |
| 22 | ID pohybu | Number | Used to generate Transaction.id | Unique transaction identifier |
| 25 | Komentář | String | Part of Transaction.description | Comment/memo field (optional) |
| 26 | BIC | String | - | BIC/SWIFT code (optional) |

Notes:
- Some columns may be missing for certain transaction types
- The implementation should handle null/empty values appropriately
- Multiple reference fields (KS, VS, SS) might need to be combined
- The transaction description should typically combine columns 8 (type) and 25 (comment)
- Date values need to be parsed from the format with timezone offset

## Implementation Structure
The implementation should consist of the following components:

1. **FioApiClient** - Low-level HTTP client for Fio Bank API
   - Reuse logic from existing `FioClient` implementation
   - Handles HTTP requests and authentication
   - Parses JSON responses using zio-json
   - Maps API errors to client-specific errors

2. **FioTokenManager** - Service for secure token handling
   - Retrieves tokens by account ID
   - Handles token encryption/decryption
   - Provides caching to minimize database lookups
   - Logs access for security audit purposes

3. **FioAccount** - Entity for storing account-specific data
   - Holds information about a Fio Bank account
   - Contains encrypted token
   - Tracks synchronization state

4. **FioAccountRepository** - Repository for FioAccount entities
   - Saves and retrieves FioAccount records
   - Implements database encryption logic

5. **FioMappers** - Utility for mapping between Fio model and domain model
   - Adapt data models from existing `FioCodecs` implementation
   - Maps Fio transaction format to domain Transaction model
   - Handles currency conversion and date formatting
   - Processes different column data types (String, Number, Date)
   - Handles potentially missing or null column values
   - Combines multiple fields for references and descriptions

6. **FioBankTransactionService** - Adapter implementing BankTransactionService
   - Uses FioApiClient to fetch data
   - Uses FioTokenManager to retrieve tokens
   - Uses FioMappers to convert data
   - Implements date range validation logic
   - Maps technical errors to domain errors

7. **FioConfig** - Configuration for Fio Bank integration
   - API endpoint configuration
   - Timeout settings
   - Retry policies
   - Security settings

8. **FioBankTransactionServiceLive** - Live implementation
   - Concrete implementation with all dependencies
   - ZLayer for providing the service

## Security Considerations
1. Secure token storage with encryption
2. No hardcoded credentials in code
3. Account-specific token management
4. Detailed audit logging for API access
5. Proper error handling to avoid exposing sensitive information

## Test Plan
1. Unit tests for FioApiClient with mocked HTTP responses
   - Test different API endpoints
   - Test error handling for different HTTP response codes
   - Test rate limiting behavior
   - Test timeout handling

2. Unit tests for FioTokenManager with mock encryption
   - Test token retrieval and caching
   - Test encryption/decryption functionality
   - Test error handling for missing tokens
   - Test audit logging functionality

3. Unit tests for FioMappers with example JSON data
   - Test mapping with complete transaction data
   - Test mapping with missing optional fields
   - Test date parsing with different formats and timezones
   - Test reference field combination
   - Test description field generation

4. Unit tests for FioBankTransactionService with mocked client
   - Test successful transaction import
   - Test empty transaction list handling
   - Test error propagation
   - Test date range validation
   - Test proper error transformation to domain errors

5. Integration tests with the real Fio Bank API (conditionally run when credentials are available)
   - Test with real API responses
   - Test with real tokens
   - Test real data mapping to domain models
   - Verify rate limiting handling

## Estimated Effort
- 1 day for initial implementation
- 0.5 day for token management implementation
- 0.5 day for unit tests
- 0.5 day for integration with existing components

## Next Steps After Implementation
1. Create database repositories for transaction storage
2. Implement account management UI for storing and managing tokens
3. Integrate with the transaction import UI components
4. Implement error handling in the UI
5. Set up proper logging and monitoring