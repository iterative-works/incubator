package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.*
import java.time.LocalDate
import java.time.Instant
import service.TransactionImportService
import service.TransactionRepository
import service.TransactionProcessingStateRepository
import service.SourceAccountRepository

class FioTransactionImportService(
    fioClient: FioClient,
    transactionRepository: TransactionRepository,
    processingStateRepository: TransactionProcessingStateRepository,
    sourceAccountRepository: SourceAccountRepository
) extends TransactionImportService:

    override def importTransactions(from: LocalDate, to: LocalDate): Task[Int] =
        for
            response <- fioClient.fetchTransactions(from, to)
            transactions <- mapFioTransactionsToModel(response)
            _ <- ZIO.foreachDiscard(transactions) { transaction =>
                for
                    // First save the immutable transaction
                    _ <- transactionRepository.save(transaction.id, transaction)
                    // Then create and save the initial processing state
                    initialState = TransactionProcessingState.initial(transaction)
                    _ <- processingStateRepository.save(transaction.id, initialState)
                yield ()
            }
        yield transactions.size

    override def importNewTransactions(lastId: Option[Long]): Task[Int] =
        for
            id <- ZIO.fromOption(lastId).orElse(ZIO.succeed(0L))
            response <- fioClient.fetchNewTransactions(id)
            transactions <- mapFioTransactionsToModel(response)
            _ <- ZIO.foreachDiscard(transactions) { transaction =>
                for
                    // First save the immutable transaction
                    _ <- transactionRepository.save(transaction.id, transaction)
                    // Then create and save the initial processing state
                    initialState = TransactionProcessingState.initial(transaction)
                    _ <- processingStateRepository.save(transaction.id, initialState)
                yield ()
            }
        yield transactions.size

    private def mapFioTransactionsToModel(response: FioResponse): Task[List[Transaction]] =
        val accountId = response.accountStatement.info.accountId
        val bankId = response.accountStatement.info.bankId

        // Find the source account by account ID and bank ID
        sourceAccountRepository.find(
            SourceAccountQuery(accountId = Some(accountId), bankId = Some(bankId))
        ).map(_.headOption) flatMap {
            case Some(sourceAccount) =>
                // We found the matching source account
                ZIO.succeed(mapTransactions(response, sourceAccount.id))
            case None =>
                // No matching source account found - this is an error condition
                ZIO.fail(new RuntimeException(
                    s"No source account found for account ID $accountId and bank ID $bankId"
                ))
        }

    /** Maps FIO transaction data to our domain model
     * 
     * @param response The parsed FIO API response
     * @param sourceAccountId The resolved internal source account ID
     * @return List of domain Transaction objects
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
        TransactionProcessingStateRepository & 
        SourceAccountRepository, 
        Config.Error, 
        TransactionImportService
    ] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                txRepo <- ZIO.service[TransactionRepository]
                stateRepo <- ZIO.service[TransactionProcessingStateRepository]
                sourceRepo <- ZIO.service[SourceAccountRepository]
            yield FioTransactionImportService(client, txRepo, stateRepo, sourceRepo)
        }
end FioTransactionImportService
