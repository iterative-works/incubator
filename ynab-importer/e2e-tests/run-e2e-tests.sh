#!/bin/bash

# Run E2E Tests for YNAB Importer
# This script runs all end-to-end tests with a locally running application

# Make sure script exits on first error
set -e

# Make sure the test directory exists
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR/../.."

# Function to print a header
print_header() {
  echo
  echo "=== $1 ==="
  echo
}

# Default mode is to start the app, run tests, and stop the app
START_APP=true
STOP_APP=true
APP_PID=""

# Process command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --no-start)
      START_APP=false
      shift
      ;;
    --no-stop)
      STOP_APP=false
      shift
      ;;
    *)
      echo "Unknown option: $1"
      echo "Usage: $0 [--no-start] [--no-stop]"
      echo "  --no-start: Don't start the application (assumes it's already running)"
      echo "  --no-stop: Don't stop the application after tests"
      exit 1
      ;;
  esac
done

# Function to stop the application
stop_app() {
  if [ -n "$APP_PID" ]; then
    print_header "Stopping application (PID: $APP_PID)"
    kill $APP_PID
    # Wait for the app to actually stop
    wait $APP_PID 2>/dev/null || true
    echo "Application stopped"
  fi
}

# Set up trap to make sure the app is stopped if the script exits
if [ "$STOP_APP" = true ]; then
  trap stop_app EXIT
fi

# Start the application if needed
if [ "$START_APP" = true ]; then
  print_header "Starting the application"
  # Start the application in the background
  sbtn reStart &
  APP_PID=$!
  
  # Wait for the application to start
  echo "Waiting for application to start..."
  attempts=0
  max_attempts=30
  while ! curl -s http://localhost:8080/health > /dev/null; do
    attempts=$((attempts + 1))
    if [ $attempts -ge $max_attempts ]; then
      echo "Application failed to start in time"
      exit 1
    fi
    echo "Waiting... ($attempts/$max_attempts)"
    sleep 1
  done
  echo "Application started successfully"
fi

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
echo "Screenshots are available in the target/e2e-screenshots directory"

# Print location of test results
if [ -d "target/test-reports" ]; then
  echo "Detailed test reports are available in target/test-reports"
fi

echo
echo "=== End of E2E Test Run ==="

# Stop the app unless --no-stop was specified
if [ "$STOP_APP" = true ] && [ "$START_APP" = true ]; then
  # App will be stopped by the trap handler
  :
elif [ "$STOP_APP" = true ]; then
  echo "Note: Application was not started by this script, not stopping it"
fi