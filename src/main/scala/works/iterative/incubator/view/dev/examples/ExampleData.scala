package works.iterative.incubator.view.dev.examples

import works.iterative.incubator.transactions._
import works.iterative.incubator.transactions.views.TransactionWithState
import scala.collection.immutable.Map

/**
 * Container for example data used in view previews.
 * Provides a flexible structure for different types of test data.
 */
case class ExampleData(
  sourceAccounts: List[SourceAccount] = List.empty,
  transactions: List[Transaction] = List.empty,
  processingStates: List[TransactionProcessingState] = List.empty,
  transactionsWithState: List[TransactionWithState] = List.empty,
  errors: List[String] = List.empty,
  warnings: List[String] = List.empty,
  formValues: Map[String, String] = Map.empty
)

object ExampleData {
  val empty: ExampleData = ExampleData()
}