package works.iterative.incubator.transactions.views

import scalatags.Text.TypedTag

/**
 * Interface for Transaction view components.
 * This trait defines the methods for rendering transaction related views.
 */
trait TransactionViews {
  /**
   * View for displaying a list of transactions with their processing states.
   *
   * @param transactions List of transactions with their processing states
   * @param importStatus Optional status message from a recent import operation
   * @return HTML fragment for the transaction list page
   */
  def transactionList(
    transactions: Seq[TransactionWithState],
    importStatus: Option[String] = None
  ): TypedTag[String]
}