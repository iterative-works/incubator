# YNAB Test Implementation Challenges - 2025-04-09

## Overview

During our implementation of tests for the YNAB API integration, we encountered several challenges that prevented us from achieving complete test coverage. This document outlines these challenges as a reference for future work.

## Test Structure

We created three types of tests:

1. **YnabServiceSpec** - Testing the delegation from the service layer to the client
2. **YnabClientSpec** - Testing the HTTP client with stubbed responses
3. **YnabIntegrationSpec** - Optional tests that run against the real YNAB API when configured

## What Worked

The service tests were successful. `YnabServiceSpec` properly verified:
- The service layer correctly delegates to the client
- The budget-specific service passes the correct budget ID to client calls
- All service methods are properly implemented

## Challenges Encountered

### 1. HTTP Client Stubbing Issues

The main challenge was with the `YnabClientSpec` tests using the sttp backend stub:

- **Request Matching Problem**: Our attempts to match requests in the stub backend didn't work correctly. The request matcher conditions were not correctly identifying the incoming requests.

```scala
// This pattern was not matching the actual requests being made
.whenRequestMatches(r => 
  r.uri.path.mkString("/") == "api.test.ynab/user" && 
  r.header("Authorization").contains("Bearer test-token")
)
```

- **Error Message**: We encountered errors like:
  ```
  java.lang.IllegalArgumentException: No behavior stubbed for request: RequestT(GET,https://api.test.ynab/user,NoBody,Vector(Accept-Encoding: gzip, deflate, Authorization: Bearer test-token),MappedResponseAs(ResponseAsByteArray,sttp.client3.SttpApi$$Lambda/0x000000b802f03d98@12f39d2e,Some(as string)),RequestOptions(true,1 minute,32,false),Map())
  ```

- **Request Format Mismatch**: The stubbed behaviors weren't matching the actual requests, suggesting a format difference between what we were trying to match and what was being generated.

### 2. Type Mismatches

We experienced various type mismatch issues:

- **SyncBackendStub vs. SttpBackendStub**: The initial tests used a non-existent `SyncBackendStub` class instead of the correct `SttpBackendStub`.

- **Task Effect Type**: We had to create a proper ZIO `Task`-based backend stub:
  ```scala
  private def createBackendStub() = SttpBackendStub[Task, Any](new RIOMonadAsyncError[Any])
  ```

- **YnabAccount Parameter Order**: The test was using an incorrect parameter order for YnabAccount constructor:
  ```scala
  // Incorrect
  YnabAccount("account-1", "Test Account", "budget-1", "checking", BigDecimal(1000))
  
  // Correct
  YnabAccount(
    id = "account-1", 
    name = "Test Account", 
    accountType = "checking", 
    balance = BigDecimal(1000)
  )
  ```

### 3. Integration Test Environment

For the integration tests, we needed to:

- Provide a dummy backend to the test environment even though each test provides its own layer
- Make tests conditional based on environment variables
- Structure the test to properly cascade failures when API tokens are missing

## Next Steps

To make the client tests work correctly, we would need to:

1. Better understand how the sttp stub backend matches requests
2. Possibly use more precise matchers or debug the actual request format
3. Consider using an alternative approach like recording and verifying requests

The service layer tests provide good coverage of our business logic, so this is an acceptable stopping point for now. Future work should focus on:

1. Fixing the client tests to properly match HTTP requests
2. Adding more granular test cases for error handling
3. Creating a more comprehensive set of integration tests
4. Setting up a dedicated test YNAB budget for automated testing

## Lessons Learned

- **Start with simpler tests**: The service tests were easier to set up and provided good value
- **Use debugging tools**: We should have used more logging/debugging to see what the actual requests looked like
- **Split test responsibilities**: Having separate service and client tests was a good approach
- **Conditional integration tests**: The approach to make integration tests conditional on environment variables works well

These challenges provide useful context for anyone who continues work on improving the YNAB integration test suite in the future.