package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.{
    BankTransactionService,
    TransactionImportError
}
import zio.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

/** Helper utilities for integration testing with Fio Bank API.
  *
  * This file contains convenience functions for getting environment variables, creating test
  * layers, and setting up test data.
  *
  * To run these integration tests with a real Fio Bank API token:
  *
  *   1. Set the FIO_TOKEN environment variable before running tests: export
  *      FIO_TOKEN=your-fio-api-token
  *
  * 2. Run the integration tests: sbtn budget-it/test
  *
  * If FIO_TOKEN is not set, tests requiring real API access will be skipped.
  */
object FioBankIntegrationHelpers:

    /** Gets the Fio API token from environment variables. Returns None if FIO_TOKEN is not set.
      */
    def getFioToken: Option[String] = sys.env.get("FIO_TOKEN")

    /** Returns a boolean indicating whether integration tests can be run. This is helpful when you
      * want to quickly determine if tests should run.
      */
    def canRunIntegrationTests: Boolean = getFioToken.isDefined

    /** Logs a message about whether integration tests will run or be skipped.
      */
    def logIntegrationTestStatus: UIO[Unit] =
        ZIO.logInfo(if canRunIntegrationTests then
            "FIO_TOKEN found in environment, running integration tests with real API"
        else
            "FIO_TOKEN not found in environment, integration tests will be skipped"
        )

    /** Standard test configuration for integration tests.
      */
    val testConfig = FioConfig(
        baseUrl = "https://fioapi.fio.cz/v1/rest",
        maxDateRangeDays = 90,
        connectionTimeoutSeconds = 10,
        requestTimeoutSeconds = 30,
        maxRetries = 1,
        initialBackoffSeconds = 1,
        maxBackoffSeconds = 5,
        encryptionKey = "test-integration-key-for-testing-only"
    )

    /** Sample account ID for testing.
      */
    val testAccountId = AccountId("fio", "test-account")

    /** Sample import batch ID for testing.
      */
    val testImportBatchId = ImportBatchId("test-import-batch", 1)

    /** Creates a standard date range for testing (last 7 days).
      * @return
      *   A tuple containing (startDate, endDate)
      */
    def getTestDateRange: (LocalDate, LocalDate) =
        val today = LocalDate.now()
        val startDate = today.minus(7, ChronoUnit.DAYS)
        (startDate, today)
    end getTestDateRange

    /** Creates a complete test layer for the Fio Bank adapter.
      *
      * @param token
      *   The Fio API token to use
      * @return
      *   A ZLayer that provides a BankTransactionService
      */
    def createTestLayer(token: String): ZLayer[Any, Throwable, BankTransactionService] =
        ZLayer.make[BankTransactionService](
            // Config layer
            ZLayer.succeed(testConfig),

            // Repository layer
            ZLayer.fromZIO(for
                accountsRef <- Ref.make(Map.empty[Long, FioAccount])
                idCounter = new AtomicLong(0)
            yield InMemoryFioAccountRepository(accountsRef, idCounter)),

            // API Client layer
            FioApiClient.live,

            // Prepare the token manager with our token
            ZLayer.fromZIO(for
                repo <- ZIO.service[FioAccountRepository]
                tokenCacheRef <- Ref.make(Map.empty[String, String])
                encryptionKey = testConfig.encryptionKey.getBytes("UTF-8").take(32)
                tokenManager = FioTokenManagerLive(repo, encryptionKey, tokenCacheRef)
                _ <- tokenManager.storeToken(testAccountId, token).mapError(e => new RuntimeException(e))
            yield tokenManager),

            // Service layer
            FioBankTransactionService.layer
        )

    /** Helper function to run a test with a real Fio API token. Returns None if the token isn't
      * available.
      *
      * @param testFn
      *   The test function to run with the token
      * @return
      *   A ZIO effect that completes with an optional test result
      */
    def withFioToken[R, E, A](testFn: String => ZIO[R, E, A]): ZIO[R, E, Option[A]] =
        ZIO.succeed(getFioToken).flatMap {
            case Some(token) =>
                ZIO.logInfo("Using real FIO_TOKEN for integration test") *>
                    testFn(token).map(Some(_))
            case None =>
                ZIO.logWarning("Skipping test - FIO_TOKEN not available") *>
                    ZIO.succeed(None)
        }

    /** Helper function to run a program with the Fio Bank adapter.
      *
      * @param program
      *   The program to run with the BankTransactionService
      * @return
      *   A ZIO effect that completes with the program result or None if token is not available
      */
    def runWithFioAdapter[R, E, A](
        program: ZIO[BankTransactionService & R, E, A]
    ): ZIO[R, E, Option[A]] =
        ZIO.succeed(getFioToken).flatMap {
            case Some(token) =>
                ZIO.logInfo("Using real FIO_TOKEN for adapter integration test") *>
                    program
                        .provideSomeLayer[R](createTestLayer(token))
                        .map(Some(_))
                        .mapError(e => e.asInstanceOf[E])
            case None =>
                ZIO.logWarning("Skipping adapter test - FIO_TOKEN not available") *>
                    ZIO.succeed(None)
        }
end FioBankIntegrationHelpers
