# End-to-End Tests for YNAB Importer

This module contains end-to-end tests for the YNAB Importer application using Playwright, TestContainers, and ZIO.

## Overview

The end-to-end test framework provides:

1. **Automated browser installation and validation**
2. **Locally running application tests** with plans for full TestContainers integration
3. **Screenshot capture** on test failures for easier debugging
4. **Test data management** for creating test fixtures
5. **Health check endpoint** for application readiness

## Setup

Playwright browsers installation is automated - they'll be automatically installed if missing when you run the tests.

However, you can also install them manually if needed:

```bash
cd ynab-importer
sbt "ynabImporterE2ETests/runMain works.iterative.incubator.e2e.setup.InstallBrowsers"
```

## Running the Tests

The easiest way to run the tests is using the provided script:

```bash
./run-e2e-tests.sh
```

This script will:
1. Start the application
2. Wait for it to be ready (using the health endpoint)
3. Run all E2E tests
4. Stop the application

### Script Options

- `--no-start`: Don't start the application (assumes it's already running)
- `--no-stop`: Don't stop the application after tests

### Running Tests Manually

To run all the e2e tests manually:

```bash
# First, start the application in another terminal
sbtn reStart

# Then run the tests
sbtn ynabImporterE2ETests/test
```

To run a specific test suite:

```bash
sbtn "ynabImporterE2ETests/testOnly works.iterative.incubator.e2e.tests.SourceAccountManagementSpec"
```

## Current Implementation

The current E2E test implementation uses:

1. A locally running application instance (not containerized yet)
2. Mock test data creation (real database interaction coming later)
3. Local connection health checking before tests run
4. Proper test failures with screenshots when things go wrong

## Test Organization

The tests are organized based on the feature files in the `ynab-importer/features` directory. Each test class corresponds to a feature file and implements the scenarios described in the feature.

## Key Components

### TestContainersSupport

Currently provides a simplified local test environment:
- Connection checking to locally running application
- Future plans for full containerized testing with PostgreSQL and application containers

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
- Currently mocks creation of test accounts
- Transaction test data generation
- Helper methods for test setup
- Plans for real database interaction in the future

## Adding New Tests

To add a new test:

1. Create a new test class in the `works.iterative.incubator.e2e.tests` package
2. Extend `ZIOSpecDefault` and mix in `PlaywrightSupport`
3. Implement the scenarios from the corresponding feature file
4. Use `withPlaywright` for local test execution
5. Use helper methods from `PlaywrightSupport` to interact with the page

Example:

```scala
test("My new test") {
  withPlaywright(
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

## Future Improvements

1. Full TestContainers implementation for isolated testing
2. Real database interaction for test data management
3. API-based test data creation
4. Test parallelization
5. Comprehensive reporting