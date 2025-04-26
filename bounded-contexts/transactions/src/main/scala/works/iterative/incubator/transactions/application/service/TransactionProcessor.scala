package works.iterative.incubator.transactions.application.service

import zio.*
import works.iterative.incubator.transactions.domain.model.{
    Transaction,
    TransactionId,
    TransactionProcessingState
}

/** Service responsible for processing imported transactions
  *
  * This service is responsible for:
  *   1. Creating initial processing state records for imported transactions 2. Applying
  *      categorization and processing to transactions 3. Managing the transaction processing
  *      lifecycle
  *
  * Classification: Application Service
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
      * @param submissionData
      *   Pairs of transaction IDs and their corresponding YNAB transaction IDs
      * @param ynabAccountId
      *   The YNAB account ID they were submitted to
      * @return
      *   The number of transactions marked as submitted
      */
    def markTransactionsAsSubmitted(
        submissionData: Seq[(TransactionId, String)],
        ynabAccountId: String
    ): Task[Int]
end TransactionProcessor
