package works.iterative.incubator.ynab.application.port

import zio.*
import works.iterative.incubator.ynab.domain.model.*

/** YNAB Transaction Port
  *
  * Port interface for sending transactions from the Transaction Management context to YNAB. This
  * defines the boundary between the Transaction context and the YNAB context.
  *
  * Application Port: This is a port interface defining cross-context communication.
  */
trait YnabTransactionPort:
    /** Submit a batch of transactions to YNAB
      *
      * @param transactions
      *   The transactions to submit
      * @param sourceAccountId
      *   The ID of the source account
      * @return
      *   A map of transaction IDs to their YNAB transaction IDs
      */
    def submitTransactions(
        transactions: Seq[YnabTransaction],
        sourceAccountId: String
    ): Task[Map[String, YnabTransactionImportResult]]

    /** Link a source account to a YNAB account
      *
      * @param sourceAccountId
      *   The ID of the source account
      * @param ynabAccountId
      *   The ID of the YNAB account
      * @return
      *   Unit indicating successful linking
      */
    def linkSourceAccountToYnab(
        sourceAccountId: String,
        ynabAccountId: String
    ): Task[Unit]

    /** Get all available YNAB accounts for linking
      *
      * @return
      *   A sequence of YNAB accounts
      */
    def getAvailableYnabAccounts(): Task[Seq[YnabAccount]]
end YnabTransactionPort
