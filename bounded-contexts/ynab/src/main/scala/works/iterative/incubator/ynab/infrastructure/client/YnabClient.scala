package works.iterative.incubator.ynab.infrastructure.client

import zio.*
import zio.json.*
import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.{StatusCode, Uri}
import works.iterative.incubator.ynab.domain.model.*
import works.iterative.incubator.ynab.infrastructure.config.YnabConfig
import YnabCodecs.given

/** HTTP client for YNAB API
  *
  * This client handles communication with YNAB's REST API, providing methods to access budgets,
  * accounts, categories, and transactions.
  *
  * Classification: Infrastructure Client
  */
trait YnabClient:
    /** Verify that the API token is valid
      *
      * @return
      *   True if the token is valid, false otherwise
      */
    def verifyToken(): Task[Boolean]

    /** Get all budgets for the authenticated user
      *
      * @return
      *   A sequence of YNAB budgets
      */
    def getBudgets(): Task[Seq[YnabBudget]]

    /** Get all accounts for a specific budget
      *
      * @param budgetId
      *   The ID of the budget to get accounts for
      * @return
      *   A sequence of YNAB accounts
      */
    def getAccounts(budgetId: String): Task[Seq[YnabAccount]]

    /** Get all category groups for a specific budget
      *
      * @param budgetId
      *   The ID of the budget to get category groups for
      * @return
      *   A sequence of YNAB category groups
      */
    def getCategoryGroups(budgetId: String): Task[Seq[YnabCategoryGroup]]

    /** Get all categories for a specific budget
      *
      * @param budgetId
      *   The ID of the budget to get categories for
      * @return
      *   A sequence of YNAB categories
      */
    def getCategories(budgetId: String): Task[Seq[YnabCategory]]

    /** Create a single transaction in a specific budget
      *
      * @param budgetId
      *   The ID of the budget to create the transaction in
      * @param transaction
      *   The transaction to create
      * @return
      *   The ID of the created transaction
      */
    def createTransaction(budgetId: String, transaction: YnabTransaction): Task[String]

    /** Create multiple transactions in a specific budget
      *
      * @param budgetId
      *   The ID of the budget to create the transactions in
      * @param transactions
      *   The transactions to create
      * @return
      *   A map of transactions to their created IDs
      */
    def createTransactions(
        budgetId: String,
        transactions: Seq[YnabTransaction]
    ): Task[Map[YnabTransaction, String]]
end YnabClient

/** Live implementation of YnabClient using sttp
  *
  * Classification: Infrastructure Client Implementation
  */
case class YnabClientLive(config: YnabConfig, backend: Backend[Task]) extends YnabClient:
    private val baseUrl = config.apiUrl
    private val token = config.token.value

    // Add Authorization header to every request
    private def authenticatedRequest = basicRequest.header("Authorization", s"Bearer $token")

    // Handle standard API response with data wrapper
    private def handleResponse[T](response: Response[String])(using
        JsonDecoder[ApiResponse[T]]
    ): Task[T] =
        response.code match
            case StatusCode.Ok =>
                ZIO.fromEither(response.body.fromJson[ApiResponse[T]])
                    .mapError(err => new RuntimeException(s"Failed to parse YNAB response: $err"))
                    .map(_.data)
            case StatusCode.Unauthorized =>
                ZIO.fail(YnabAuthenticationError("Invalid YNAB API token"))
            case StatusCode.NotFound =>
                ZIO.fail(YnabResourceNotFound("resource", s"Not found: ${response.body}"))
            case StatusCode.BadRequest =>
                ZIO.fail(YnabValidationError(s"Bad request: ${response.body}"))
            case StatusCode.Forbidden =>
                ZIO.fail(YnabAuthenticationError(s"Forbidden: ${response.body}"))
            case _ =>
                ZIO.fail(YnabNetworkError(
                    new RuntimeException(s"YNAB API error: ${response.code} - ${response.body}")
                ))

    override def verifyToken(): Task[Boolean] =
        for
            response <- authenticatedRequest
                .get(buildUri("/user"))
                .response(asStringAlways)
                .send(backend)
        yield response.code == StatusCode.Ok
        end for
    end verifyToken

    override def getBudgets(): Task[Seq[YnabBudget]] =
        for
            response <- authenticatedRequest
                .get(buildUri("/budgets"))
                .response(asStringAlways)
                .send(backend)
            result <- handleResponse[BudgetsResponse](response)
        yield result.budgets
        end for
    end getBudgets

    override def getAccounts(budgetId: String): Task[Seq[YnabAccount]] =
        for
            response <- authenticatedRequest
                .get(buildUri(s"/budgets/$budgetId/accounts"))
                .response(asStringAlways)
                .send(backend)
            result <- handleResponse[AccountsResponse](response)
            // Map the API's account model to our domain model
            accounts = result.accounts.map(_.toDomain)
        yield accounts
        end for
    end getAccounts

    override def getCategoryGroups(budgetId: String): Task[Seq[YnabCategoryGroup]] =
        for
            response <- authenticatedRequest
                .get(buildUri(s"/budgets/$budgetId/categories"))
                .response(asStringAlways)
                .send(backend)
            result <- handleResponse[CategoriesResponse](response)
            // Extract category groups from the response
            categoryGroups = result.category_groups.map(group =>
                YnabCategoryGroup(
                    id = group.id,
                    name = group.name,
                    hidden = group.hidden,
                    deleted = group.deleted
                )
            )
        yield categoryGroups
        end for
    end getCategoryGroups

    override def getCategories(budgetId: String): Task[Seq[YnabCategory]] =
        for
            response <- authenticatedRequest
                .get(buildUri(s"/budgets/$budgetId/categories"))
                .response(asStringAlways)
                .send(backend)
            result <- handleResponse[CategoriesResponse](response)
            // Flatten categories from all groups
            categories = result.category_groups.flatMap(group =>
                group.categories.map(cat =>
                    YnabCategory(
                        id = cat.id,
                        name = cat.name,
                        groupId = group.id,
                        groupName = group.name,
                        hidden = cat.hidden,
                        deleted = cat.deleted,
                        budgeted = cat.budgeted.map(b => BigDecimal(b) / 1000), // Convert from milliunits
                        activity = cat.activity.map(a => BigDecimal(a) / 1000), // Convert from milliunits
                        balance = cat.balance.map(b => BigDecimal(b) / 1000)   // Convert from milliunits
                    )
                )
            )
        yield categories
        end for
    end getCategories

    override def createTransaction(budgetId: String, transaction: YnabTransaction): Task[String] =
        val transactionData = TransactionCreateRequest(
            transaction = TransactionDTO.fromDomain(transaction)
        )

        // Send the request with logging
        for
            _ <- Console.printLine(s"Sending transaction: ${transactionData.toJson}").orDie
            response <- authenticatedRequest
                .post(buildUri(s"/budgets/$budgetId/transactions"))
                .body(transactionData.toJson)
                .header("Content-Type", "application/json")
                .response(asStringAlways)
                .send(backend)
            _ <- Console.printLine(s"Response: ${response.body}").orDie // Debug response
            
            // Parse the response directly instead of using handleResponse
            txResult <- if response.code == StatusCode.Created || response.code == StatusCode.Ok then
                ZIO.fromEither(response.body.fromJson[ApiResponse[TransactionResponse]])
                   .mapError(err => new RuntimeException(s"Failed to parse YNAB response: $err"))
                   .map(_.data)
            else
                ZIO.fail(YnabNetworkError(new RuntimeException(
                    s"YNAB API error: ${response.code} - ${response.body}"
                )))
            
            transactionId = if txResult.transaction_ids.nonEmpty then 
                txResult.transaction_ids.head
            else
                txResult.transaction.id
                
            _ <- Console.printLine(s"Created transaction with ID: $transactionId").orDie
        yield transactionId
    end createTransaction

    override def createTransactions(
        budgetId: String,
        transactions: Seq[YnabTransaction]
    ): Task[Map[YnabTransaction, String]] =
        val transactionsData = TransactionsCreateRequest(
            transactions = transactions.map(TransactionDTO.fromDomain)
        )

        for
            response <- authenticatedRequest
                .post(buildUri(s"/budgets/$budgetId/transactions/bulk"))
                .body(transactionsData.toJson)
                .response(asStringAlways)
                .send(backend)
            result <- handleResponse[TransactionsResponse](response)
            // Create a map of original transactions to their IDs
            transactionMap = transactions.zip(result.transactions.map(_.id)).toMap
        yield transactionMap
        end for
    end createTransactions

    private def buildUri(path: String): Uri =
        Uri.parse(s"$baseUrl$path")
            .getOrElse(throw new IllegalArgumentException(s"Invalid URL: $baseUrl$path"))
end YnabClientLive

object YnabClient:
    val layer: ZLayer[Backend[Task], Config.Error, YnabClient] =
        ZLayer {
            for
                config <- ZIO.config[YnabConfig]
                backend <- ZIO.service[Backend[Task]]
            yield YnabClientLive(config, backend)
        }

    def withConfig(config: YnabConfig): ZLayer[Backend[Task], Nothing, YnabClientLive] = ZLayer {
        for
            backend <- ZIO.service[Backend[Task]]
        yield YnabClientLive(config, backend)
    }

    val live: ZLayer[Any, Throwable, YnabClient] =
        HttpClientZioBackend.layer() >>> layer

    def liveWithConfig(config: YnabConfig): ZLayer[Any, Throwable, YnabClient] =
        HttpClientZioBackend.layer() >>> withConfig(config)
end YnabClient
