package works.iterative.incubator.ynab

import zio.*
import works.iterative.incubator.ynab.application.service.YnabService
import works.iterative.incubator.ynab.infrastructure.config.{YnabConfig, SecretApiToken}
import works.iterative.incubator.ynab.infrastructure.service.YnabServiceImpl
import works.iterative.incubator.ynab.domain.model.*
import java.time.LocalDate

/** CLI tool for testing YNAB API integration
  *
  * This tool allows testing the YNAB API connection, listing budgets, accounts, and categories, and
  * submitting test transactions without requiring the full application.
  */
object YnabApiCliTool extends ZIOAppDefault:
    // Command parsing
    def parseArgs(args: Chunk[String]): IO[String, Command] =
        if args.isEmpty then
            ZIO.succeed(Command.Help)
        else
            args(0) match
                case "verify" =>
                    ZIO.succeed(Command.Verify(
                        getOptionValue(args, "--token")
                    ))
                case "budgets" =>
                    ZIO.succeed(Command.ListBudgets(
                        getOptionValue(args, "--token")
                    ))
                case "accounts" =>
                    ZIO.succeed(Command.ListAccounts(
                        getOptionValue(args, "--token"),
                        getOptionValue(args, "--budget")
                    ))
                case "categories" =>
                    ZIO.succeed(Command.ListCategories(
                        getOptionValue(args, "--token"),
                        getOptionValue(args, "--budget")
                    ))
                case "submit-tx" =>
                    for
                        token <- ZIO.fromOption(getOptionValue(args, "--token"))
                            .orElseSucceed(sys.env.getOrElse("YNAB_API_TOKEN", ""))
                        budgetId <- ZIO.fromOption(getOptionValue(args, "--budget"))
                            .orElseFail("Budget ID is required for submit-tx command")
                        accountId <- ZIO.fromOption(getOptionValue(args, "--account"))
                            .orElseFail("Account ID is required for submit-tx command")
                        amount <- ZIO.fromOption(getOptionValue(args, "--amount"))
                            .orElseFail("Amount is required for submit-tx command")
                            .flatMap(a =>
                                ZIO.attempt(BigDecimal(a))
                                    .orElseFail("Invalid amount")
                            )
                        payee <- ZIO.fromOption(getOptionValue(args, "--payee"))
                            .orElseFail("Payee is required for submit-tx command")
                        category = getOptionValue(args, "--category")
                    yield Command.SubmitTransaction(
                        Some(token),
                        Some(budgetId),
                        Some(accountId),
                        amount,
                        payee,
                        category
                    )
                case "help" | _ =>
                    ZIO.succeed(Command.Help)

    private def getOptionValue(args: Chunk[String], option: String): Option[String] =
        val index = args.indexOf(option)
        if index >= 0 && index < args.size - 1 then
            Some(args(index + 1))
        else
            None
    end getOptionValue

    // Run the selected command
    def runCommand(command: Command, service: YnabService): Task[Unit] =
        command match
            case Command.Verify(_) =>
                for
                    isValid <- service.verifyConnection()
                    _ <- Console.printLine(
                        if isValid then "✅ API token is valid"
                        else "❌ API token is invalid"
                    )
                yield ()

            case Command.ListBudgets(_) =>
                for
                    budgets <- service.getBudgets()
                    _ <- displayBudgets(budgets)
                yield ()

            case Command.ListAccounts(_, Some(budgetId)) =>
                val budgetService = service.getBudgetService(budgetId)
                for
                    accounts <- budgetService.getAccounts()
                    _ <- displayAccounts(accounts)
                yield ()

            case Command.ListAccounts(_, None) =>
                for
                    budgets <- service.getBudgets()
                    _ <- displayBudgets(budgets)
                    budgetId <- selectBudget(budgets)
                    budgetService = service.getBudgetService(budgetId)
                    accounts <- budgetService.getAccounts()
                    _ <- displayAccounts(accounts)
                yield ()

            case Command.ListCategories(_, Some(budgetId)) =>
                val budgetService = service.getBudgetService(budgetId)
                for
                    categoryGroups <- budgetService.getCategoryGroups()
                    categories <- budgetService.getCategories()
                    _ <- displayCategoryGroups(categoryGroups)
                    _ <- displayCategories(categories)
                yield ()
                end for

            case Command.ListCategories(_, None) =>
                for
                    budgets <- service.getBudgets()
                    _ <- displayBudgets(budgets)
                    budgetId <- selectBudget(budgets)
                    budgetService = service.getBudgetService(budgetId)
                    categoryGroups <- budgetService.getCategoryGroups()
                    categories <- budgetService.getCategories()
                    _ <- displayCategoryGroups(categoryGroups)
                    _ <- displayCategories(categories)
                yield ()

            case Command.SubmitTransaction(_, Some(budgetId), accountId, amount, payee, category) =>
                val transaction = YnabTransaction(
                    date = LocalDate.now(),
                    amount = amount,
                    accountId = accountId.get,
                    payeeName = Some(payee),
                    categoryId = category,
                    memo = Some("Test transaction from YNAB CLI tool")
                )

                val budgetService = service.getBudgetService(budgetId)

                for
                    id <- budgetService.createTransaction(transaction)
                    _ <- Console.printLine(s"✅ Transaction created successfully with ID: $id")
                yield ()
                end for
                
            case Command.SubmitTransaction(_, None, _, _, _, _) =>
                for
                    budgets <- service.getBudgets()
                    _ <- displayBudgets(budgets)
                    budgetId <- selectBudget(budgets)
                    // Recursive call with the selected budget ID
                    _ <- runCommand(
                        Command.SubmitTransaction(
                            None, 
                            Some(budgetId), 
                            command.asInstanceOf[Command.SubmitTransaction].accountId,
                            command.asInstanceOf[Command.SubmitTransaction].amount,
                            command.asInstanceOf[Command.SubmitTransaction].payee,
                            command.asInstanceOf[Command.SubmitTransaction].category
                        ), 
                        service
                    )
                yield ()

            case Command.Help =>
                displayHelp()

    // Display helpers
    def displayBudgets(budgets: Seq[YnabBudget]): Task[Unit] =
        Console.printLine("Available Budgets:") *>
            ZIO.foreach(budgets) { budget =>
                Console.printLine(
                    s"ID: ${budget.id}\n" +
                        s"Name: ${budget.name}\n" +
                        s"Currency: ${budget.currencyCode.getOrElse("N/A")}\n" +
                        s"Last Modified: ${budget.lastModifiedOn.getOrElse("N/A")}\n"
                )
            }.unit

    def displayAccounts(accounts: Seq[YnabAccount]): Task[Unit] =
        Console.printLine("Accounts:") *>
            ZIO.foreach(accounts.filterNot(_.deleted)) { account =>
                Console.printLine(
                    s"ID: ${account.id}\n" +
                        s"Name: ${account.name}\n" +
                        s"Type: ${account.accountType}\n" +
                        s"Balance: ${formatAmount(account.balance)}\n" +
                        s"On Budget: ${account.onBudget}\n" +
                        (if account.closed then "Status: Closed\n" else "Status: Open\n")
                )
            }.unit

    def displayCategoryGroups(groups: Seq[YnabCategoryGroup]): Task[Unit] =
        Console.printLine("Category Groups:") *>
            ZIO.foreach(groups.filterNot(_.deleted)) { group =>
                Console.printLine(
                    s"ID: ${group.id}\n" +
                        s"Name: ${group.name}\n" +
                        (if group.hidden then "Status: Hidden\n" else "Status: Visible\n")
                )
            }.unit

    def displayCategories(categories: Seq[YnabCategory]): Task[Unit] =
        Console.printLine("Categories:") *>
            ZIO.foreach(categories.filterNot(_.deleted)) { category =>
                Console.printLine(
                    s"ID: ${category.id}\n" +
                        s"Name: ${category.name}\n" +
                        s"Group: ${category.groupName}\n" +
                        (if category.hidden then "Status: Hidden\n" else "Status: Visible\n") +
                        category.budgeted.map(b => s"Budgeted: ${formatAmount(b)}\n").getOrElse(
                            ""
                        ) +
                        category.activity.map(a => s"Activity: ${formatAmount(a)}\n").getOrElse(
                            ""
                        ) +
                        category.balance.map(b => s"Balance: ${formatAmount(b)}\n").getOrElse("")
                )
            }.unit

    def displayHelp(): Task[Unit] =
        Console.printLine(
            """
            |Usage: ynab-cli [options] [command]
            |
            |Options:
            |  --token TOKEN    YNAB API token (or set YNAB_API_TOKEN env variable)
            |  --budget BUDGET  YNAB budget ID (required for accounts, categories, submit-tx)
            |  --verbose        Enable verbose logging
            |
            |Commands:
            |  verify           Verify API token is valid
            |  budgets          List available budgets
            |  accounts         List accounts in selected budget
            |  categories       List categories in selected budget
            |  submit-tx        Submit a test transaction
            |    --account ACCOUNT_ID  Account ID for the transaction
            |    --amount AMOUNT       Transaction amount (positive for inflow, negative for outflow)
            |    --payee PAYEE         Payee name
            |    --category CATEGORY   Optional category ID
            |  help             Show this help message
            """.stripMargin
        )

    // Interactive budget selection
    def selectBudget(budgets: Seq[YnabBudget]): Task[String] =
        for
            _ <- Console.printLine("Select a budget:")
            _ <- ZIO.foreachDiscard(budgets.zipWithIndex) { case (budget, index) =>
                Console.printLine(s"${index + 1}. ${budget.name}")
            }
            selection <- Console.readLine.flatMap { input =>
                ZIO.attempt(input.toInt - 1)
                    .mapError(_ => new NumberFormatException("Invalid selection"))
                    .filterOrFail(i => i >= 0 && i < budgets.length)(
                        new IndexOutOfBoundsException("Selection out of range")
                    )
                    .map(budgets(_).id)
            }.orElse(Console.printLine("Invalid selection, please try again") *> selectBudget(
                budgets
            ))
        yield selection

    // Helper for formatting currency amounts
    private def formatAmount(amount: BigDecimal): String =
        val absAmount = amount.abs
        val sign = if amount < 0 then "-" else ""
        f"$sign$$${absAmount.setScale(2, BigDecimal.RoundingMode.HALF_UP)}%.2f"

    // Main program
    override def run: ZIO[ZIOAppArgs, Any, Any] =
        for
            args <- getArgs
            command <- parseArgs(args).orDieWith(msg => new IllegalArgumentException(msg))

            // Read token from args or environment
            token = command match
                case Command.Verify(t)                           => t
                case Command.ListBudgets(t)                      => t
                case Command.ListAccounts(t, _)                  => t
                case Command.ListCategories(t, _)                => t
                case Command.SubmitTransaction(t, _, _, _, _, _) => t
                case _                                           => None
            tokenValue = token.getOrElse(sys.env.getOrElse("YNAB_API_TOKEN", ""))

            // Create service with config
            config = YnabConfig(SecretApiToken(tokenValue))
            layer = ZLayer.succeed(config) >>> YnabServiceImpl.live
            service <- ZIO.serviceWithZIO[YnabService](s => ZIO.succeed(s)).provide(layer)

            // Run the command
            _ <- runCommand(command, service)
        yield ()
end YnabApiCliTool

// Command ADT
enum Command:
    case Verify(token: Option[String])
    case ListBudgets(token: Option[String])
    case ListAccounts(token: Option[String], budgetId: Option[String])
    case ListCategories(token: Option[String], budgetId: Option[String])
    case SubmitTransaction(
        token: Option[String],
        budgetId: Option[String],
        accountId: Option[String],
        amount: BigDecimal,
        payee: String,
        category: Option[String]
    )
    case Help
end Command