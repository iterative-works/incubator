package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.BankTransactionService
import zio.*

/** Module that provides all Fio Bank-related services.
  *
  * Conveniently bundles all Fio Bank integration components in a single module.
  *
  * Category: Module Layer: Infrastructure
  */
object FioBankModule:
    /** Creates an in-memory implementation for testing.
      *
      * @return
      *   A ZLayer that provides all Fio Bank services with in-memory implementations
      */
    val inMemory: ULayer[BankTransactionService] =
        ZLayer.make[BankTransactionService](
            InMemoryFioAccountRepository.layer,
            FioApiClient.live.orDie,
            FioTokenManager.layer.orDie,
            FioBankTransactionService.layer.orDie
        )

    /** Creates a production-ready implementation with all services.
      *
      * @param config
      *   The configuration for the Fio Bank integration
      * @return
      *   A ZLayer that provides all Fio Bank services with production implementations
      */
    val live: ZLayer[FioAccountRepository, Throwable, BankTransactionService] =
        ZLayer.fromZIO {
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

    /** Convenient accessor for importing transactions.
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
    def importTransactions(
        accountId: AccountId,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        importBatchId: ImportBatchId
    ): ZIO[BankTransactionService, Throwable, List[Transaction]] =
        BankTransactionService.fetchTransactions(accountId, startDate, endDate, importBatchId)

    /** Convenient accessor for validating date ranges.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires BankTransactionService and completes successfully if the date
      *   range is valid for the specified account, or fails with an InvalidDateRange error if
      *   invalid
      */
    def validateDateRange(
        accountId: AccountId,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ): ZIO[
        BankTransactionService,
        works.iterative.incubator.budget.domain.service.TransactionImportError,
        Unit
    ] =
        BankTransactionService.validateDateRangeForAccount(accountId, startDate, endDate)

    /** Convenient accessor for storing a token.
      *
      * @param accountId
      *   The account ID to store the token for
      * @param token
      *   The Fio API token to store
      * @return
      *   A ZIO effect that requires FioTokenManager and returns Unit or fails with an error
      */
    def storeToken(
        accountId: AccountId,
        token: String
    ): ZIO[FioTokenManager, String, Unit] =
        FioTokenManager.storeToken(accountId, token)
end FioBankModule
