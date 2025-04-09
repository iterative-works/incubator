package works.iterative.incubator.ynab.infrastructure.client

import zio.*
import zio.test.*
import zio.test.Assertion.*
import sttp.client4.*
import sttp.model.StatusCode
import works.iterative.incubator.ynab.domain.model.*
import works.iterative.incubator.ynab.infrastructure.config.{YnabConfig, SecretApiToken}
import java.time.LocalDate
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.Method

object YnabClientSpec extends ZIOSpecDefault:
    // Test configuration
    private val testConfig = YnabConfig(SecretApiToken("test-token"), "https://api.test.ynab")

    // Sample data
    private val sampleTransaction = YnabTransaction(
        id = None,
        date = LocalDate.of(2025, 4, 10),
        amount = BigDecimal(-10.99),
        memo = Some("Test transaction"),
        accountId = "account-1",
        payeeName = Some("Test Payee")
    )

    // Sample JSON responses
    private val budgetsJson = """
  {
    "data": {
      "budgets": [
        {
          "id": "budget-1",
          "name": "Test Budget",
          "last_modified_on": null
        }
      ]
    }
  }
  """

    private val accountsJson = """
  {
    "data": {
      "accounts": [
        {
          "id": "account-1",
          "name": "Test Account",
          "type": "checking",
          "balance": 1000,
          "closed": false,
          "on_budget": true,
          "transfer_payee_id": null,
          "deleted": false
        }
      ]
    }
  }
  """

    private val categoriesJson = """
  {
    "data": {
      "category_groups": [
        {
          "id": "group-1",
          "name": "Test Group",
          "hidden": false,
          "deleted": false,
          "categories": [
            {
              "id": "category-1",
              "name": "Test Category",
              "hidden": false,
              "deleted": false,
              "budgeted": 100,
              "activity": -50,
              "balance": 50
            }
          ]
        }
      ]
    }
  }
  """

    private val createTransactionJson = """
  {
    "data": {
      "transaction_ids": ["transaction-1"],
      "transaction": {
        "id": "transaction-1",
        "date": "2025-04-10",
        "amount": -10990,
        "memo": "Test transaction",
        "cleared": "cleared",
        "approved": true,
        "account_id": "account-1",
        "payee_name": "Test Payee",
        "payee_id": null,
        "category_id": null,
        "flag_color": null,
        "import_id": "20250410:-10990:12345678"
      }
    }
  }
  """

    private val createTransactionsJson = """
  {
    "data": {
      "transactions": [
        {
          "id": "transaction-1",
          "date": "2025-04-10",
          "amount": -10990,
          "memo": "Test transaction",
          "cleared": "cleared",
          "approved": true,
          "account_id": "account-1",
          "payee_name": "Test Payee",
          "payee_id": null,
          "category_id": null,
          "flag_color": null,
          "import_id": "20250410:-10990:12345678"
        }
      ]
    }
  }
  """

    // Error responses
    private val unauthorizedJson = """
  {
    "error": {
      "id": "401",
      "name": "unauthorized",
      "detail": "Invalid API token"
    }
  }
  """

    private val notFoundJson = """
  {
    "error": {
      "id": "404",
      "name": "resource_not_found",
      "detail": "Resource not found"
    }
  }
  """

    private val badRequestJson = """
  {
    "error": {
      "id": "400",
      "name": "bad_request",
      "detail": "Invalid request data"
    }
  }
  """

    def spec = suite("YnabClient")(
        // Authentication test
        test("verifyToken should return true when token is valid") {
            // Create a stub backend that returns 200 OK for the user endpoint
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/user") &&
                        r.header("Authorization").contains("Bearer test-token")
                )
                .thenRespondWithCode(StatusCode.Ok)

            val client = YnabClientLive(testConfig, testBackend)

            for
                result <- client.verifyToken()
            yield assertTrue(result)
        },
        test("verifyToken should return false when token is invalid") {
            // Create a stub backend that returns 401 Unauthorized for the user endpoint
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r => r.uri.toString.contains("/user"))
                .thenRespondAdjust(unauthorizedJson, StatusCode.Unauthorized)

            val client = YnabClientLive(testConfig, testBackend)

            for
                result <- client.verifyToken()
            yield assertTrue(!result)
        },

        // Budget tests
        test("getBudgets should return budgets when successful") {
            // Create a stub backend that returns budgets
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r => r.uri.toString.contains("/budgets"))
                .thenRespondAdjust(budgetsJson)

            val client = YnabClientLive(testConfig, testBackend)

            for
                budgets <- client.getBudgets()
            yield assertTrue(
                budgets.size == 1,
                budgets.head.id == "budget-1",
                budgets.head.name == "Test Budget"
            )
        },
        test("getBudgets should fail with YnabAuthenticationError when unauthorized") {
            // Create a stub backend that returns 401 Unauthorized
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r => r.uri.toString.contains("/budgets"))
                .thenRespondAdjust(unauthorizedJson, StatusCode.Unauthorized)

            val client = YnabClientLive(testConfig, testBackend)

            for
                error <- client.getBudgets().flip
            yield assert(error)(isSubtype[YnabAuthenticationError](anything))
        },

        // Account tests
        test("getAccounts should return accounts for a budget") {
            // Create a stub backend that returns accounts
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/budget-1/accounts")
                )
                .thenRespondAdjust(accountsJson)

            val client = YnabClientLive(testConfig, testBackend)

            for
                accounts <- client.getAccounts("budget-1")
            yield assertTrue(
                accounts.size == 1,
                accounts.head.id == "account-1",
                accounts.head.name == "Test Account",
                accounts.head.accountType == "checking",
                accounts.head.balance != BigDecimal(0) // Just check that we got a balance value
            )
        },
        test("getAccounts should fail with YnabResourceNotFound when budget not found") {
            // Create a stub backend that returns 404 Not Found
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/non-existent/accounts")
                )
                .thenRespondAdjust(notFoundJson, StatusCode.NotFound)

            val client = YnabClientLive(testConfig, testBackend)

            for
                error <- client.getAccounts("non-existent").flip
            yield assert(error)(isSubtype[YnabResourceNotFound](anything))
        },

        // Category tests
        test("getCategories should return categories for a budget") {
            // Create a stub backend that returns categories
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/budget-1/categories")
                )
                .thenRespondAdjust(categoriesJson)

            val client = YnabClientLive(testConfig, testBackend)

            for
                categories <- client.getCategories("budget-1")
            yield assertTrue(
                categories.size == 1,
                categories.head.id == "category-1",
                categories.head.name == "Test Category",
                categories.head.groupId == "group-1",
                categories.head.groupName == "Test Group",
                categories.head.hidden == false,
                categories.head.deleted == false,
                categories.head.budgeted.isDefined,
                categories.head.activity.isDefined,
                categories.head.balance.isDefined
            )
        },
        test("getCategoryGroups should return category groups for a budget") {
            // Create a stub backend that returns categories
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/budget-1/categories")
                )
                .thenRespondAdjust(categoriesJson)

            val client = YnabClientLive(testConfig, testBackend)

            for
                groups <- client.getCategoryGroups("budget-1")
            yield assertTrue(
                groups.size == 1,
                groups.head.id == "group-1",
                groups.head.name == "Test Group",
                groups.head.hidden == false,
                groups.head.deleted == false
            )
        },

        // Transaction tests
        test("createTransaction should create a transaction and return its ID") {
            // Create a stub backend that handles transaction creation
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/budget-1/transactions") &&
                        r.method == Method.POST
                )
                .thenRespondAdjust(createTransactionJson)

            val client = YnabClientLive(testConfig, testBackend)

            for
                transactionId <- client.createTransaction("budget-1", sampleTransaction)
            yield assertTrue(transactionId == "transaction-1")
        },
        test("createTransactions should create multiple transactions") {
            // Create a stub backend that handles batch transaction creation
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/budget-1/transactions/bulk") &&
                        r.method == Method.POST
                )
                .thenRespondAdjust(createTransactionsJson)

            val client = YnabClientLive(testConfig, testBackend)

            for
                result <- client.createTransactions("budget-1", Seq(sampleTransaction))
            yield assertTrue(
                result.size == 1,
                result.keys.head == sampleTransaction,
                result.values.head == "transaction-1"
            )
        },
        test("createTransaction should fail with error on bad request") {
            // Create a stub backend that returns 400 Bad Request
            val testBackend = HttpClientZioBackend.stub
                .whenRequestMatches(r =>
                    r.uri.toString.contains("/budgets/budget-1/transactions") &&
                        r.method == Method.POST
                )
                .thenRespondAdjust(badRequestJson, StatusCode.BadRequest)

            val client = YnabClientLive(testConfig, testBackend)

            for
                error <- client.createTransaction("budget-1", sampleTransaction).flip
            yield assertTrue(error.isInstanceOf[Throwable])
        }
    )
end YnabClientSpec
