# FIO API Endpoint Fix

## Problem

We discovered that our code was using an incorrect approach to fetch new transactions from the Fio Bank API. The code was using a `lastId` parameter which doesn't align with the actual API endpoint provided by Fio Bank.

Upon reviewing the Fio Bank API documentation (`API_Bankovnictvi.txt`), we found that the correct endpoint for fetching new transactions is:

```
https://fioapi.fio.cz/v1/rest/last/{token}/transactions.{format}
```

This endpoint automatically returns all transactions that have not been fetched yet with the given token, without requiring a `lastId` parameter.

## Changes Implemented

1. Updated the `FioClient` interface to remove the `lastId` parameter from `fetchNewTransactions`:
   ```scala
   def fetchNewTransactions(token: String): Task[FioResponse]
   ```

2. Updated the `FioClientLive` implementation to use the correct endpoint:
   ```scala
   val url = Uri.parse(s"$baseUrl/last/$token/transactions.json")
   ```

3. Updated the `FioImportService` interface to remove the `lastId` parameter from `importNewTransactionsWithToken`:
   ```scala
   def importNewTransactionsWithToken(
       token: String,
       sourceAccountId: Long
   ): Task[Int]
   ```

4. Updated the `FioTransactionImportService` implementation to match the new interface and remove all related `lastId` logic.

5. Updated all tests and mocks to reflect these changes.

6. Added integration tests specifically for testing the `/last` endpoint functionality.

## Benefits

- Aligns our code with the actual Fio Bank API
- Simplifies the transaction import flow by removing unnecessary parameters
- Ensures correct fetching of new transactions without having to track the last transaction ID manually

## Testing

The changes were thoroughly tested through:
- Unit tests for the `FioClient` and `FioTransactionImportService`
- Integration tests with the live Fio Bank API

The CLIs and import services function correctly with these changes, fetching new transactions as expected.