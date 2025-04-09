package works.iterative.incubator.ynab.infrastructure.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.LocalDate
import works.iterative.incubator.ynab.domain.model.*
import works.iterative.incubator.ynab.infrastructure.client.YnabClient

object YnabServiceSpec extends ZIOSpecDefault:
    // Sample data
    private val sampleBudget = YnabBudget("budget-1", "Test Budget", None)
    private val sampleAccount = YnabAccount(
        id = "account-1",
        name = "Test Account",
        accountType = "checking",
        balance = BigDecimal(1000)
    )
    private val sampleCategoryGroup = YnabCategoryGroup("group-1", "Test Group", false, false)
    private val sampleCategory = YnabCategory(
        "category-1",
        "Test Category",
        "group-1",
        "Test Group",
        hidden = false,
        deleted = false,
        budgeted = Some(BigDecimal(100)),
        activity = Some(BigDecimal(-50)),
        balance = Some(BigDecimal(50))
    )
    private val sampleTransaction = YnabTransaction(
        id = None,
        date = LocalDate.of(2025, 4, 10),
        amount = BigDecimal(-10.99),
        memo = Some("Test transaction"),
        accountId = "account-1",
        payeeName = Some("Test Payee")
    )

    // Mock YnabClient
    class MockYnabClient extends YnabClient:
        var verifyTokenCalled = false
        var getBudgetsCalled = false
        var getAccountsCalled = false
        var getCategoryGroupsCalled = false
        var getCategoriesCalled = false
        var createTransactionCalled = false
        var createTransactionsBatchCalled = false
        var lastBudgetId: Option[String] = None

        override def verifyToken(): Task[Boolean] =
            verifyTokenCalled = true
            ZIO.succeed(true)

        override def getBudgets(): Task[Seq[YnabBudget]] =
            getBudgetsCalled = true
            ZIO.succeed(Seq(sampleBudget))

        override def getAccounts(budgetId: String): Task[Seq[YnabAccount]] =
            getAccountsCalled = true
            lastBudgetId = Some(budgetId)
            ZIO.succeed(Seq(sampleAccount))
        end getAccounts

        override def getCategoryGroups(budgetId: String): Task[Seq[YnabCategoryGroup]] =
            getCategoryGroupsCalled = true
            lastBudgetId = Some(budgetId)
            ZIO.succeed(Seq(sampleCategoryGroup))
        end getCategoryGroups

        override def getCategories(budgetId: String): Task[Seq[YnabCategory]] =
            getCategoriesCalled = true
            lastBudgetId = Some(budgetId)
            ZIO.succeed(Seq(sampleCategory))
        end getCategories

        override def createTransaction(
            budgetId: String,
            transaction: YnabTransaction
        ): Task[String] =
            createTransactionCalled = true
            lastBudgetId = Some(budgetId)
            ZIO.succeed("transaction-1")
        end createTransaction

        override def createTransactions(
            budgetId: String,
            transactions: Seq[YnabTransaction]
        ): Task[Map[YnabTransaction, String]] =
            createTransactionsBatchCalled = true
            lastBudgetId = Some(budgetId)
            ZIO.succeed(transactions.map(tx => tx -> "transaction-1").toMap)
        end createTransactions
    end MockYnabClient

    def spec = suite("YnabService")(
        // YnabService tests
        test("verifyConnection should delegate to client.verifyToken") {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                service <- ZIO.succeed(YnabServiceImpl(mockClient))
                result <- service.verifyConnection()
            yield assertTrue(
                result == true,
                mockClient.verifyTokenCalled
            )
        },
        test("getBudgets should delegate to client.getBudgets") {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                service <- ZIO.succeed(YnabServiceImpl(mockClient))
                budgets <- service.getBudgets()
            yield assertTrue(
                budgets.size == 1,
                budgets.head == sampleBudget,
                mockClient.getBudgetsCalled
            )
        },
        test("getBudgetService should return a YnabBudgetService with the specified budgetId") {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                service <- ZIO.succeed(YnabServiceImpl(mockClient))
                budgetService = service.getBudgetService("budget-1")
                // Call a method to verify that the budgetId is passed correctly
                accounts <- budgetService.getAccounts()
            yield assertTrue(
                accounts.size == 1,
                accounts.head == sampleAccount,
                mockClient.getAccountsCalled,
                mockClient.lastBudgetId.contains("budget-1")
            )
        },

        // YnabBudgetService tests
        test(
            "YnabBudgetService.getAccounts should delegate to client.getAccounts with correct budgetId"
        ) {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                budgetService <- ZIO.succeed(YnabBudgetServiceImpl(mockClient, "budget-1"))
                accounts <- budgetService.getAccounts()
            yield assertTrue(
                accounts.size == 1,
                accounts.head == sampleAccount,
                mockClient.getAccountsCalled,
                mockClient.lastBudgetId.contains("budget-1")
            )
        },
        test(
            "YnabBudgetService.getCategoryGroups should delegate to client.getCategoryGroups with correct budgetId"
        ) {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                budgetService <- ZIO.succeed(YnabBudgetServiceImpl(mockClient, "budget-1"))
                categoryGroups <- budgetService.getCategoryGroups()
            yield assertTrue(
                categoryGroups.size == 1,
                categoryGroups.head == sampleCategoryGroup,
                mockClient.getCategoryGroupsCalled,
                mockClient.lastBudgetId.contains("budget-1")
            )
        },
        test(
            "YnabBudgetService.getCategories should delegate to client.getCategories with correct budgetId"
        ) {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                budgetService <- ZIO.succeed(YnabBudgetServiceImpl(mockClient, "budget-1"))
                categories <- budgetService.getCategories()
            yield assertTrue(
                categories.size == 1,
                categories.head == sampleCategory,
                mockClient.getCategoriesCalled,
                mockClient.lastBudgetId.contains("budget-1")
            )
        },
        test(
            "YnabBudgetService.createTransaction should delegate to client.createTransaction with correct budgetId"
        ) {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                budgetService <- ZIO.succeed(YnabBudgetServiceImpl(mockClient, "budget-1"))
                transactionId <- budgetService.createTransaction(sampleTransaction)
            yield assertTrue(
                transactionId == "transaction-1",
                mockClient.createTransactionCalled,
                mockClient.lastBudgetId.contains("budget-1")
            )
        },
        test(
            "YnabBudgetService.createTransactions should delegate to client.createTransactions with correct budgetId"
        ) {
            for
                mockClient <- ZIO.succeed(new MockYnabClient())
                budgetService <- ZIO.succeed(YnabBudgetServiceImpl(mockClient, "budget-1"))
                result <- budgetService.createTransactions(Seq(sampleTransaction))
            yield assertTrue(
                result.size == 1,
                result.keys.head == sampleTransaction,
                result.values.head == "transaction-1",
                mockClient.createTransactionsBatchCalled,
                mockClient.lastBudgetId.contains("budget-1")
            )
        }
    )
end YnabServiceSpec
