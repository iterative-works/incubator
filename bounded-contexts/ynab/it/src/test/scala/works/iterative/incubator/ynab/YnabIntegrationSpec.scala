package works.iterative.incubator.ynab.integration

import zio.*
import zio.test.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import works.iterative.incubator.ynab.application.service.YnabService
import works.iterative.incubator.ynab.infrastructure.client.YnabClient
import works.iterative.incubator.ynab.infrastructure.service.YnabServiceImpl
import works.iterative.incubator.ynab.domain.model.*
import java.time.LocalDate

/** Integration tests for the YNAB API integration.
  *
  * These tests require a valid YNAB API token and budget ID. They are designed to be run manually,
  * not as part of the automated test suite.
  *
  * To run these tests, you need to set the following environment variables:
  *   - YNAB_API_TOKEN: A valid YNAB API token
  *   - YNAB_TEST_BUDGET_ID: ID of a test budget to use (optional)
  *   - YNAB_TEST_ACCOUNT_ID: ID of a test account to use (optional)
  *
  * If the environment variables are not set, the tests will be skipped.
  */
object YnabIntegrationSpec extends ZIOSpecDefault:
    // Read config from environment
    private val testBudgetId = sys.env.get("YNAB_TEST_BUDGET_ID")
    private val testAccountId = sys.env.get("YNAB_TEST_ACCOUNT_ID")

    // Service and backend layers
    private val backendLayer = HttpClientZioBackend.layer()

    private val serviceLayer =
        backendLayer >>> YnabClient.layer >>> YnabServiceImpl.layer

    // Unique test transaction
    private def createTestTransaction(accountId: String) =
        val date = LocalDate.now
        val amount =
            BigDecimal(-0.01) // 1 cent transaction to avoid accidentally modifying real data
        val timestamp = java.lang.System.currentTimeMillis()

        YnabTransaction(
            id = None,
            date = date,
            amount = amount,
            memo = Some(s"Integration test transaction $timestamp"),
            accountId = accountId,
            payeeName = Some("YNAB Integration Test"),
            // Let the YNAB client generate the import ID
            importId = None
        )
    end createTestTransaction

    def spec = suite("YNAB Integration Tests")(
        test("YNAB service should connect successfully") {
            for
                service <- ZIO.service[YnabService]
                connected <- service.verifyConnection()
            yield assertTrue(connected)
        },
        test("YNAB service should retrieve budgets") {
            for
                service <- ZIO.service[YnabService]
                budgets <- service.getBudgets()
                _ <- Console.printLine(s"Found ${budgets.size} budgets:")
                _ <- ZIO.foreach(budgets)(budget =>
                    Console.printLine(s"- ${budget.name} (${budget.id})")
                )
            yield assertTrue(budgets.nonEmpty)
        },
        test("YNAB service should retrieve accounts for a budget") {
            (for
                service <- ZIO.service[YnabService]
                // Use provided test budget ID or get the first budget
                budgetId <- ZIO.fromOption(testBudgetId).catchAll(_ =>
                    service.getBudgets().map(_.headOption.map(_.id))
                        .flatMap(ZIO.fromOption(_))
                        .orElseFail(new RuntimeException("No budgets available"))
                )
                budgetService = service.getBudgetService(budgetId)
                accounts <- budgetService.getAccounts()
                _ <- Console.printLine(s"Found ${accounts.size} accounts in budget $budgetId:")
                _ <- ZIO.foreach(accounts)(account =>
                    Console.printLine(s"- ${account.name} (${account.id}): ${account.balance}")
                )
            yield assertTrue(accounts.nonEmpty))
        },
        test("YNAB service should retrieve categories for a budget") {
            (for
                service <- ZIO.service[YnabService]
                // Use provided test budget ID or get the first budget
                budgetId <- ZIO.fromOption(testBudgetId).catchAll(_ =>
                    service.getBudgets().map(_.headOption.map(_.id))
                        .flatMap(ZIO.fromOption(_))
                        .orElseFail(new RuntimeException("No budgets available"))
                )
                budgetService = service.getBudgetService(budgetId)
                categories <- budgetService.getCategories()
                _ <- Console.printLine(s"Found ${categories.size} categories in budget $budgetId")
            yield assertTrue(categories.nonEmpty))
        },
        test("YNAB service should create a transaction") {
            (for
                service <- ZIO.service[YnabService]
                // Use provided test budget ID or get the first budget
                budgetId <- ZIO.fromOption(testBudgetId).catchAll(_ =>
                    service.getBudgets().map(_.headOption.map(_.id))
                        .flatMap(ZIO.fromOption(_))
                        .orElseFail(new RuntimeException("No budgets available"))
                )
                // Use provided test account ID or get the first account
                accountId <- ZIO.fromOption(testAccountId).catchAll(_ =>
                    service.getBudgetService(budgetId).getAccounts()
                        .map(_.headOption.map(_.id))
                        .flatMap(ZIO.fromOption(_))
                        .orElseFail(new RuntimeException("No accounts available"))
                )
                budgetService = service.getBudgetService(budgetId)
                // Create a test transaction
                transaction = createTestTransaction(accountId)
                _ <- Console.printLine(s"Creating test transaction in account $accountId")
                transactionId <- budgetService.createTransaction(transaction)
                _ <- Console.printLine(s"Created transaction with ID: $transactionId")
            yield assertTrue(transactionId.nonEmpty))
        }
    ).provideLayerShared(
        serviceLayer
    ) @@ TestAspect.withLiveEnvironment @@ TestAspect.ifEnvSet("YNAB_TOKEN") @@ TestAspect.ifEnvSet(
        "YNAB_TEST_BUDGET_ID"
    )
end YnabIntegrationSpec
