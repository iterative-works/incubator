package works.iterative.incubator.ynab

import zio.*

/**
 * Configuration for YNAB API integration
 *
 * This model contains the necessary configuration for connecting to the YNAB API,
 * including the API token and selected budget ID.
 */
case class YnabConfig(
    token: SecretApiToken,
    selectedBudgetId: Option[String] = None,
    apiUrl: String = "https://api.youneedabudget.com/v1"
)

/**
 * Secret API Token wrapper
 *
 * Provides a secure wrapper for the API token to prevent accidental logging or exposure.
 * Overrides toString to hide the actual token value in logs and debug output.
 */
case class SecretApiToken(value: String):
    override def toString: String = "YNAB_API_TOKEN_REDACTED"

/**
 * YNAB Configuration Service
 *
 * Service for managing YNAB configuration, including loading from environment
 * variables or secure storage, and updating configuration.
 */
trait YnabConfigService:
    /**
     * Get the current YNAB configuration
     *
     * @return The current YNAB configuration or None if not configured
     */
    def getConfig: Task[Option[YnabConfig]]
    
    /**
     * Save a new YNAB configuration
     *
     * @param config The configuration to save
     * @return Unit indicating successful save
     */
    def saveConfig(config: YnabConfig): Task[Unit]
    
    /**
     * Update the selected budget ID
     *
     * @param budgetId The budget ID to select
     * @return The updated configuration
     */
    def selectBudget(budgetId: String): Task[YnabConfig]
    
    /**
     * Create a configuration from an API token
     *
     * @param token The API token string
     * @return A new configuration with the given token
     */
    def createFromToken(token: String): Task[YnabConfig]

/**
 * YNAB Configuration Error
 *
 * Represents errors that can occur when working with YNAB configuration
 */
sealed trait YnabConfigError extends Throwable
case class YnabNotConfigured() extends YnabConfigError
case class YnabInvalidToken(message: String) extends YnabConfigError
case class YnabConfigSaveError(cause: Throwable) extends YnabConfigError