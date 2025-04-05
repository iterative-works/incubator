package works.iterative.incubator.ynab

/** YNAB Domain Models
  *
  * @deprecated
  *   This file is maintained for backward compatibility. Use the individual domain model classes in
  *   the domain.model package instead.
  */

// Re-export domain models for backwards compatibility
type YnabBudget = domain.model.YnabBudget
val YnabBudget = domain.model.YnabBudget

type YnabAccount = domain.model.YnabAccount
val YnabAccount = domain.model.YnabAccount

type YnabCategoryGroup = domain.model.YnabCategoryGroup
val YnabCategoryGroup = domain.model.YnabCategoryGroup

type YnabCategory = domain.model.YnabCategory
val YnabCategory = domain.model.YnabCategory

type YnabTransaction = domain.model.YnabTransaction
val YnabTransaction = domain.model.YnabTransaction

type YnabApiError = domain.model.YnabApiError
val YnabAuthenticationError = domain.model.YnabAuthenticationError
val YnabBudgetNotSelected = domain.model.YnabBudgetNotSelected
val YnabNetworkError = domain.model.YnabNetworkError
val YnabResourceNotFound = domain.model.YnabResourceNotFound
val YnabValidationError = domain.model.YnabValidationError

type YnabTransactionImportResult = domain.model.YnabTransactionImportResult
val YnabTransactionImportSuccess = domain.model.YnabTransactionImportSuccess
val YnabTransactionImportError = domain.model.YnabTransactionImportError
