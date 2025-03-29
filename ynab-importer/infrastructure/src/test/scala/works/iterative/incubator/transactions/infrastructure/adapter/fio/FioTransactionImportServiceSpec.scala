package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.*
import zio.test.*
import zio.test.Assertion.*
import service.{TransactionRepository, SourceAccountRepository}
import java.time.LocalDate

object FioTransactionImportServiceSpec extends ZIOSpecDefault:

    // Sample data
    val sampleAccountId = "2200000001"
    val sampleBankId = "2010"
    val sourceAccountId = 1L

    // Create mock FioClient that returns predefined responses
    class MockFioClient extends FioClient:
        override def fetchTransactions(from: LocalDate, to: LocalDate): Task[FioResponse] =
            ZIO.succeed(createSampleResponse())

        override def fetchNewTransactions(lastId: Long): Task[FioResponse] =
            ZIO.succeed(createSampleResponse())
    end MockFioClient

    // Create an in-memory transaction repository for testing
    class MockTransactionRepository extends TransactionRepository:
        private val transactions = Unsafe.unsafe { implicit unsafe =>
            Ref.unsafe.make(Map.empty[TransactionId, Transaction])
        }

        override def save(id: TransactionId, transaction: Transaction): UIO[Unit] =
            transactions.update(_ + (id -> transaction))

        override def load(id: TransactionId): UIO[Option[Transaction]] =
            transactions.get.map(_.get(id))

        override def find(query: TransactionQuery): UIO[Seq[Transaction]] =
            transactions.get.map(_.values.toSeq)
    end MockTransactionRepository

    // Create a mock source account repository that counts database calls
    class MockSourceAccountRepository extends SourceAccountRepository:
        private val sourceAccounts = Unsafe.unsafe { implicit unsafe =>
            Ref.unsafe.make(Map(sourceAccountId -> createSampleSourceAccount()))
        }
        private val dbCallCounter = Unsafe.unsafe { implicit unsafe =>
            Ref.unsafe.make(0)
        }
        private val idCounter = Unsafe.unsafe { implicit unsafe =>
            Ref.unsafe.make(sourceAccountId + 1L)
        }

        // Method to get the current call count
        def getCallCount: UIO[Int] = dbCallCounter.get

        // Method to reset the call counter
        def resetCallCounter: UIO[Unit] = dbCallCounter.set(0)

        override def save(id: Long, account: SourceAccount): UIO[Unit] =
            sourceAccounts.update(_ + (id -> account))

        override def load(id: Long): UIO[Option[SourceAccount]] =
            sourceAccounts.get.map(_.get(id))

        override def find(query: SourceAccountQuery): UIO[Seq[SourceAccount]] =
            dbCallCounter.update(_ + 1) *>
                sourceAccounts.get.map { accounts =>
                    accounts.values
                        .filter { acc =>
                            query.accountId.forall(_ == acc.accountId) &&
                            query.bankId.forall(_ == acc.bankId)
                        }
                        .toSeq
                }
                
        override def create(value: CreateSourceAccount): UIO[Long] =
            for
                nextId <- idCounter.getAndUpdate(_ + 1)
                _ <- sourceAccounts.update(_ + (nextId -> SourceAccount(
                    id = nextId,
                    accountId = value.accountId,
                    bankId = value.bankId,
                    name = value.name,
                    currency = value.currency,
                    ynabAccountId = value.ynabAccountId,
                    active = value.active
                )))
            yield nextId
    end MockSourceAccountRepository

    // Helper methods to create test data
    def createSampleSourceAccount(): SourceAccount =
        SourceAccount(
            id = sourceAccountId,
            accountId = sampleAccountId,
            bankId = sampleBankId,
            name = "Test Account",
            currency = "CZK",
            ynabAccountId = None,
            active = true,
            lastSyncTime = None
        )

    def createSampleResponse(): FioResponse =
        FioResponse(
            accountStatement = FioAccountStatement(
                info = FioStatementInfo(
                    accountId = sampleAccountId,
                    bankId = sampleBankId,
                    currency = "CZK",
                    iban = "CZ7920100000002200000001",
                    bic = "FIOBCZPPXXX",
                    openingBalance = 100000.0,
                    closingBalance = 147000.0,
                    dateStart = "2025-03-01+0100",
                    dateEnd = "2025-03-31+0100",
                    yearList = None,
                    idList = None,
                    idFrom = 0L,
                    idTo = 0L,
                    idLastDownload = None
                ),
                transactionList = FioTransactionList(
                    transaction = List(
                        // Transaction 1
                        FioTransaction(
                            column0 = Some(FioTransactionValue("2025-03-14+0100", "Datum", 0)),
                            column1 = Some(FioTransactionValue(50000.0, "Objem", 1)),
                            column2 = Some(FioTransactionValue("123456789", "Protiúčet", 2)),
                            column3 = Some(FioTransactionValue("0800", "Kód banky", 3)),
                            column4 = Some(FioTransactionValue("0558", "KS", 4)),
                            column5 = Some(FioTransactionValue("0000000001", "VS", 5)),
                            column6 = Some(FioTransactionValue("0000000001", "SS", 6)),
                            column7 = Some(FioTransactionValue(
                                "Company A",
                                "Uživatelská identifikace",
                                7
                            )),
                            column8 = Some(FioTransactionValue("Payment", "Typ", 8)),
                            column9 = None,
                            column10 =
                                Some(FioTransactionValue("Invoice 001", "Zpráva pro příjemce", 10)),
                            column12 = Some(FioTransactionValue("Some Bank", "Název banky", 12)),
                            column14 = Some(FioTransactionValue("CZK", "Měna", 14)),
                            column16 = None,
                            column17 = None,
                            column18 = None,
                            column22 = Some(FioTransactionValue(26962199069L, "ID pohybu", 22)),
                            column25 = Some(FioTransactionValue("Comment 1", "Komentář", 25)),
                            column26 = None,
                            column27 = None
                        ),
                        // Transaction 2
                        FioTransaction(
                            column0 = Some(FioTransactionValue("2025-03-15+0100", "Datum", 0)),
                            column1 = Some(FioTransactionValue(-3000.0, "Objem", 1)),
                            column2 = Some(FioTransactionValue("987654321", "Protiúčet", 2)),
                            column3 = Some(FioTransactionValue("0100", "Kód banky", 3)),
                            column4 = None,
                            column5 = Some(FioTransactionValue("0000000002", "VS", 5)),
                            column6 = None,
                            column7 = Some(FioTransactionValue(
                                "Company B",
                                "Uživatelská identifikace",
                                7
                            )),
                            column8 = Some(FioTransactionValue("Withdrawal", "Typ", 8)),
                            column9 = None,
                            column10 =
                                Some(FioTransactionValue("Invoice 002", "Zpráva pro příjemce", 10)),
                            column12 = Some(FioTransactionValue("Other Bank", "Název banky", 12)),
                            column14 = Some(FioTransactionValue("CZK", "Měna", 14)),
                            column16 = None,
                            column17 = None,
                            column18 = None,
                            column22 = Some(FioTransactionValue(26962199070L, "ID pohybu", 22)),
                            column25 = Some(FioTransactionValue("Comment 2", "Komentář", 25)),
                            column26 = None,
                            column27 = None
                        )
                    )
                )
            )
        )

    // Instead of using ZLayers directly, we'll create a test environment function
    // that provides all the required services
    def testEnv(): ZIO[
        Any,
        Nothing,
        (FioTransactionImportService, TransactionRepository, MockSourceAccountRepository)
    ] =
        for
            fioClient <- ZIO.succeed(new MockFioClient())
            txRepo <- ZIO.succeed(new MockTransactionRepository())
            sourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
            importService <-
                ZIO.succeed(new FioTransactionImportService(fioClient, txRepo, sourceRepo))
        yield (importService, txRepo, sourceRepo)

    def spec = suite("FioTransactionImportService")(
        test("should import transactions and create domain model") {
            for
                env <- testEnv()
                (importService, transactionRepo, _) = env

                count <- importService.importTransactions(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 3, 31)
                )
                transactions <- transactionRepo.find(TransactionQuery())
            yield assertTrue(
                count == 2,
                transactions.size == 2,
                transactions.exists(_.id.transactionId == "26962199069"),
                transactions.exists(_.id.transactionId == "26962199070")
            )
        },
        test("should use cache for repeated source account lookups") {
            for
                env <- testEnv()
                (importService, _, mockRepo) = env

                _ <- mockRepo.resetCallCounter

                // First import should query the database
                _ <- importService.importTransactions(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 3, 31)
                )
                firstCallCount <- mockRepo.getCallCount

                // Reset counter for second test
                _ <- mockRepo.resetCallCounter

                // Second import with same data without clearing cache
                // (manually bypassing the cache clear)
                response = createSampleResponse()
                // Access the private method via reflection
                transactions <- ZIO.attempt {
                    val method = importService.getClass.getDeclaredMethod(
                        "mapFioTransactionsToModel",
                        classOf[FioResponse]
                    )
                    method.setAccessible(true)
                    method.invoke(importService, response).asInstanceOf[Task[List[Transaction]]]
                }.flatMap(identity)

                secondCallCount <- mockRepo.getCallCount
            yield assertTrue(
                firstCallCount == 1, // First should query database
                secondCallCount == 0, // Second should use cache
                transactions.length == 2 // Should still process transactions correctly
            )
        },
        test("should clear cache at beginning of each import batch") {
            for
                env <- testEnv()
                (importService, _, mockRepo) = env

                // First import
                _ <- mockRepo.resetCallCounter
                _ <- importService.importTransactions(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 3, 31)
                )
                firstCallCount <- mockRepo.getCallCount

                // Second import (should clear cache and query again)
                _ <- mockRepo.resetCallCounter
                _ <- importService.importTransactions(
                    LocalDate.of(2025, 3, 1),
                    LocalDate.of(2025, 3, 31)
                )
                secondCallCount <- mockRepo.getCallCount
            yield assertTrue(
                firstCallCount == 1, // First call hits database
                secondCallCount == 1 // Second call should also hit database due to cache clearing
            )
        }
    )
end FioTransactionImportServiceSpec
