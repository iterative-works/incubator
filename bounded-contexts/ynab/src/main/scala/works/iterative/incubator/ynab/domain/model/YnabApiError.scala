package works.iterative.incubator.ynab.domain.model

/**
 * YNAB API Errors
 *
 * Represents errors that can occur when interacting with the YNAB API
 *
 * Domain Model: These are domain exceptions representing various error conditions.
 */
sealed trait YnabApiError extends Throwable
case class YnabAuthenticationError(message: String) extends YnabApiError
case class YnabBudgetNotSelected() extends YnabApiError
case class YnabNetworkError(cause: Throwable) extends YnabApiError
case class YnabResourceNotFound(resource: String, id: String) extends YnabApiError
case class YnabValidationError(message: String) extends YnabApiError