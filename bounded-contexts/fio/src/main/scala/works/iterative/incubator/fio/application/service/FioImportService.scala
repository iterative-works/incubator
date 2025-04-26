package works.iterative.incubator.fio.application.service

import zio.Task
import java.time.LocalDate
import works.iterative.incubator.fio.domain.model.FioAccount

/** Service interface for importing transactions from Fio Bank API
  *
  * This service is responsible for fetching transactions from Fio Bank API and converting them to
  * the application's domain model.
  *
  * Classification: Application Service
  */
trait FioImportService:
    /** Import transactions from Fio Bank API for a given date range
      *
      * @param from
      *   Start date for transaction import (inclusive)
      * @param to
      *   End date for transaction import (inclusive)
      * @param accountId
      *   Optional account ID to limit import to a specific account (imports all accounts if None)
      * @return
      *   Number of transactions imported
      */
    def importFioTransactions(
        from: LocalDate,
        to: LocalDate,
        accountId: Option[Long] = None
    ): Task[Int]

    /** Import transactions from Fio Bank API for a specific account
      *
      * @param from
      *   Start date for transaction import (inclusive)
      * @param to
      *   End date for transaction import (inclusive)
      * @param accountId
      *   The Fio account ID to import for
      * @return
      *   Number of transactions imported
      */
    def importTransactionsForAccount(
        from: LocalDate,
        to: LocalDate,
        accountId: Long
    ): Task[Int] = importFioTransactions(from, to, Some(accountId))

    /** Import transactions from Fio Bank API for a given date range with specific token
      *
      * @param token
      *   API token for Fio Bank
      * @param from
      *   Start date for transaction import (inclusive)
      * @param to
      *   End date for transaction import (inclusive)
      * @param sourceAccountId
      *   Source account ID to associate with imported transactions
      * @return
      *   Number of transactions imported
      */
    def importTransactionsWithToken(
        token: String,
        from: LocalDate,
        to: LocalDate,
        sourceAccountId: Long
    ): Task[Int]

    /** Import new transactions from Fio Bank API since the last import
      *
      * This method uses Fio Bank's "last" endpoint which returns all transactions that have not
      * been fetched yet.
      *
      * @param accountId
      *   Optional account ID to limit import to a specific account (imports all accounts if None)
      * @return
      *   Number of transactions imported
      */
    def importNewTransactions(accountId: Option[Long] = None): Task[Int]

    /** Import new transactions from Fio Bank API for a specific account
      *
      * @param accountId
      *   The Fio account ID to import for
      * @return
      *   Number of transactions imported
      */
    def importNewTransactionsForAccount(accountId: Long): Task[Int] =
        importNewTransactions(Some(accountId))

    /** Legacy method to maintain compatibility with TransactionImportService interface
      */
    def importNewTransactionsForAccount(accountId: Option[Long] = None): Task[Int] =
        importNewTransactions(accountId)

    /** Import new transactions from Fio Bank API with specific token
      *
      * This method uses Fio Bank's "last" endpoint which returns all transactions that have not
      * been fetched yet using this token.
      *
      * @param token
      *   API token for Fio Bank
      * @param sourceAccountId
      *   Source account ID to associate with imported transactions
      * @return
      *   Number of transactions imported
      */
    def importNewTransactionsWithToken(
        token: String,
        sourceAccountId: Long
    ): Task[Int]

    /** Get the available Fio Bank accounts
      *
      * @return
      *   List of source account IDs that have Fio Bank configuration
      */
    def getFioSourceAccounts(): Task[List[Long]]

    /** Get all Fio Bank accounts
      *
      * @return
      *   List of FioAccount objects with their configuration
      */
    def getFioAccounts(): Task[List[FioAccount]]

    /** Get a specific Fio Bank account by ID
      *
      * @param id
      *   ID of the Fio account to retrieve
      * @return
      *   The FioAccount if found
      */
    def getFioAccount(id: Long): Task[Option[FioAccount]]

    /** Get a Fio Bank account by source account ID
      *
      * @param sourceAccountId
      *   Source account ID to retrieve Fio account for
      * @return
      *   The FioAccount if found
      */
    def getFioAccountBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]]
end FioImportService
