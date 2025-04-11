# Fio Bank API Integration Tests

This module contains integration tests for the Fio Bank API integration in the YNAB Importer application.

## Overview

The integration tests verify that the Fio Bank API client and related services work correctly with the actual Fio Bank API. They test:

1. Connecting to the Fio Bank API
2. Fetching transactions by date range
3. Importing transactions into the application
4. Token security and management

## Running the Tests

These tests are designed to be run manually, not as part of the regular automated test suite, because they require a valid Fio Bank API token.

### Prerequisites

To run the integration tests, you need:

1. A valid Fio Bank API token
2. The ID of a test source account in the application that is connected to your Fio Bank account

### Environment Variables

Set the following environment variables before running the tests:

```bash
export FIO_TOKEN=your_fio_api_token
export FIO_TEST_ACCOUNT_ID=your_test_account_id
```

### Command

Run the tests using sbt:

```bash
sbtn "fio-it/testOnly works.iterative.incubator.fio.integration.FioIntegrationSpec"
```

To run only the security integration tests:

```bash
sbtn "fio-it/testOnly works.iterative.incubator.fio.integration.FioSecurityIntegrationSpec"
```

## Test Structure

### FioIntegrationSpec

Tests basic integration with the Fio Bank API:

- Connection to the API
- Fetching transactions by date range
- Using the import service with repositories

### FioSecurityIntegrationSpec

Tests the security features for token management:

- Token encryption and decryption
- Caching behavior
- Audit logging
- Token updates and invalidation

## Notes

- The tests will only run if the `FIO_API_TOKEN` environment variable is set
- For security, tokens are never logged in full, only their length
- The integration tests use in-memory repositories to avoid modifying actual data
