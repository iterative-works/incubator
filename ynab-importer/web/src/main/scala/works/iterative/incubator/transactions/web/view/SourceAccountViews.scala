package works.iterative.incubator.transactions.web.view

import scalatags.Text.TypedTag
import works.iterative.incubator.transactions.domain.model.SourceAccount

/**
 * Interface for Source Account view components.
 * This trait defines the methods for rendering source account related views.
 *
 * Classification: Web View Interface
 */
trait SourceAccountViews {
  /**
   * View for displaying an error when an account is not found.
   *
   * @param accountId The ID of the account that was not found
   * @return HTML fragment for the error message
   */
  def accountNotFound(accountId: Long): TypedTag[String]
  
  /**
   * View for displaying a list of source accounts.
   *
   * @param accounts List of source accounts to display
   * @param selectedStatus Current filter status (all, active, inactive)
   * @return HTML fragment for the account list page
   */
  def sourceAccountList(accounts: Seq[SourceAccount], selectedStatus: String = "active"): TypedTag[String]
  
  /**
   * View for displaying a form to create or edit a source account.
   *
   * @param account Optional existing account for editing, or None for creating new
   * @return HTML fragment for the account form
   */
  def sourceAccountForm(account: Option[SourceAccount] = None): TypedTag[String]
  
  /**
   * View for displaying detailed information about a source account.
   *
   * @param account The source account to display details for
   * @return HTML fragment for the account detail page
   */
  def sourceAccountDetail(account: SourceAccount): TypedTag[String]
}