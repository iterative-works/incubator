# End-to-End Tests for YNAB Importer

This module contains end-to-end tests for the YNAB Importer application using Playwright, TestContainers, and ZIO.

## Overview

The end-to-end test framework provides:

1. **Automated browser installation and validation**
2. **TestContainers integration** for spinning up isolated test environments
3. **Screenshot capture** on test failures for easier debugging
4. **Test data management** for creating test fixtures
5. **Configuration system** with environment variable support

## Setup

Playwright browsers installation is now completely automated - they'll be automatically installed if missing when you run the tests.

However, you can also install them manually if needed:

```bash
cd ynab-importer
sbt "ynabImporterE2ETests/runMain works.iterative.incubator.e2e.setup.InstallBrowsers"
```

## Running the Tests

To run all the e2e tests:

```bash
sbtn ynabImporterE2ETests/test
```

To run a specific test suite:

```bash
sbtn "ynabImporterE2ETests/testOnly works.iterative.incubator.e2e.tests.SourceAccountManagementSpec"
```

To run a specific test case:

```bash
sbtn "ynabImporterE2ETests/testOnly works.iterative.incubator.e2e.tests.SourceAccountManagementSpec -- -t 'Creating a new source account'"
```

## Configuration

The tests can be configured through environment variables or by editing the `application.conf` file:

### Playwright Configuration

- `E2E_BASE_URL`: Base URL of the application (default: determined automatically from TestContainers)
- `E2E_HEADLESS`: Whether to run in headless mode (default: true)
- `E2E_SLOW_MO`: Slow down execution in ms (default: 0)
- `E2E_TIMEOUT`: Timeout in ms (default: 30000)
- `E2E_BROWSER_TYPE`: Browser to use: chromium, firefox, or webkit (default: chromium)
- `E2E_VIEWPORT_WIDTH`: Viewport width (default: 1280)
- `E2E_VIEWPORT_HEIGHT`: Viewport height (default: 720)

### TestContainers Configuration

- `E2E_PG_IMAGE`: PostgreSQL Docker image (default: postgres:17-alpine)
- `E2E_PG_DB_NAME`: Database name (default: ynab_importer_test)
- `E2E_PG_USERNAME`: Database username (default: test_user)
- `E2E_PG_PASSWORD`: Database password (default: test_password)
- `E2E_APP_IMAGE`: Application Docker image (default: ynab-importer:latest)
- `E2E_APP_SERVER_PORT`: Internal port the app listens on (default: 8080)
- `E2E_APP_EXPOSED_PORT`: Port to expose on host, 0 for random (default: 0)

### Screenshot Configuration

- `E2E_SCREENSHOTS_DIR`: Directory to save screenshots (default: target/e2e-screenshots)
- `E2E_CAPTURE_ON_FAILURE`: Whether to capture screenshots on test failures (default: true)

## Test Organization

The tests are organized based on the feature files in the `ynab-importer/features` directory. Each test class corresponds to a feature file and implements the scenarios described in the feature.

## Key Components

### TestContainersSupport

Provides containerized test environment setup with:
- PostgreSQL database in a Docker container
- Application in a Docker container
- Automatic port mapping and connection configuration

### ScreenshotSupport

Handles screenshot capture for test debugging:
- Automatic capture on test failures
- Manual capture during tests
- Timestamped and named screenshots

### BrowserInstallation

Manages browser installation and verification:
- Automatic checks for browser availability
- Installation when needed
- Detailed error reporting

### TestDataManager

Utilities for test data management:
- Creation of test accounts with random or predefined data
- Transaction test data generation
- Helper methods for test setup

## Adding New Tests

To add a new test:

1. Create a new test class in the `works.iterative.incubator.e2e.tests` package
2. Extend `ZIOSpecDefault` and mix in `PlaywrightSupport`
3. Implement the scenarios from the corresponding feature file
4. Use `withTestContainers` for automatic test environment setup
5. Use helper methods from `PlaywrightSupport` to interact with the page

Example:

```scala
test("My new test") {
  withTestContainers(
    for {
      // Navigate to a page
      _ <- navigateTo("/my-page")
      
      // Take a screenshot
      _ <- takeScreenshot("my-page")
      
      // Interact with the page
      _ <- click("#my-button")
      
      // Assert something
      result <- getText("#result")
    } yield {
      assertTrue(result.contains("Expected Value"))
    }
  )
}
```
