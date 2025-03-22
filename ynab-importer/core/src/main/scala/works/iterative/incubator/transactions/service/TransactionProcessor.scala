package works.iterative.incubator.transactions
package service

import zio.*

/** Service responsible for processing imported transactions
  *
  * This service is responsible for:
  *   1. Creating initial processing state records for imported transactions 2. Applying
  *      categorization and processing to transactions 3. Managing the transaction processing
  *      lifecycle
  */
trait TransactionProcessor:
    /** Initialize processing state for newly imported transactions
      *
      * This method scans for transactions that don't have a processing state and creates the
      * initial processing state for them.
      *
      * @return
      *   The number of transactions initialized
      */
    def initializeNewTransactions(): Task[Int]

    /** Process transactions that are in the imported state
      *
      * This method applies categorization and other processing to transactions that are in the
      * imported state, moving them to the categorized state.
      *
      * @return
      *   The number of transactions processed
      */
    def processImportedTransactions(): Task[Int]

    /** Find transactions ready for submission to YNAB
      *
      * @return
      *   Transactions that are ready to be submitted to YNAB
      */
    def findTransactionsReadyForSubmission(): Task[Seq[(Transaction, TransactionProcessingState)]]

    /** Mark transactions as submitted to YNAB
      *
      * @param transactionIds
      *   The IDs of the transactions that were submitted
      * @param ynabIds
      *   The YNAB transaction IDs assigned to them
      * @param ynabAccountId
      *   The YNAB account ID they were submitted to
      * @return
      *   The number of transactions marked as submitted
      */
    def markTransactionsAsSubmitted(
        transactionIds: Seq[TransactionId],
        ynabIds: Seq[String],
        ynabAccountId: String
    ): Task[Int]
end TransactionProcessor
