# End-to-End Tests for YNAB Importer

This module contains end-to-end tests for the YNAB Importer application using Playwright and ZIO.

## Setup

Before running the tests, you need to install the Playwright browsers. This only needs to be done once, and make sure not to use sbtn, or it exits your SBT at the end:

```bash
cd ynab-importer
sbt "e2e-tests/runMain works.iterative.incubator.e2e.setup.InstallBrowsers"
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

## Configuration

The tests can be configured through environment variables or by editing the `application.conf` file:

- `E2E_BASE_URL`: Base URL of the application (default: http://localhost:8080)
- `E2E_HEADLESS`: Whether to run in headless mode (default: true)
- `E2E_SLOW_MO`: Slow down execution in ms (default: 0)
- `E2E_TIMEOUT`: Timeout in ms (default: 30000)
- `E2E_BROWSER_TYPE`: Browser to use: chromium, firefox, or webkit (default: chromium)
- `E2E_VIEWPORT_WIDTH`: Viewport width (default: 1280)
- `E2E_VIEWPORT_HEIGHT`: Viewport height (default: 720)

## Test Organization

The tests are organized based on the feature files in the `ynab-importer/features` directory. Each test class corresponds to a feature file and implements the scenarios described in the feature.

## Adding New Tests

To add a new test:

1. Create a new test class in the `works.iterative.incubator.e2e.tests` package
2. Extend `ZIOSpecDefault` and mix in `PlaywrightSupport`
3. Implement the scenarios from the corresponding feature file
4. Use the helper methods from `PlaywrightSupport` to interact with the page
