#!/bin/bash

# Run E2E Tests for YNAB Importer
# This script runs all end-to-end tests in sequence
# It reports results and generates screenshots for debugging

echo "=== YNAB Importer E2E Test Runner ==="
echo "Starting tests at $(date)"

# Make sure the test directory exists
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR/.."

# Function to print a header
print_header() {
  echo
  echo "=== $1 ==="
  echo
}

# First, install browsers if needed
print_header "Installing browsers if needed"
sbtn "ynabImporterE2ETests/Test/runMain works.iterative.incubator.e2e.setup.InstallBrowsers"

# Run the application tests in sequence
print_header "Running Source Account Management tests"
sbtn "ynabImporterE2ETests/testOnly works.iterative.incubator.e2e.tests.SourceAccountManagementSpec"

# Add more test suites as they're implemented
# print_header "Running Transaction Import tests"
# sbtn "ynabImporterE2ETests/testOnly works.iterative.incubator.e2e.tests.TransactionImportSpec"

# Print test completion message
print_header "E2E Test Summary"
echo "Tests completed at $(date)"
echo "Screenshots are available in the target/screenshots directory"

# Print location of test results
if [ -d "target/test-reports" ]; then
  echo "Detailed test reports are available in target/test-reports"
fi

echo
echo "=== End of E2E Test Run ==="