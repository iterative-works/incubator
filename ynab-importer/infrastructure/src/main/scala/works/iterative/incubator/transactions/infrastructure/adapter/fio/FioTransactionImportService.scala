package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.*
import java.time.LocalDate
import java.time.Instant
import service.TransactionImportService
import service.TransactionRepository
import service.SourceAccountRepository

class FioTransactionImportService(
    fioClient: FioClient,
    transactionRepository: TransactionRepository,
    sourceAccountRepository: SourceAccountRepository
) extends TransactionImportService:

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
        yield transactions.size

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
end FioTransactionImportService

object FioTransactionImportService:
    val layer: ZLayer[
        FioClient &
            TransactionRepository &
            SourceAccountRepository,
        Config.Error,
        TransactionImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
            yield FioTransactionImportService(client, txRepo, sourceRepo)
        }
end FioTransactionImportService
