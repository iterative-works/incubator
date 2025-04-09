package works.iterative.incubator.ynab.application.service

import zio.*
import works.iterative.incubator.ynab.domain.model.*

/** 
 * YNAB Service
 *
 * Service interface for interacting with YNAB. This service provides access to YNAB budgets
 * and the ability to create budget-specific services.
 *
 * Application Service: This is a service interface defining operations provided by the YNAB integration.
 */
trait YnabService:
    /** Verify the connection to YNAB using the configured API token
      *
      * @return
      *   true if the connection is successful, false otherwise
      */
    def verifyConnection(): Task[Boolean]

    /** Get all budgets available to the configured API token
      *
      * @return
      *   A sequence of YNAB budgets
      */
    def getBudgets(): Task[Seq[YnabBudget]]
    
    /** Get a budget-specific service for a given budget ID
      *
      * @param budgetId
      *   The ID of the budget to create a service for
      * @return
      *   A budget-specific service
      */
    def getBudgetService(budgetId: String): YnabBudgetService
end YnabService

/**
 * YNAB Budget Service
 *
 * Service interface for interacting with a specific YNAB budget. This service provides
 * access to accounts, categories, and transactions within a specific budget.
 *
 * Application Service: This is a service interface for budget-specific operations.
 */
trait YnabBudgetService:
    /** Get all accounts in the budget
      *
      * @return
      *   A sequence of YNAB accounts
      */
    def getAccounts(): Task[Seq[YnabAccount]]

    /** Get all category groups in the budget
      *
      * @return
      *   A sequence of YNAB category groups
      */
    def getCategoryGroups(): Task[Seq[YnabCategoryGroup]]

    /** Get all categories in the budget
      *
      * @return
      *   A sequence of YNAB categories with their groups
      */
    def getCategories(): Task[Seq[YnabCategory]]

    /** Create a transaction in the budget
      *
      * @param transaction
      *   The transaction to create
      * @return
      *   The ID of the created transaction
      */
    def createTransaction(transaction: YnabTransaction): Task[String]

    /** Create multiple transactions in the budget
      *
      * @param transactions
      *   The transactions to create
      * @return
      *   A map of transaction IDs to their created IDs
      */
    def createTransactions(transactions: Seq[YnabTransaction]): Task[Map[YnabTransaction, String]]
end YnabBudgetService