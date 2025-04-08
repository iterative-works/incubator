# YNAB CLI Tool Design - 2025-04-09

## Purpose

Creating a CLI tool for YNAB API testing will provide us with a quick way to:

1. Verify connectivity to the YNAB API
2. Test retrieval of budgets, accounts, and categories
3. Submit test transactions to YNAB
4. Debug issues without requiring the full application

This will be invaluable for testing our YNAB integration before we connect it to the web UI.

## Functionality

The CLI tool will support the following commands:

```
Usage: ynab-cli [options] [command]

Options:
  --token TOKEN    YNAB API token (or set YNAB_API_TOKEN env variable)
  --budget BUDGET  YNAB budget ID (can also be selected interactively)
  --verbose        Enable verbose logging

Commands:
  verify           Verify API token is valid
  budgets          List available budgets
  accounts         List accounts in selected budget
  categories       List categories in selected budget
  submit-tx        Submit a test transaction
  help             Show this help message
```

## Implementation

We'll create a new Scala file in the `ynab-importer/core/src/test/scala/works/iterative/incubator/ynab` directory called `YnabApiCliTool.scala`.

The tool will:

1. Use the same `YnabService` implementation that the main application will use
2. Support command-line arguments using a simple parser
3. Format output for easy reading in the terminal
4. Provide both interactive and scripted modes

### Example Code Structure

```scala
package works.iterative.incubator.ynab

import zio.*
import works.iterative.incubator.ynab.application.service.YnabService
import works.iterative.incubator.ynab.infrastructure.config.{YnabConfig, SecretApiToken}
import works.iterative.incubator.ynab.domain.model.*
import java.time.LocalDate

object YnabApiCliTool extends ZIOAppDefault:
  // Command parsing
  def parseArgs(args: Chunk[String]): IO[String, Command] = ???

  // Run the selected command
  def runCommand(command: Command, service: YnabService): Task[Unit] = ???

  // Display helpers
  def displayBudgets(budgets: Seq[YnabBudget]): Unit = ???
  def displayAccounts(accounts: Seq[YnabAccount]): Unit = ???
  def displayCategories(categories: Seq[YnabCategory]): Unit = ???

  // Interactive budget selection
  def selectBudget(budgets: Seq[YnabBudget]): Task[String] = ???

  // Main program
  override def run: ZIO[Environment, Any, Any] =
    for
      args <- getArgs
      command <- parseArgs(args).orDieWith(msg => new IllegalArgumentException(msg))
      
      // Read token from args or environment
      token = command.token.getOrElse(sys.env.getOrElse("YNAB_API_TOKEN", ""))
      
      // Create service with config
      config = YnabConfig(SecretApiToken(token), command.budgetId)
      service <- ZIO.service[YnabService].provide(
        // Layer for YnabServiceImpl
        ???
      )
      
      // Run the command
      _ <- runCommand(command, service)
    yield ()

// Command ADT
enum Command:
  case Verify(token: Option[String])
  case ListBudgets(token: Option[String])
  case ListAccounts(token: Option[String], budgetId: Option[String])
  case ListCategories(token: Option[String], budgetId: Option[String])
  case SubmitTransaction(token: Option[String], budgetId: Option[String], accountId: Option[String], amount: BigDecimal, payee: String, category: Option[String])
  case Help
```

## Benefits

1. **Rapid Development**: The CLI tool can be developed quickly to test our YNAB integration
2. **Independent Testing**: Allows testing of YNAB integration without the web UI
3. **Scriptable**: Can be run from scripts for automated testing
4. **Reusable**: We can reuse the same service implementation that will be used in the main application

## Next Steps

1. Implement the basic CLI tool structure with command parsing
2. Implement the verification and budget listing functionality
3. Add account and category listing
4. Implement test transaction submission
5. Document usage in a markdown file