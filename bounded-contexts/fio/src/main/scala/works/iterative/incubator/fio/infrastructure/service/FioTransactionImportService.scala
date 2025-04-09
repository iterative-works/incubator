package works.iterative.incubator.fio.infrastructure.service

import zio.*
import java.time.LocalDate
import java.time.Instant
import works.iterative.incubator.fio.application.service.FioImportService
import works.iterative.incubator.fio.infrastructure.client.FioClient
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
    importStateRepository: Option[FioImportStateRepository] = None
) extends FioImportService, TransactionImportService:

    // Simple in-memory cache using ZIO Ref
    private val sourceAccountCache: Ref[Map[(String, String), Long]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[(String, String), Long])

    override def importTransactions(from: LocalDate, to: LocalDate): Task[Int] =
        for
            // Clear the cache at the beginning of each import batch
            _ <- sourceAccountCache.set(Map.empty)
            response <- fioClient.fetchTransactions(from, to)
            transactions <- mapFioTransactionsToModel(response)
            _ <- ZIO.foreachDiscard(transactions) { transaction =>
                // Only save the immutable transaction
                transactionRepository.save(transaction.id, transaction)
            }
            
            // Update the import state with the latest transaction ID if available
            _ <- updateImportState(transactions, response)
        yield transactions.size

    override def importNewTransactions(): Task[Int] =
        for
            // Get all Fio source accounts
            sourceAccounts <- getFioSourceAccounts()
            
            // For each source account, get the last transaction ID
            // and import new transactions
            results <- ZIO.foreach(sourceAccounts) { sourceId =>
                getLastTransactionId(sourceId).flatMap { lastId =>
                    importNewTransactions(lastId)
                }
            }
        yield results.sum

    override def getFioSourceAccounts(): Task[List[Long]] =
        sourceAccountRepository.find(SourceAccountQuery()).map(_.map(_.id).toList)

    /** Legacy method to maintain compatibility with TransactionImportService
      */
    override def importNewTransactions(lastId: Option[Long]): Task[Int] =
        for
            // Clear the cache at the beginning of each import batch
            _ <- sourceAccountCache.set(Map.empty)
            id <- ZIO.fromOption(lastId).orElse(ZIO.succeed(0L))
            response <- fioClient.fetchNewTransactions(id)
            transactions <- mapFioTransactionsToModel(response)
            _ <- ZIO.foreachDiscard(transactions) { transaction =>
                // Only save the immutable transaction
                transactionRepository.save(transaction.id, transaction)
            }
            
            // Update the import state with the latest transaction ID if available
            _ <- updateImportState(transactions, response)
        yield transactions.size

    private def mapFioTransactionsToModel(response: FioResponse): Task[List[Transaction]] =
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
                        SourceAccountQuery(accountId = Some(accountId), bankId = Some(bankId))
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
    
    /**
     * Updates the import state with the latest transaction ID
     * Only executes if importStateRepository is available
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
    
    /**
     * Gets the last transaction ID for a source account
     */
    private def getLastTransactionId(sourceAccountId: Long): Task[Option[Long]] =
        importStateRepository match
            case Some(repo) =>
                repo.getImportState(sourceAccountId).map(_.flatMap(_.lastTransactionId))
            case None =>
                ZIO.succeed(None)
end FioTransactionImportService

object FioTransactionImportService:
    // Layer that provides both TransactionImportService and FioImportService
    val layer: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository,
        Nothing,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                service = new FioTransactionImportService(client, txRepo, sourceRepo)
            yield service
        }
        
    // Layer with import state repository
    val layerWithImportState: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository &
            FioImportStateRepository,
        Nothing,
        TransactionImportService & FioImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
                importStateRepo <- ZIO.service[FioImportStateRepository]
                service = new FioTransactionImportService(
                    client, 
                    txRepo, 
                    sourceRepo,
                    Some(importStateRepo)
                )
            yield service
        }
end FioTransactionImportService