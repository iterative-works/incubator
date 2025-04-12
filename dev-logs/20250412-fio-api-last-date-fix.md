# 2025-04-12 Developer Log

## Fixing Fio API Authentication Issue in Integration Tests

### Problem Description

We encountered an issue with the Fio Bank API integration tests where the test "Fio client should fetch new transactions using last endpoint" was failing with a 422 error:

```
Exception in thread "zio-fiber-1188" works.iterative.incubator.fio.domain.model.error.FioNetworkError: Network error: Unexpected response from Fio API: 422 - Data není možné poskytnout bez silné autorizace.
```

The error message translates to: "Data cannot be provided without strong authorization."

After investigating the Fio Bank API documentation, we determined that this error occurs because:

1. When using the `/last/` endpoint with a token that has no bookmark ("zarážka") set, the Fio API attempts to retrieve all available transactions from the account's entire history.
2. According to the Fio API documentation (section 3.1), retrieving data older than 90 days requires special authorization in the internet banking interface.
3. This special authorization is not practical to perform during automated testing.

### Solution

We implemented the following solution:

1. Added a `setLastDate` method to the `FioClient` trait and its implementation in `FioClientLive`:
   ```scala
   def setLastDate(token: String, date: LocalDate): Task[Unit]
   ```

2. This method uses the Fio API endpoint `/set-last-date/{token}/{date}/` to set a bookmark ("zarážka") to a specific date.

3. Updated both the direct client test and the import service test to set a bookmark to 90 days ago before attempting to fetch new transactions.

4. With this approach, when the `/last/` endpoint is called, it only attempts to retrieve transactions from the last 90 days, which doesn't require special authorization.

### Implementation Details

1. Added the `setLastDate` method to the `FioClient` trait with appropriate documentation.

2. Implemented the method in `FioClientLive` to make a call to the Fio API's `/set-last-date/` endpoint.

3. Updated the integration tests to set a bookmark date before calling the `/last/` endpoint.

### Benefits

1. **No Manual Authorization Required**: The tests can now run without requiring manual intervention in the internet banking interface.

2. **More Robust Testing**: We avoid sporadic failures that would occur when attempting to access historical data.

3. **Better Documentation**: The solution is now documented in both the code and this dev log, making it clear why this approach is necessary.

### Notes for Future Development

- If we need to test retrieval of transactions older than 90 days, we'll need to implement a different approach or handle the authorization requirement differently.
- The setLastDate method could potentially be useful in production code to prevent performance issues when fetching large amounts of historical data.