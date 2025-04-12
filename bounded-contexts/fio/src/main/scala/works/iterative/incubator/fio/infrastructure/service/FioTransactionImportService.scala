package works.iterative.incubator.fio.infrastructure.service

import zio.*
import java.time.LocalDate
import java.time.Instant
import works.iterative.incubator.fio.application.service.FioImportService
import works.iterative.incubator.fio.infrastructure.client.FioClient
import works.iterative.incubator.fio.infrastructure.config.FioConfig
import works.iterative.incubator.fio.infrastructure.security.FioTokenManager
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.transactions.domain.repository.{
    TransactionRepository,
    SourceAccountRepository
}
import works.iterative.incubator.transactions.domain.model.{Transaction, TransactionId}
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery
import works.iterative.incubator.transactions.application.service.TransactionImportService

/** Implementation of FioImportService that imports transactions from Fio Bank API and converts them
  * to the application's domain model.
  *
  * This service is responsible for:
  *   1. Fetching transactions from Fio Bank API using FioClient 2. Resolving source account IDs for
  *      the transactions 3. Converting Fio Bank transaction format to our domain model 4. Saving
  *      the transactions to the repository
  *
  * Classification: Infrastructure Service Implementation
  */
class FioTransactionImportService(
    fioClient: FioClient,
    transactionRepository: TransactionRepository,
    sourceAccountRepository: SourceAccountRepository,
    fioAccountRepository: Option[FioAccountRepository] = None,
    importStateRepository: Option[FioImportStateRepository] = None,
    tokenManager: Option[FioTokenManager] = None,
    config: Option[FioConfig] = None
) extends FioImportService, TransactionImportService:

    // Simple in-memory cache using ZIO Ref
    private val sourceAccountCache: Ref[Map[(String, String), Long]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[(String, String), Long])

    // FioImportService implementation
    def importFioTransactions(
        from: LocalDate,
        to: LocalDate,
        accountId: Option[Long] = None
    ): Task[Int] =
        fioAccountRepository match
            // Use account-based import if FioAccountRepository is available
            case Some(repo) =>
                for
                    // Get all Fio accounts or filter by the specified account ID
                    accounts <- if accountId.isEmpty then repo.getAll()
                    else repo.getById(accountId.get).map(_.toList)

                    // Import transactions for each account
                    results <- ZIO.foreach(accounts) { account =>
                        for
                            // Get decrypted token using token manager if available
                            tokenStr <- getTokenForAccount(account.id, Some(account.token))
                            count <- importTransactionsWithToken(
                                tokenStr,
                                from,
                                to,
                                account.sourceAccountId
                            )
                        yield count
                    }
                yield results.sum

            // Fall back to legacy mode using config token if no repository
            case None =>
                for
                    token <- getDefaultToken()
                    sourceAccounts <- getFioSourceAccounts()
                    sourceId <- accountId match
                        case Some(id) if sourceAccounts.contains(id) => ZIO.succeed(id)
                        case Some(id) =>
                            ZIO.fail(new RuntimeException(s"Invalid source account ID: $id"))
                        case None if sourceAccounts.isEmpty =>
                            ZIO.fail(new RuntimeException("No source accounts configured"))
                        case None => ZIO.succeed(sourceAccounts.head)
                    count <- importTransactionsWithToken(token, from, to, sourceId)
                yield count

    override def importTransactionsWithToken(
        token: String,
        from: LocalDate,
        to: LocalDate,
        sourceAccountId: Long
    ): Task[Int] =
        for
            // Clear the cache at the beginning of each import batch
            _ <- sourceAccountCache.set(Map.empty)
            _ <- ZIO.logInfo(
                s"Importing transactions from $from to $to for source account $sourceAccountId"
            )
            response <- fioClient.fetchTransactions(token, from, to)
            transactions <- mapFioTransactionsToModel(response, Some(sourceAccountId))
            _ <- ZIO.foreachDiscard(transactions) { transaction =>
                // Only save the immutable transaction
                transactionRepository.save(transaction.id, transaction)
            }

            // Update the import state with the latest transaction ID if available
            _ <- updateImportState(transactions, response)

            // Update last fetched ID in Fio account if repository is available
            _ <- updateFioAccountLastFetched(sourceAccountId, response)
        yield transactions.size

    override def importNewTransactionsForAccount(accountId: Option[Long] = None): Task[Int] =
        fioAccountRepository match
            // Use account-based import if FioAccountRepository is available
            case Some(repo) =>
                for
                    // Get all Fio accounts or filter by the specified account ID
                    accounts <- if accountId.isEmpty then
                        repo.getAll()
                    else
                        repo.getById(accountId.get).map(_.toList)

                    // Import new transactions for each account
                    results <- ZIO.foreach(accounts) { account =>
                        for
                            // Get decrypted token using token manager if available
                            tokenStr <- getTokenForAccount(account.id, Some(account.token))
                            count <- importNewTransactionsWithToken(
                                tokenStr,
                                account.sourceAccountId
                            )
                        yield count
                    }
                yield results.sum

            // Fall back to legacy mode if no repository
            case None =>
                for
                    // Get all Fio source accounts
                    sourceAccounts <- getFioSourceAccounts()

                    // Filter by specified account ID if provided
                    filteredAccounts <- accountId match
                        case Some(id) if sourceAccounts.contains(id) => ZIO.succeed(List(id))
                        case Some(id) =>
                            ZIO.fail(new RuntimeException(s"Invalid source account ID: $id"))
                        case None => ZIO.succeed(sourceAccounts)

                    // For each source account, get the token and import new transactions
                    results <- ZIO.foreach(filteredAccounts) { sourceId =>
                        for
                            token <- getTokenForSourceAccount(sourceId)
                            count <- importNewTransactionsWithToken(token, sourceId)
                        yield count
                    }
                yield results.sum

    // Implementing default method from trait to avoid duplicate code
    override def importNewTransactionsForAccount(accountId: Long): Task[Int] =
        importNewTransactions(Some(accountId))

    override def importNewTransactionsWithToken(
        token: String,
        sourceAccountId: Long
    ): Task[Int] =
        for
            // Clear the cache at the beginning of each import batch
            _ <- sourceAccountCache.set(Map.empty)
            _ <- ZIO.logInfo(
                s"Importing new transactions for source account $sourceAccountId using last endpoint"
            )
            response <- fioClient.fetchNewTransactions(token)
            transactions <- mapFioTransactionsToModel(response, Some(sourceAccountId))
            _ <- ZIO.foreachDiscard(transactions) { transaction =>
                // Only save the immutable transaction
                transactionRepository.save(transaction.id, transaction)
            }

            // Update the import state with the latest transaction ID if available
            _ <- updateImportState(transactions, response)

            // Update last fetched ID in Fio account if repository is available
            _ <- updateFioAccountLastFetched(sourceAccountId, response)
        yield transactions.size

    override def getFioSourceAccounts(): Task[List[Long]] =
        fioAccountRepository match
            case Some(repo) => repo.getAll().map(_.map(_.sourceAccountId))
            case None => sourceAccountRepository.find(SourceAccountQuery()).map(_.map(_.id).toList)

    override def getFioAccounts(): Task[List[FioAccount]] =
        fioAccountRepository match
            case Some(repo) => repo.getAll()
            case None       => ZIO.succeed(List.empty)

    override def getFioAccount(id: Long): Task[Option[FioAccount]] =
        fioAccountRepository match
            case Some(repo) => repo.getById(id)
            case None       => ZIO.succeed(None)

    override def getFioAccountBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]] =
        fioAccountRepository match
            case Some(repo) => repo.getBySourceAccountId(sourceAccountId)
            case None       => ZIO.succeed(None)

    /** Legacy method to maintain compatibility with TransactionImportService
      *
      * @param lastId
      *   The last transaction ID processed
      * @return
      *   Number of transactions imported
      */
    override def importNewTransactions(lastId: Option[Long]): Task[Int] =
        for
            sourceAccounts <- getFioSourceAccounts()
            sourceId <- if sourceAccounts.isEmpty then
                ZIO.fail(new RuntimeException("No source accounts configured"))
            else
                ZIO.succeed(sourceAccounts.head)
            token <- getTokenForSourceAccount(sourceId)
            count <- importNewTransactionsWithToken(token, sourceId)
        yield count

    /** Import transactions for a specific account ID
      *
      * @param from
      *   Start date for the import
      * @param to
      *   End date for the import
      * @param accountId
      *   The Fio account ID to import for
      * @return
      *   Number of transactions imported
      */
    override def importTransactionsForAccount(
        from: LocalDate,
        to: LocalDate,
        accountId: Long
    ): Task[Int] =
        importFioTransactions(from, to, Some(accountId))

    /** Implementation of TransactionImportService.importTransactions
      *
      * @param from
      *   Start date for the import
      * @param to
      *   End date for the import
      * @return
      *   Number of transactions imported
      */
    override def importTransactions(from: LocalDate, to: LocalDate): Task[Int] =
        importFioTransactions(from, to, None)

    /** Get the token for a specific account, using the token manager if available
      *
      * @param accountId
      *   The Fio account ID
      * @param fallbackToken
      *   Fallback encrypted token to use if token manager is not available
      * @return
      *   Decrypted token string
      */
    private def getTokenForAccount(accountId: Long, fallbackToken: Option[String] = None): Task[String] =
        tokenManager match
            case Some(manager) =>
                manager.getToken(accountId).flatMap {
                    case Some(token) => ZIO.succeed(token)
                    case None => fallbackToken match
                        case Some(token) => ZIO.succeed(token)
                        case None => ZIO.fail(new RuntimeException(s"No token found for account $accountId"))
                }
            case None =>
                fallbackToken match
                    case Some(token) => ZIO.succeed(token)
                    case None => ZIO.fail(new RuntimeException(s"No token found for account $accountId"))
                    
    /** Get the token for a specific source account, using the token manager if available
      *
      * @param sourceAccountId
      *   The source account ID
      * @return
      *   Decrypted token string
      */
    private def getTokenForSourceAccount(sourceAccountId: Long): Task[String] =
        tokenManager match
            case Some(manager) =>
                manager.getTokenBySourceAccountId(sourceAccountId).flatMap {
                    case Some(token) => ZIO.succeed(token)
                    case None => getDefaultToken()
                }
            case None =>
                // If no token manager is available, try to get the account from repository
                fioAccountRepository match
                    case Some(repo) =>
                        repo.getBySourceAccountId(sourceAccountId).flatMap {
                            case Some(account) => ZIO.succeed(account.token)
                            case None => getDefaultToken()
                        }
                    case None => getDefaultToken()

    /** Get the default token from config if available
      *
      * @return
      *   Token string
      */
    private def getDefaultToken(): Task[String] =
        config.flatMap(_.defaultToken) match
            case Some(token) => ZIO.succeed(token)
            case None => ZIO.fail(new RuntimeException(
                    "No default Fio API token configured and no account repository available"
                ))

    /** Map Fio transactions to our domain model
      *
      * @param response
      *   The Fio API response
      * @param overrideSourceAccountId
      *   Optional source account ID to use instead of looking it up
      * @return
      *   List of domain Transaction objects
      */
    private def mapFioTransactionsToModel(
        response: FioResponse,
        overrideSourceAccountId: Option[Long] = None
    ): Task[List[Transaction]] =
        overrideSourceAccountId match
            case Some(sourceId) =>
                // We already have the source account ID
                ZIO.succeed(mapTransactions(response, sourceId))

            case None =>
                // Need to look up the source account
                val accountId = response.accountStatement.info.accountId
                val bankId = response.accountStatement.info.bankId

                // Try to get the source account ID from the cache first
                val key = (accountId, bankId)

                sourceAccountCache.get.flatMap { cache =>
                    cache.get(key) match
                        case Some(id) =>
                            // Cache hit
                            ZIO.succeed(Some(id))
                        case None =>
                            // Cache miss, query the database and cache the result
                            sourceAccountRepository.find(
                                SourceAccountQuery(
                                    accountId = Some(accountId),
                                    bankId = Some(bankId)
                                )
                            ).flatMap { accounts =>
                                accounts.headOption match
                                    case Some(sourceAccount) =>
                                        // Update cache with the found source account ID
                                        sourceAccountCache.update(_ + (key -> sourceAccount.id))
                                            .as(Some(sourceAccount.id))
                                    case None =>
                                        ZIO.succeed(None)
                            }
                } flatMap {
                    case Some(sourceAccountId) =>
                        // We found the matching source account
                        ZIO.succeed(mapTransactions(response, sourceAccountId))
                    case None =>
                        // No matching source account found - this is an error condition
                        ZIO.fail(new RuntimeException(
                            s"No source account found for account ID $accountId and bank ID $bankId"
                        ))
                }
    end mapFioTransactionsToModel

    /** Maps FIO transaction data to our domain model
      *
      * @param response
      *   The parsed FIO API response
      * @param sourceAccountId
      *   The resolved internal source account ID
      * @return
      *   List of domain Transaction objects
      */
    private def mapTransactions(response: FioResponse, sourceAccountId: Long): List[Transaction] =
        response.accountStatement.transactionList.transaction.map { fioTx =>
            val txId = fioTx.column22.map(_.value.toString).getOrElse(
                throw new RuntimeException("Transaction has no ID")
            )

            // Get date from column0, which is the date field based on example JSON
            val dateStr = fioTx.column0.map(_.value).getOrElse(
                throw new RuntimeException("Transaction has no date")
            )
            // Example date format from JSON: "2025-03-14+0100"
            val date = LocalDate.parse(dateStr.split("\\+").head)

            // Get amount from column1
            val amount = fioTx.column1.map(_.value).getOrElse(
                throw new RuntimeException("Transaction has no amount")
            )

            // Get currency from column14 (based on example JSON)
            val currency = fioTx.column14.map(_.value).getOrElse("CZK")

            // Create the immutable Transaction object
            val transaction = Transaction(
                id = TransactionId(sourceAccountId, txId),
                date = date,
                amount = BigDecimal(amount),
                currency = currency,
                // Counter account is column2
                counterAccount = fioTx.column2.map(_.value),
                // Bank code is column3
                counterBankCode = fioTx.column3.map(_.value),
                // Bank name is column12
                counterBankName = fioTx.column12.map(_.value),
                // Variable symbol is column5
                variableSymbol = fioTx.column5.map(_.value),
                // Constant symbol is column4
                constantSymbol = fioTx.column4.map(_.value),
                // Specific symbol is column6
                specificSymbol = fioTx.column6.map(_.value),
                // User identification is column7
                userIdentification = fioTx.column7.map(_.value),
                // Counter account name is column10
                message = fioTx.column10.map(_.value),
                // Transaction type is column8
                transactionType = fioTx.column8.map(_.value).getOrElse("Unknown"),
                // Comment is column25
                comment = fioTx.column25.map(_.value),
                // Set metadata
                importedAt = Instant.now()
            )

            transaction
        }
    end mapTransactions

    /** Updates the import state with the latest transaction ID Only executes if
      * importStateRepository is available
      */
    private def updateImportState(
        transactions: List[Transaction],
        response: FioResponse
    ): Task[Unit] =
        val maxTransactionId = response.accountStatement.info.idTo

        // Get source account ID from the first transaction if available
        ZIO.whenCase(importStateRepository) {
            case Some(repo) if transactions.nonEmpty =>
                val sourceId = transactions.head.id.sourceAccountId
                val state = FioImportState(
                    sourceAccountId = sourceId,
                    lastTransactionId = Some(maxTransactionId),
                    lastImportTimestamp = Instant.now()
                )
                repo.updateImportState(state)
        }.unit
    end updateImportState

    /** Updates the Fio account's last fetched ID and sync time Only executes if
      * fioAccountRepository is available
      */
    private def updateFioAccountLastFetched(
        sourceAccountId: Long,
        response: FioResponse
    ): Task[Unit] =
        val maxTransactionId = response.accountStatement.info.idTo

        ZIO.whenCase(fioAccountRepository) {
            case Some(repo) =>
                repo.getBySourceAccountId(sourceAccountId).flatMap {
                    case Some(account) =>
                        repo.updateLastFetched(account.id, maxTransactionId, Instant.now())
                    case None =>
                        ZIO.unit
                }
        }.unit
    end updateFioAccountLastFetched
end FioTransactionImportService

object FioTransactionImportService:
    // Layer with all repositories and token manager
    val completeLayer: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioAccountRepository &
            FioImportStateRepository &
            FioTokenManager,
        Config.Error,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                fioAccountRepo <- ZIO.service[FioAccountRepository]
                importStateRepo <- ZIO.service[FioImportStateRepository]
                tokenManager <- ZIO.service[FioTokenManager]
                config <- ZIO.config[FioConfig]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    Some(fioAccountRepo),
                    Some(importStateRepo),
                    Some(tokenManager),
                    Some(config)
                )
            yield service
        }

    // Complete layer without token manager (for backward compatibility)
    val completeLayerNoTokenManager: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioAccountRepository &
            FioImportStateRepository,
        Config.Error,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                fioAccountRepo <- ZIO.service[FioAccountRepository]
                importStateRepo <- ZIO.service[FioImportStateRepository]
                config <- ZIO.config[FioConfig]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    Some(fioAccountRepo),
                    Some(importStateRepo),
                    None,
                    Some(config)
                )
            yield service
        }

    // Layer with token manager and account repository
    val accountLayer: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioAccountRepository &
            FioTokenManager,
        Nothing,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                fioAccountRepo <- ZIO.service[FioAccountRepository]
                tokenManager <- ZIO.service[FioTokenManager]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    Some(fioAccountRepo),
                    None,
                    Some(tokenManager),
                    None
                )
            yield service
        }

    // Layer with account repository (no token manager)
    val accountLayerNoTokenManager: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioAccountRepository,
        Nothing,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                fioAccountRepo <- ZIO.service[FioAccountRepository]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    Some(fioAccountRepo),
                    None,
                    None,
                    None
                )
            yield service
        }

    // Layer with import state repository and token manager
    val legacyLayer: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioImportStateRepository &
            FioTokenManager,
        Config.Error,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                importStateRepo <- ZIO.service[FioImportStateRepository]
                tokenManager <- ZIO.service[FioTokenManager]
                config <- ZIO.config[FioConfig]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    None,
                    Some(importStateRepo),
                    Some(tokenManager),
                    Some(config)
                )
            yield service
        }

    // Legacy layer without token manager
    val legacyLayerNoTokenManager: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioImportStateRepository,
        Config.Error,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                importStateRepo <- ZIO.service[FioImportStateRepository]
                config <- ZIO.config[FioConfig]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    None,
                    Some(importStateRepo),
                    None,
                    Some(config)
                )
            yield service
        }

    // Minimal layer with token manager
    val minimalLayer: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioTokenManager,
        Config.Error,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                tokenManager <- ZIO.service[FioTokenManager]
                config <- ZIO.config[FioConfig]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    None,
                    None,
                    Some(tokenManager),
                    Some(config)
                )
            yield service
        }
        
    // Minimal layer without token manager (for backward compatibility)
    val minimalLayerNoTokenManager: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository,
        Config.Error,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                config <- ZIO.config[FioConfig]
                service = new FioTransactionImportService(
                    client,
                    txRepo,
                    sourceRepo,
                    None,
                    None,
                    None,
                    Some(config)
                )
            yield service
        }
end FioTransactionImportService
