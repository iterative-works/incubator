package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.BankTransactionService
import works.iterative.incubator.budget.domain.service.TransactionImportError
import works.iterative.incubator.budget.domain.service.TransactionImportError.*
import java.time.{LocalDate, Period}
import zio.*

/** Fio Bank implementation of BankTransactionService.
  *
  * This service connects to the Fio Bank API, fetches transactions for a given date range, and
  * transforms them into the domain Transaction model.
  *
  * Category: Service Implementation Layer: Infrastructure
  */
trait FioBankTransactionService extends BankTransactionService

/** Live implementation of FioBankTransactionService.
  *
  * Provides integration with the Fio Bank API for importing transactions.
  *
  * @param config
  *   The configuration for the Fio Bank integration
  * @param apiClient
  *   The client for interacting with the Fio Bank API
  * @param tokenManager
  *   The service for managing Fio API tokens
  */
final case class FioBankTransactionServiceLive(
    config: FioConfig,
    apiClient: FioApiClient,
    tokenManager: FioTokenManager
) extends FioBankTransactionService:

    /** Validates date range according to Fio Bank-specific rules.
      *
      * Fio Bank has a limit of 90 days for transaction history retrieval.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if the validation passes, or fails with an
      *   InvalidDateRange error
      */
    override protected def validateBankSpecificDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        // Get the maximum date range from configuration
        val maxDays = config.maxDateRangeDays
        val daysBetween = Period.between(startDate, endDate).getDays + 1

        if daysBetween > maxDays then
            ZIO.fail(
                InvalidDateRange(
                    s"Date range cannot exceed $maxDays days (Fio Bank limitation)"
                )
            )
        else
            ZIO.unit
        end if
    end validateBankSpecificDateRange

    /** Fetches transactions from the Fio Bank API for a specified date range.
      *
      * This method handles token retrieval, API calls, error mapping, and transformation to domain
      * model.
      *
      * @param accountId
      *   The ID of the source account to fetch transactions for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @param importBatchId
      *   The ID of the import batch
      * @return
      *   A ZIO effect that returns a list of transactions or a service-specific error
      */
    override def fetchTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate,
        importBatchId: ImportBatchId
    ): ZIO[Any, Throwable, List[Transaction]] =
        for
            // Get the Fio token for this account
            _ <- ZIO.logInfo(s"Fetching Fio token for account ${accountId.toString}")
            token <- tokenManager.getToken(accountId)
                .mapError(err => new RuntimeException(s"Token retrieval error: $err"))

            // Fetch transactions from Fio API
            _ <- ZIO.logInfo(
                s"Fetching transactions from Fio Bank API for period $startDate to $endDate"
            )
            fioTransactions <- apiClient
                .fetchTransactionsByDateRange(token, startDate, endDate)
                .tapError(err =>
                    ZIO.logError(s"Error fetching transactions from Fio API: ${err.getMessage}")
                )

            // Process any API-specific errors
            _ <- ZIO.logInfo(s"Processing API response with ${fioTransactions.size} transactions")

            // Map Fio transactions to domain model
            domainTransactions <- ZIO.foreach(fioTransactions) { fioTx =>
                ZIO.fromEither(
                    FioMappers
                        .mapToDomainTransaction(fioTx, accountId, importBatchId)
                        .left
                        .map(err => new RuntimeException(s"Mapping error: $err"))
                )
            }

            // Check if we have transactions or need to report empty result
            _ <- if domainTransactions.isEmpty then
                ZIO.logWarning(
                    s"No transactions found for account ${accountId.toString} in period $startDate to $endDate"
                )
            else ZIO.unit
        yield domainTransactions
end FioBankTransactionServiceLive

/** Companion object for FioBankTransactionService.
  */
object FioBankTransactionService:
    /** Creates a layer with all required dependencies.
      *
      * @return
      *   A ZLayer that provides a BankTransactionService
      */
    val layer: ZLayer[FioApiClient & FioTokenManager, Config.Error, BankTransactionService] =
        ZLayer {
            for
                config <- ZIO.config[FioConfig]
                client <- ZIO.service[FioApiClient]
                tokenManager <- ZIO.service[FioTokenManager]
            yield FioBankTransactionServiceLive(config, client, tokenManager)
        }
    end layer

    /** Convenience layer that includes all necessary downstream dependencies.
      *
      * @return
      *   A ZLayer that provides a BankTransactionService
      */
    val fullLayer: ZLayer[FioAccountRepository, Throwable, BankTransactionService] =
        ZLayer {
            for
                repo <- ZIO.service[FioAccountRepository]
                config <- ZIO.config[FioConfig]
                apiClient = FioApiClientLive(config)
                tokenManagerRef <- Ref.make(Map.empty[String, String])
                tokenManager = FioTokenManagerLive(
                    repo,
                    config.encryptionKey.getBytes("UTF-8").take(32),
                    tokenManagerRef
                )
                service = FioBankTransactionServiceLive(config, apiClient, tokenManager)
            yield service: BankTransactionService
        }
end FioBankTransactionService
