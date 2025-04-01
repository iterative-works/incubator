package works.iterative.incubator.ynab

import zio.*
import java.time.LocalDate

/**
 * YNAB Service
 *
 * Service interface for interacting with YNAB.
 * This service provides access to YNAB budgets, accounts, categories, and transactions.
 */
trait YnabService:
    /**
     * Verify the connection to YNAB using the configured API token
     *
     * @return true if the connection is successful, false otherwise
     */
    def verifyConnection(): Task[Boolean]
    
    /**
     * Get all budgets available to the configured API token
     *
     * @return A sequence of YNAB budgets
     */
    def getBudgets(): Task[Seq[YnabBudget]]
    
    /**
     * Get all accounts in the selected budget
     *
     * @return A sequence of YNAB accounts
     */
    def getAccounts(): Task[Seq[YnabAccount]]
    
    /**
     * Get all category groups in the selected budget
     *
     * @return A sequence of YNAB category groups
     */
    def getCategoryGroups(): Task[Seq[YnabCategoryGroup]]
    
    /**
     * Get all categories in the selected budget
     *
     * @return A sequence of YNAB categories with their groups
     */
    def getCategories(): Task[Seq[YnabCategory]]
    
    /**
     * Create a transaction in YNAB
     *
     * @param transaction The transaction to create
     * @return The ID of the created transaction
     */
    def createTransaction(transaction: YnabTransaction): Task[String]
    
    /**
     * Create multiple transactions in YNAB
     *
     * @param transactions The transactions to create
     * @return A map of transaction IDs to their created IDs
     */
    def createTransactions(transactions: Seq[YnabTransaction]): Task[Map[YnabTransaction, String]]

/**
 * YNAB Transaction Import Service
 *
 * Service for importing transactions to YNAB.
 * This service handles the mapping from source transactions to YNAB transactions
 * and handles submission to YNAB.
 */
trait YnabTransactionImportService:
    /**
     * Import transactions to YNAB
     *
     * @param transactions The transactions to import
     * @param sourceAccountId The source account ID
     * @return A map of transaction IDs to their import status
     */
    def importTransactions(
        transactions: Seq[YnabTransaction], 
        sourceAccountId: String
    ): Task[Map[String, YnabTransactionImportResult]]

/**
 * YNAB Transaction Import Result
 * 
 * Represents the result of importing a transaction to YNAB
 */
sealed trait YnabTransactionImportResult
case class YnabTransactionImportSuccess(ynabTransactionId: String) extends YnabTransactionImportResult
case class YnabTransactionImportError(error: Throwable) extends YnabTransactionImportResult

/**
 * YNAB API Errors
 *
 * Represents errors that can occur when interacting with the YNAB API
 */
sealed trait YnabApiError extends Throwable
case class YnabAuthenticationError(message: String) extends YnabApiError
case class YnabBudgetNotSelected() extends YnabApiError
case class YnabNetworkError(cause: Throwable) extends YnabApiError 
case class YnabResourceNotFound(resource: String, id: String) extends YnabApiError
case class YnabValidationError(message: String) extends YnabApiError