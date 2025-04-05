package works.iterative.incubator.ynab.application.service

import zio.*
import works.iterative.incubator.ynab.domain.model.*

/** 
 * YNAB Transaction Import Service
 *
 * Service for importing transactions to YNAB. This service handles the mapping from source
 * transactions to YNAB transactions and handles submission to YNAB.
 *
 * Application Service: This is a service interface defining transaction import operations.
 */
trait YnabTransactionImportService:
    /** Import transactions to YNAB
      *
      * @param transactions
      *   The transactions to import
      * @param sourceAccountId
      *   The source account ID
      * @return
      *   A map of transaction IDs to their import status
      */
    def importTransactions(
        transactions: Seq[YnabTransaction],
        sourceAccountId: String
    ): Task[Map[String, YnabTransactionImportResult]]
end YnabTransactionImportService