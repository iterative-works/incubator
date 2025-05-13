package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.BankTransactionService
import works.iterative.incubator.budget.domain.service.TransactionImportError
import works.iterative.incubator.budget.domain.service.TransactionImportError.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.FioModels.*
import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.{LocalDate, Instant}

/** Test specification for FioBankTransactionService.
  */
object FioBankTransactionServiceSpec extends ZIOSpecDefault:
    // Create sample test data
    val sampleAccountId = AccountId("fio", "1234567890")
    val sampleImportBatchId = ImportBatchId("fio-1234567890", 1)
    val sampleToken = "test-token-123456"
    val sampleDate1 = LocalDate.of(2025, 3, 14)
    val sampleDate2 = LocalDate.of(2025, 3, 15)

    val sampleFioTransaction = FioTransaction(
        column0 = Some(Column(0, "Datum", "2025-03-14+0100")),
        column1 = Some(Column(1, "Objem", BigDecimal("1000.50"))),
        column2 = Some(Column(2, "Protiúčet", "2800000002")),
        column3 = Some(Column(3, "Kód banky", "2010")),
        column4 = Some(Column(4, "KS", "123")),
        column5 = Some(Column(5, "VS", "456789")),
        column6 = Some(Column(6, "SS", "789")),
        column7 = Some(Column(7, "Uživatelská identifikace", "User ID")),
        column8 = Some(Column(8, "Typ", "Příjem převodem uvnitř banky")),
        column10 = Some(Column(10, "Název protiúčtu", "Novák, Jan")),
        column12 = Some(Column(12, "Název banky", "Fio banka, a.s.")),
        column14 = Some(Column(14, "Měna", "CZK")),
        column17 = Some(Column(17, "ID pokynu", BigDecimal("12345"))),
        column22 = Some(Column(22, "ID pohybu", BigDecimal("26962199069"))),
        column25 = Some(Column(25, "Komentář", "Test payment")),
        column26 = Some(Column(26, "BIC", "FIOBCZPPXXX"))
    )

    // Default configuration for testing
    val testConfig = FioConfig.defaultConfig

    // Manual mock for FioApiClient
    def createMockApiClient(transactions: List[FioTransaction] = List.empty): FioApiClient =
        new FioApiClient:
            override def fetchTransactionsByDateRange(
                token: String,
                dateFrom: LocalDate,
                dateTo: LocalDate
            ): ZIO[Any, Throwable, List[FioTransaction]] =
                ZIO.succeed(transactions)

            override def fetchNewTransactions(token: String)
                : ZIO[Any, Throwable, List[FioTransaction]] =
                ZIO.succeed(transactions)

            override def setLastDate(token: String, date: LocalDate): ZIO[Any, Throwable, Unit] =
                ZIO.unit

    // Mock that throws an exception
    def createErrorMockApiClient(error: Throwable): FioApiClient = new FioApiClient:
        override def fetchTransactionsByDateRange(
            token: String,
            dateFrom: LocalDate,
            dateTo: LocalDate
        ): ZIO[Any, Throwable, List[FioTransaction]] =
            ZIO.fail(error)

        override def fetchNewTransactions(token: String)
            : ZIO[Any, Throwable, List[FioTransaction]] =
            ZIO.fail(error)

        override def setLastDate(token: String, date: LocalDate): ZIO[Any, Throwable, Unit] =
            ZIO.fail(error)

    // Manual mock for FioTokenManager
    def createMockTokenManager(token: String = sampleToken): FioTokenManager = new FioTokenManager:
        override def getToken(accountId: AccountId): ZIO[Any, String, String] =
            ZIO.succeed(token)

        override def storeToken(accountId: AccountId, token: String): ZIO[Any, String, Unit] =
            ZIO.unit

        override def encryptToken(token: String): ZIO[Any, String, String] =
            ZIO.succeed(s"encrypted-$token")

        override def decryptToken(encryptedToken: String): ZIO[Any, String, String] =
            ZIO.succeed(encryptedToken.replace("encrypted-", ""))

        override def clearCache(): UIO[Unit] =
            ZIO.unit

    // Mock that returns an error
    def createErrorMockTokenManager(error: String): FioTokenManager = new FioTokenManager:
        override def getToken(accountId: AccountId): ZIO[Any, String, String] =
            ZIO.fail(error)

        override def storeToken(accountId: AccountId, token: String): ZIO[Any, String, Unit] =
            ZIO.fail(error)

        override def encryptToken(token: String): ZIO[Any, String, String] =
            ZIO.fail(error)

        override def decryptToken(encryptedToken: String): ZIO[Any, String, String] =
            ZIO.fail(error)

        override def clearCache(): UIO[Unit] =
            ZIO.unit

    def spec = suite("FioBankTransactionService")(
        suite("validateDateRangeForAccount")(
            test("should succeed for valid date range") {
                // Use in-memory implementation to verify the validation logic
                val service = FioBankTransactionServiceLive(
                    testConfig,
                    null, // Not used in this test
                    null // Not used in this test
                )

                val result = service.validateDateRangeForAccount(
                    sampleAccountId,
                    sampleDate1,
                    sampleDate2
                )

                assertZIO(result)(isUnit)
            },
            test("should fail for date range exceeding maximum days") {
                // Use in-memory implementation to verify the validation logic
                val service = FioBankTransactionServiceLive(
                    testConfig,
                    null, // Not used in this test
                    null // Not used in this test
                )

                val largeRange = service.validateDateRangeForAccount(
                    sampleAccountId,
                    sampleDate1,
                    sampleDate1.plusDays(testConfig.maxDateRangeDays + 1)
                )

                assertZIO(largeRange.exit)(
                    fails(isSubtype[InvalidDateRange](anything))
                )
            }
        ),
        suite("fetchTransactions")(
            test("should fetch and transform transactions successfully") {
                // Create mock dependencies
                val mockApiClient = createMockApiClient(List(sampleFioTransaction))
                val mockTokenManager = createMockTokenManager()

                // Create the service to test
                val serviceUnderTest = FioBankTransactionServiceLive(
                    testConfig,
                    mockApiClient,
                    mockTokenManager
                )

                // Execute the test
                val result = serviceUnderTest.fetchTransactions(
                    sampleAccountId,
                    sampleDate1,
                    sampleDate2,
                    sampleImportBatchId
                )

                // Assertions
                assertZIO(result.map(_.size))(equalTo(1))
            },
            test("should handle API errors correctly") {
                // Create mock dependencies with error
                val apiException = new RuntimeException("Invalid token")
                val mockApiClient = createErrorMockApiClient(apiException)
                val mockTokenManager = createMockTokenManager()

                // Create the service to test
                val serviceUnderTest = FioBankTransactionServiceLive(
                    testConfig,
                    mockApiClient,
                    mockTokenManager
                )

                // Execute the test
                val result = serviceUnderTest.fetchTransactions(
                    sampleAccountId,
                    sampleDate1,
                    sampleDate2,
                    sampleImportBatchId
                )

                // Assertions
                assertZIO(result.exit)(
                    fails(isSubtype[Throwable](anything))
                )
            },
            test("should handle token retrieval errors correctly") {
                // Create mock dependencies with token error
                val tokenError = "Token not found"
                val mockApiClient = createMockApiClient()
                val mockTokenManager = createErrorMockTokenManager(tokenError)

                // Create the service to test
                val serviceUnderTest = FioBankTransactionServiceLive(
                    testConfig,
                    mockApiClient,
                    mockTokenManager
                )

                // Execute the test
                val result = serviceUnderTest.fetchTransactions(
                    sampleAccountId,
                    sampleDate1,
                    sampleDate2,
                    sampleImportBatchId
                )

                // Assertions
                assertZIO(result.exit)(
                    fails(isSubtype[Throwable](anything))
                )
            },
            test("should handle empty transaction list correctly") {
                // Create mock dependencies with empty transaction list
                val mockApiClient = createMockApiClient(List.empty)
                val mockTokenManager = createMockTokenManager()

                // Create the service to test
                val serviceUnderTest = FioBankTransactionServiceLive(
                    testConfig,
                    mockApiClient,
                    mockTokenManager
                )

                // Execute the test
                val result = serviceUnderTest.fetchTransactions(
                    sampleAccountId,
                    sampleDate1,
                    sampleDate2,
                    sampleImportBatchId
                )

                // Assertions
                assertZIO(result)(isEmpty)
            }
        )
    )
end FioBankTransactionServiceSpec
