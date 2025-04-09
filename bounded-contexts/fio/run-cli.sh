#!/bin/bash
# Run the Fio CLI tool with environment variables

# Usage information
function show_usage {
  echo "Usage: $0 COMMAND [options]"
  echo ""
  echo "Commands:"
  echo "  import --from=YYYY-MM-DD --to=YYYY-MM-DD  Import transactions for date range"
  echo "  import-new                                Import new transactions since last import"
  echo "  list-accounts                             List available Fio accounts"
  echo "  help                                      Show this help message"
  echo ""
  echo "Environment variables:"
  echo "  FIO_TOKEN                      Fio API token (required)"
  echo "  USE_POSTGRES                   Use PostgreSQL instead of in-memory storage (default: false)"
  echo ""
  echo "Example:"
  echo "  FIO_TOKEN=your-token $0 import --from=2025-04-01 --to=2025-04-09"
}

# Show usage if no arguments provided
if [ $# -eq 0 ]; then
  show_usage
  exit 1
fi

# Check if FIO_TOKEN is set
if [ -z "$FIO_TOKEN" ]; then
  echo "Error: FIO_TOKEN environment variable is not set."
  echo "Please set it before running the CLI tool:"
  echo "  FIO_TOKEN=your-token $0 COMMAND"
  exit 1
fi

# Pass all arguments to the SBT run command
cd "$(dirname "$0")/../.." || exit 1
SBT_COMMAND="sbtn \"fio/runMain works.iterative.incubator.fio.cli.FioCliMain $*\""
echo "Running: $SBT_COMMAND"
eval "$SBT_COMMAND"