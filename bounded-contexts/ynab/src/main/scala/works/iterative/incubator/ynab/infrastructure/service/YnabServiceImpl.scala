package works.iterative.incubator.ynab.infrastructure.service

import zio.*
import works.iterative.incubator.ynab.domain.model.*
import works.iterative.incubator.ynab.application.service.*
import works.iterative.incubator.ynab.infrastructure.client.YnabClient
import works.iterative.incubator.ynab.infrastructure.config.YnabConfig
import sttp.client4.Backend

/** Implementation of YnabService using the YNAB API client
  *
  * Classification: Infrastructure Service Implementation
  */
case class YnabServiceImpl(client: YnabClient) extends YnabService:
    override def verifyConnection(): Task[Boolean] =
        client.verifyToken()

    override def getBudgets(): Task[Seq[YnabBudget]] =
        client.getBudgets()

    override def getBudgetService(budgetId: String): YnabBudgetService =
        YnabBudgetServiceImpl(client, budgetId)
end YnabServiceImpl

/** Implementation of YnabBudgetService using the YNAB API client
  *
  * Classification: Infrastructure Service Implementation
  */
case class YnabBudgetServiceImpl(client: YnabClient, budgetId: String) extends YnabBudgetService:
    override def getAccounts(): Task[Seq[YnabAccount]] =
        client.getAccounts(budgetId)

    override def getCategoryGroups(): Task[Seq[YnabCategoryGroup]] =
        client.getCategoryGroups(budgetId)

    override def getCategories(): Task[Seq[YnabCategory]] =
        client.getCategories(budgetId)

    override def createTransaction(transaction: YnabTransaction): Task[String] =
        client.createTransaction(budgetId, transaction)

    override def createTransactions(transactions: Seq[YnabTransaction])
        : Task[Map[YnabTransaction, String]] =
        client.createTransactions(budgetId, transactions)
end YnabBudgetServiceImpl

object YnabServiceImpl:
    val layer: ZLayer[YnabClient, Nothing, YnabService] =
        ZLayer {
            for
                client <- ZIO.service[YnabClient]
            yield YnabServiceImpl(client)
        }

    def withConfig(config: YnabConfig): ZLayer[Backend[Task], Throwable, YnabService] =
        YnabClient.withConfig(config) >>> layer

    val live: ZLayer[Any, Throwable, YnabService] =
        YnabClient.live >>> layer

    def liveWithConfig(config: YnabConfig): ZLayer[Any, Throwable, YnabService] =
        YnabClient.liveWithConfig(config) >>> layer
end YnabServiceImpl
