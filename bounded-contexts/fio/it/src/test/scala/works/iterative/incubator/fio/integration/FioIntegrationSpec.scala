package works.iterative.incubator.fio.integration

import zio.*
import zio.test.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import works.iterative.incubator.fio.application.service.FioImportService
import works.iterative.incubator.fio.infrastructure.client.FioClient
import works.iterative.incubator.fio.infrastructure.service.FioTransactionImportService
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.security.*
import works.iterative.incubator.transactions.domain.model.{SourceAccount, Transaction}
import works.iterative.incubator.transactions.domain.repository.{
    TransactionRepository,
    SourceAccountRepository
}
import works.iterative.incubator.transactions.domain.query.{SourceAccountQuery, TransactionQuery}
import java.time.{LocalDate, Instant}

/** Integration tests for the Fio Bank API integration.
  *
  * These tests require a valid Fio API token. They are designed to be run manually, not as part of
  * the automated test suite.
  *
  * To run these tests, you need to set the following environment variables:
  *   - FIO_TOKEN: A valid Fio API token
  *   - FIO_TEST_ACCOUNT_ID: ID of a test account to use (optional)
  *
  * If the environment variables are not set, the tests will be skipped.
  */
object FioIntegrationSpec extends ZIOSpecDefault:
    // Read config from environment
    private val testToken = sys.env.get("FIO_TOKEN")
    private val testAccountId = sys.env.getOrElse("FIO_TEST_ACCOUNT_ID", "1").toLong

    // Security configuration
    private val securityConfig = FioSecurityConfig(
        encryptionKey = "test-integration-key-32-chars-long",
        cacheExpirationMinutes = 5
    )

    // Service and backend layers
    private val backendLayer = HttpClientZioBackend.layer()
    private val clientLayer = backendLayer >>> FioClient.test

    // Set up in-memory repositories for testing
    private val inMemoryFioAccountRepository = ZLayer.succeed {
        new FioAccountRepository:
            private val accounts = collection.mutable.Map.empty[Long, FioAccount]

            // Initialize with test account if provided
            testToken match
                case Some(token) =>
                    val encrypted = encrypt(token, securityConfig.encryptionKey)
                    val account = FioAccount(
                        id = 1L,
                        sourceAccountId = testAccountId,
                        token = encrypted,
                        lastFetchedId = None,
                        lastSyncTime = None
                    )
                    accounts(1L) = account
                case _ => ()
            end match

            override def getAll(): Task[List[FioAccount]] =
                ZIO.succeed(accounts.values.toList)

            override def getById(id: Long): Task[Option[FioAccount]] =
                ZIO.succeed(accounts.get(id))

            override def getBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]] =
                ZIO.succeed(accounts.values.find(_.sourceAccountId == sourceAccountId))

            override def create(command: CreateFioAccount): Task[Long] =
                ZIO.succeed {
                    val id = accounts.size + 1L
                    val account = FioAccount(
                        id = id,
                        sourceAccountId = command.sourceAccountId,
                        token = command.token,
                        lastFetchedId = None,
                        lastSyncTime = None
                    )
                    accounts(id) = account
                    id
                }

            override def update(account: FioAccount): Task[Unit] =
                ZIO.succeed {
                    accounts(account.id) = account
                }

            override def delete(id: Long): Task[Unit] =
                ZIO.succeed {
                    val _ = accounts.remove(id)
                    ()
                }

            override def updateLastFetched(id: Long, lastId: Long, syncTime: Instant): Task[Unit] =
                ZIO.succeed {
                    accounts.get(id).foreach { account =>
                        accounts(id) = account.copy(
                            lastFetchedId = Some(lastId),
                            lastSyncTime = Some(syncTime)
                        )
                    }
                }
    }

    private val inMemorySourceAccountRepository = ZLayer.succeed {
        new SourceAccountRepository:
            private val accounts = collection.mutable.Map.empty[Long, SourceAccount]

            // Initialize with test account if provided
            val account = SourceAccount(
                id = testAccountId,
                accountId = "2100123456",
                bankId = "2010",
                name = "Test Account",
                currency = "CZK",
                active = true
            )
            accounts(testAccountId) = account

            override def find(filter: SourceAccountQuery): UIO[Seq[SourceAccount]] =
                ZIO.succeed {
                    accounts.values.filter { account =>
                        (filter.accountId.isEmpty || filter.accountId.contains(
                            account.accountId
                        )) &&
                        (filter.bankId.isEmpty || filter.bankId.contains(account.bankId)) &&
                        (filter.active.isEmpty || filter.active.contains(account.active))
                    }.toSeq
                }

            override def load(id: Long): UIO[Option[SourceAccount]] =
                ZIO.succeed(accounts.get(id))

            override def create(
                value: works.iterative.incubator.transactions.domain.model.CreateSourceAccount
            ): UIO[Long] =
                ZIO.succeed {
                    val id = accounts.size + 1L
                    val account = SourceAccount(
                        id = id,
                        accountId = value.accountId,
                        bankId = value.bankId,
                        name = value.name,
                        currency = value.currency,
                        active = true
                    )
                    accounts(id) = account
                    id
                }

            override def save(key: Long, value: SourceAccount): UIO[Unit] =
                ZIO.succeed {
                    accounts(key) = value
                }

            // For compatibility with older test code
            def getById(id: Long): Task[Option[SourceAccount]] = load(id)
    }

    private val inMemoryTransactionRepository = ZLayer.succeed {
        new TransactionRepository:
            private val transactions = collection.mutable.Map.empty[
                works.iterative.incubator.transactions.domain.model.TransactionId,
                Transaction
            ]

            override def find(filter: TransactionQuery): UIO[Seq[Transaction]] =
                ZIO.succeed {
                    transactions.values.filter { tx =>
                        (filter.sourceAccountId.isEmpty || filter.sourceAccountId.contains(
                            tx.id.sourceAccountId
                        )) &&
                        (filter.dateFrom.isEmpty || !tx.date.isBefore(filter.dateFrom.get)) &&
                        (filter.dateTo.isEmpty || !tx.date.isAfter(filter.dateTo.get))
                    }.toSeq
                }

            override def load(id: works.iterative.incubator.transactions.domain.model.TransactionId)
                : UIO[Option[Transaction]] =
                ZIO.succeed(transactions.get(id))

            override def save(
                key: works.iterative.incubator.transactions.domain.model.TransactionId,
                value: Transaction
            ): UIO[Unit] =
                ZIO.succeed {
                    transactions(key) = value
                }
    }

    private val tokenAuditLayer = ZLayer.succeed {
        new FioTokenAuditService:
            private val events = Unsafe.unsafely {
                Ref.unsafe.make(List.empty[TokenAuditEvent])
            }

            override def logEvent(event: TokenAuditEvent): Task[Unit] =
                events.update(event :: _)

            override def getRecentEvents(limit: Int): Task[List[TokenAuditEvent]] =
                events.get.map(_.take(limit))

            override def getEventsForAccount(
                accountId: Long,
                limit: Int
            ): Task[List[TokenAuditEvent]] =
                events.get.map(_.filter(_.accountId == accountId).take(limit))
    }

    private val tokenManagerLayer = ZLayer {
        for
            repo <- ZIO.service[FioAccountRepository]
            audit <- ZIO.service[FioTokenAuditService]
        yield new FioTokenManagerLive(repo, securityConfig, audit)
    }

    private val allLayers = ZLayer.make[
        FioClient & FioImportService & FioTokenManager & FioTokenAuditService & FioAccountRepository
    ](
        clientLayer,
        inMemoryTransactionRepository,
        inMemorySourceAccountRepository,
        inMemoryFioAccountRepository,
        tokenAuditLayer,
        tokenManagerLayer,
        FioTransactionImportService.accountLayer
    )

    // Helper method to get date range for testing
    private def getDateRange(): (LocalDate, LocalDate) =
        val to = LocalDate.now
        val from = to.minusDays(30) // Last 30 days
        (from, to)

    // Helper method to encrypt tokens for testing
    private def encrypt(value: String, key: String): String =
        try
            // Use the first 16 bytes of the key as IV for simplicity
            val keyBytes = key.getBytes("UTF-8").take(32)
            val iv = keyBytes.take(16)

            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val ivSpec = new javax.crypto.spec.IvParameterSpec(iv)

            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(value.getBytes("UTF-8"))
            java.util.Base64.getEncoder.encodeToString(encrypted)
        catch
            case e: Exception =>
                throw new RuntimeException("Error encrypting token", e)

    def spec = suite("Fio Bank Integration Tests")(
        test("Fio client should connect successfully") {
            for
                client <- ZIO.service[FioClient]
                token <- ZIO.fromOption(testToken)
                    .orElseFail(new RuntimeException("No FIO_TOKEN set"))
                (from, to) = getDateRange()
                response <- client.fetchTransactions(token, from, to)
            yield assertTrue(response.accountStatement.info.iban.nonEmpty)
        },
        test("Fio client should fetch transactions by date range") {
            for
                client <- ZIO.service[FioClient]
                token <- ZIO.fromOption(testToken)
                    .orElseFail(new RuntimeException("No FIO_TOKEN set"))
                (from, to) = getDateRange()
                response <- client.fetchTransactions(token, from, to)
                _ <- Console.printLine(s"Account IBAN: ${response.accountStatement.info.iban}")
                _ <- Console.printLine(
                    s"Found ${response.accountStatement.transactionList.transaction.size} transactions"
                )
            yield assertTrue(response.accountStatement.transactionList.transaction.nonEmpty)
        },
        test("Fio import service should import transactions using repository token") {
            (for
                service <- ZIO.service[FioImportService]
                (from, to) = getDateRange()
                count <- service.importTransactionsForAccount(from, to, testAccountId)
                _ <- Console.printLine(s"Imported $count transactions for account $testAccountId")
            yield assertTrue(count > 0))
                .catchAll { error =>
                    Console.printLine(s"Test failed with error: ${error.getMessage}") *>
                        ZIO.fail(error)
                }
        },
        test("Fio token manager should retrieve and decrypt token") {
            (for
                tokenManager <- ZIO.service[FioTokenManager]
                accountId <- ZIO.succeed(1L) // Using the ID we set in the test repository
                tokenOpt <- tokenManager.getToken(accountId)
                _ <- ZIO.foreach(tokenOpt) { token =>
                    // Only log the token length for security
                    Console.printLine(s"Retrieved token of length ${token.length}")
                }
            yield assertTrue(tokenOpt.isDefined && tokenOpt.get.length > 16))
        }
    ).provideLayer(allLayers) @@ TestAspect.withLiveEnvironment @@ TestAspect.ifEnvSet(
        "FIO_TOKEN"
    )
end FioIntegrationSpec
