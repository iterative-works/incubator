package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.*
import java.time.LocalDate
import java.time.Instant
import service.TransactionImportService
import service.TransactionRepository

class FioTransactionImportService(
    fioClient: FioClient,
    repository: TransactionRepository
) extends TransactionImportService:

    override def importTransactions(from: LocalDate, to: LocalDate): Task[Int] =
        for
            response <- fioClient.fetchTransactions(from, to)
            transactions = mapFioTransactionsToModel(response)
            _ <- ZIO.foreachDiscard(transactions)(tx =>
                repository.save(tx.id, tx)
            )
        yield transactions.size

    override def importNewTransactions(lastId: Option[Long]): Task[Int] =
        for
            id <- ZIO.fromOption(lastId).orElse(ZIO.succeed(0L))
            response <- fioClient.fetchNewTransactions(id)
            transactions = mapFioTransactionsToModel(response)
            _ <- ZIO.foreachDiscard(transactions)(tx =>
                repository.save(tx.id, tx)
            )
        yield transactions.size

    private def mapFioTransactionsToModel(response: FioResponse): List[Transaction] =
        val accountId = response.accountStatement.info.accountId
        val bankId = response.accountStatement.info.bankId

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

            // Map other fields correctly based on the example JSON structure
            Transaction(
                id = TransactionId(
                    sourceAccount = accountId,
                    sourceBank = bankId,
                    id = txId
                ),
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

                // Set initial status
                status = TransactionStatus.Imported,

                // Initialize AI fields as None
                suggestedPayeeName = None,
                suggestedCategory = None,
                suggestedMemo = None,

                // Initialize user overrides as None
                overridePayeeName = None,
                overrideCategory = None,
                overrideMemo = None,

                // Initialize YNAB fields as None
                ynabTransactionId = None,
                ynabAccountId = None,

                // Set metadata
                importedAt = Instant.now(),
                processedAt = None,
                submittedAt = None
            )
        }
    end mapFioTransactionsToModel
end FioTransactionImportService

object FioTransactionImportService:
    val layer: ZLayer[FioClient & TransactionRepository, Config.Error, TransactionImportService] =
        ZLayer {
            for
                client <- ZIO.service[FioClient]
                repo <- ZIO.service[TransactionRepository]
            yield FioTransactionImportService(client, repo)
        }
end FioTransactionImportService
