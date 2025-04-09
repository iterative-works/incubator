package works.iterative.incubator.fio.infrastructure.service

import zio.test.*
import zio.test.Assertion.*
import zio.*
import zio.json.*
import zio.nio.file.{Files, Path}
import java.time.{LocalDate, Instant}
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.client.{FioClient, FioCodecs}
import works.iterative.incubator.transactions.domain.model.*
import works.iterative.incubator.transactions.domain.repository.*
import works.iterative.incubator.transactions.domain.query.*
import FioCodecs.given

/**
 * Test suite for FioTransactionImportService
 */
object FioTransactionImportServiceSpec extends ZIOSpecDefault:

    // Mock repositories for testing
    class MockTransactionRepository extends TransactionRepository:
        private val storage = Ref.unsafe.make(Map.empty[TransactionId, Transaction])
        
        override def save(id: TransactionId, transaction: Transaction): Task[Unit] =
            storage.update(_ + (id -> transaction))
            
        override def get(id: TransactionId): Task[Option[Transaction]] =
            storage.get.map(_.get(id))
            
        override def find(query: TransactionQuery): Task[Seq[Transaction]] =
            storage.get.map(_.values.toSeq)
            
        // Additional method for test verification
        def getAllTransactions: Task[Seq[Transaction]] =
            storage.get.map(_.values.toSeq)
    
    class MockSourceAccountRepository extends SourceAccountRepository:
        private val accounts = Seq(
            SourceAccount(
                id = 1L,
                name = "Test Fio Account",
                accountId = "2200000001", 
                bankId = "2010",
                currency = "CZK",
                active = true,
                sourceType = "FIO"
            )
        )
        
        override def save(sourceAccount: SourceAccount): Task[Long] =
            ZIO.succeed(sourceAccount.id)
            
        override def get(id: Long): Task[Option[SourceAccount]] =
            ZIO.succeed(accounts.find(_.id == id))
            
        override def find(query: SourceAccountQuery): Task[Seq[SourceAccount]] =
            ZIO.succeed(
                accounts.filter(account =>
                    query.accountId.forall(_ == account.accountId) &&
                    query.bankId.forall(_ == account.bankId) &&
                    query.sourceType.forall(_ == account.sourceType)
                )
            )
    
    // Mock Fio client for testing
    class MockFioClient extends FioClient:
        override def fetchTransactions(from: LocalDate, to: LocalDate): Task[FioResponse] =
            loadExampleResponse()
            
        override def fetchNewTransactions(lastId: Long): Task[FioResponse] =
            loadExampleResponse()
            
        private def loadExampleResponse(): Task[FioResponse] =
            Files.readAllBytes(Path("bounded-contexts/fio/src/test/resources/example_fio.json"))
                .map(new String(_))
                .flatMap(json => 
                    ZIO.fromEither(json.fromJson[FioResponse])
                        .mapError(err => new RuntimeException(s"Failed to parse test JSON: $err"))
                )

    override def spec = suite("FioTransactionImportService")(
        test("importTransactions should correctly import transactions from Fio API") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                service = new FioTransactionImportService(mockClient, mockTxRepo, mockSourceRepo)
                count <- service.importTransactions(
                    LocalDate.of(2025, 3, 10),
                    LocalDate.of(2025, 3, 15)
                )
                transactions <- mockTxRepo.getAllTransactions
            yield
                assert(count)(equalTo(2)) &&
                assert(transactions.size)(equalTo(2)) &&
                assert(transactions.map(_.amount))(contains(BigDecimal(50000.0))) &&
                assert(transactions.map(_.amount))(contains(BigDecimal(-3000.0))) &&
                assert(transactions.map(_.currency).toSet)(equalTo(Set("CZK")))
        },
        
        test("importNewTransactions should correctly import using transaction ID") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                service = new FioTransactionImportService(mockClient, mockTxRepo, mockSourceRepo)
                count <- service.importNewTransactions(Some(0L))
                transactions <- mockTxRepo.getAllTransactions
            yield
                assert(count)(equalTo(2)) &&
                assert(transactions.size)(equalTo(2)) &&
                // Check that we correctly mapped the variable symbol from column5
                assert(transactions.flatMap(_.variableSymbol).toSet)(contains("40000000")) &&
                // Check that we correctly mapped the constant symbol from column4
                assert(transactions.flatMap(_.constantSymbol).toSet)(contains("3558")) &&
                // Check that we mapped the transaction type
                assert(transactions.map(_.transactionType).toSet)(
                    equalTo(Set("Příjem převodem uvnitř banky", "Bezhotovostní platba"))
                )
        },
        
        test("getFioSourceAccounts should return all source accounts with type FIO") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                service = new FioTransactionImportService(mockClient, mockTxRepo, mockSourceRepo)
                accounts <- service.getFioSourceAccounts()
            yield
                assert(accounts)(equalTo(List(1L)))
        },
        
        test("should correctly resolve source account ID") {
            for
                mockTxRepo <- ZIO.succeed(new MockTransactionRepository())
                mockSourceRepo <- ZIO.succeed(new MockSourceAccountRepository())
                mockClient <- ZIO.succeed(new MockFioClient())
                service = new FioTransactionImportService(mockClient, mockTxRepo, mockSourceRepo)
                count <- service.importTransactions(
                    LocalDate.of(2025, 3, 10),
                    LocalDate.of(2025, 3, 15)
                )
                transactions <- mockTxRepo.getAllTransactions
            yield
                assert(transactions.map(_.id.sourceId).toSet)(equalTo(Set(1L)))
        }
    )