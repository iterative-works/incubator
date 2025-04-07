package works.iterative.incubator.transactions.web.view

import works.iterative.incubator.transactions.domain.model.{Transaction, TransactionProcessingState, TransactionStatus}
import works.iterative.incubator.transactions.domain.model.TransactionId

/**
 * Case class to combine Transaction with its processing state for the UI.
 *
 * Classification: Web View Model
 */
case class TransactionWithState(
  transaction: Transaction,
  state: Option[TransactionProcessingState]
) {
  // Convenience accessors to avoid lots of transaction.x and state.get.y in the view
  def id = transaction.id
  def date = transaction.date
  def amount = transaction.amount
  def currency = transaction.currency
  def counterAccount = transaction.counterAccount
  def counterBankName = transaction.counterBankName
  def userIdentification = transaction.userIdentification
  def message = transaction.message

  // Processing state related accessors with fallbacks
  def status = state.map(_.status).getOrElse(TransactionStatus.Imported)
  def suggestedPayeeName = state.flatMap(_.suggestedPayeeName)
  def suggestedCategory = state.flatMap(_.suggestedCategory)
  def suggestedMemo = state.flatMap(_.suggestedMemo)
  def overridePayeeName = state.flatMap(_.overridePayeeName)
  def overrideCategory = state.flatMap(_.overrideCategory)
  def overrideMemo = state.flatMap(_.overrideMemo)
  def effectivePayeeName = state.flatMap(_.effectivePayeeName)
  def effectiveCategory = state.flatMap(_.effectiveCategory)
  def effectiveMemo = state.flatMap(_.effectiveMemo)
}