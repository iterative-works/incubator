package works.iterative.incubator.fio.infrastructure.service

import zio.test.*
import zio.*
import zio.json.*
import zio.nio.file.{Files, Path}
import java.time.LocalDate
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.client.{FioClient, FioCodecs}
import works.iterative.incubator.fio.infrastructure.config.FioConfig
import works.iterative.incubator.transactions.domain.model.*
import works.iterative.incubator.transactions.domain.repository.*
import works.iterative.incubator.transactions.domain.query.*
import FioCodecs.given

/** Test suite for FioTransactionImportService
  */
object FioTransactionImportServiceSpec extends ZIOSpecDefault:

    // Mock repositories for testing
    class MockTransactionRepository extends TransactionRepository:
        private val storage: Ref[Map[TransactionId, Transaction]] =
            Unsafe.unsafe { implicit unsafe =>
                Ref.unsafe.make(Map.empty[TransactionId, Transaction])
            }

        override def save(id: TransactionId, transaction: Transaction): UIO[Unit] =
            storage.update(_ + (id -> transaction))

        override def load(id: TransactionId): UIO[Option[Transaction]] =
            storage.get.map(_.get(id))

        override def find(query: TransactionQuery): UIO[Seq[Transaction]] =
            storage.get.map(_.values.toSeq)

        // Additional method for test verification
        def getAllTransactions: UIO[Seq[Transaction]] =
            storage.get.map(_.values.toSeq)
    end MockTransactionRepository

    class MockSourceAccountRepository extends SourceAccountRepository:
        private val accounts = Seq(
            SourceAccount(
                id = 1L,
                name = "Test Fio Account",
                accountId = "2200000001",
                bankId = "2010",
                currency = "CZK",
                active = true,
                lastSyncTime = None
            )
        )

        override def save(key: Long, value: SourceAccount): UIO[Unit] = ZIO.unit
        override def load(id: Long): UIO[Option[SourceAccount]] =
            ZIO.succeed(accounts.find(_.id == id))
        override def find(query: SourceAccountQuery): UIO[Seq[SourceAccount]] =
            ZIO.succeed(
                accounts.filter(account =>
                    query.accountId.forall(_ == account.accountId) &&
                        query.bankId.forall(_ == account.bankId)
                )
            )
        override def create(value: CreateSourceAccount): UIO[Long] = ZIO.succeed(1L)
    end MockSourceAccountRepository

    // Mock Fio client for testing
    class MockFioClient extends FioClient:
        override def fetchTransactions(
            token: String,
            from: LocalDate,
            to: LocalDate
        ): Task[FioResponse] =
            loadExampleResponse()

        override def fetchNewTransactions(token: String, lastId: Long): Task[FioResponse] =
            loadExampleResponse()

        private def loadExampleResponse(): Task[FioResponse] =
            Files.readAllBytes(Path("bounded-contexts/fio/src/test/resources/example_fio.json"))
                .map(bytes => new String(bytes.toArray))
                .flatMap(jsonString =>
                    ZIO.fromEither(jsonString.fromJson[FioResponse])
                        .mapError(err => new RuntimeException(s"Failed to parse test JSON: $err"))
                )
    end MockFioClient

    override def spec = suite("FioTransactionImportService")(
        test("importTransactions should correctly import transactions from Fio API") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                config = FioConfig(defaultToken = Some("test-token"))
                service = new FioTransactionImportService(
                    mockClient,
                    mockTxRepo,
                    mockSourceRepo,
                    None,
                    None,
                    Some(config)
                )
                count <- service.importFioTransactions(
                    LocalDate.of(2025, 3, 10),
                    LocalDate.of(2025, 3, 15)
                )
                transactions <- mockTxRepo.getAllTransactions
            yield assert(count)(Assertion.equalTo(2)) &&
                assert(transactions.size)(Assertion.equalTo(2)) &&
                assert(transactions.map(_.amount))(Assertion.contains(BigDecimal(50000.0))) &&
                assert(transactions.map(_.amount))(Assertion.contains(BigDecimal(-3000.0))) &&
                assert(transactions.map(_.currency).toSet)(Assertion.equalTo(Set("CZK")))
        },
        test("importNewTransactions should correctly import using transaction ID") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                config = FioConfig(defaultToken = Some("test-token"))
                service = new FioTransactionImportService(
                    mockClient,
                    mockTxRepo,
                    mockSourceRepo,
                    None,
                    None,
                    Some(config)
                )
                count <- service.importNewTransactions(Some(0L))
                transactions <- mockTxRepo.getAllTransactions
            yield assert(count)(Assertion.equalTo(2)) &&
                assert(transactions.size)(Assertion.equalTo(2)) &&
                // Check that we correctly mapped the variable symbol from column5
                assert(transactions.flatMap(_.variableSymbol).toSet)(
                    Assertion.contains("40000000")
                ) &&
                // Check that we correctly mapped the constant symbol from column4
                assert(transactions.flatMap(_.constantSymbol).toSet)(Assertion.contains("3558")) &&
                // Check that we mapped the transaction type
                assert(transactions.map(_.transactionType).toSet)(
                    Assertion.equalTo(Set("Příjem převodem uvnitř banky", "Bezhotovostní platba"))
                )
        },
        test("getFioSourceAccounts should return all source accounts with type FIO") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                config = FioConfig(defaultToken = Some("test-token"))
                service = new FioTransactionImportService(
                    mockClient,
                    mockTxRepo,
                    mockSourceRepo,
                    None,
                    None,
                    Some(config)
                )
                accounts <- service.getFioSourceAccounts()
            yield assert(accounts)(Assertion.equalTo(List(1L)))
        },
        test("should correctly resolve source account ID") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                config = FioConfig(defaultToken = Some("test-token"))
                service = new FioTransactionImportService(
                    mockClient,
                    mockTxRepo,
                    mockSourceRepo,
                    None,
                    None,
                    Some(config)
                )
                count <- service.importFioTransactions(
                    LocalDate.of(2025, 3, 10),
                    LocalDate.of(2025, 3, 15)
                )
                transactions <- mockTxRepo.getAllTransactions
            yield assert(transactions.map(_.id.sourceAccountId).toSet)(Assertion.equalTo(Set(1L)))
        }
    )
end FioTransactionImportServiceSpec
