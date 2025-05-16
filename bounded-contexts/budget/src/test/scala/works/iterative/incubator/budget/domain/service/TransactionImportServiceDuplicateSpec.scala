package works.iterative.incubator.budget.domain.service

import java.time.{Instant, LocalDate}
import java.util.Currency
import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*

object TransactionImportServiceDuplicateSpec extends ZIOSpecDefault:
    // Sample data for tests
    private val accountId = AccountId("test-bank", "123456789")
    private val importBatchId = ImportBatchId("test-account-123", 1L)
    private val testStartDate = LocalDate.now().minusDays(30)
    private val testEndDate = LocalDate.now().minusDays(1)

    // Create test transactions
    private def createTestTransactions(
        count: Int,
        importBatchId: ImportBatchId
    ): List[Transaction] =
        (1 to count).map { i =>
            Transaction(
                id = TransactionId(accountId, s"TX$i"),
                date = testStartDate.plusDays(i % 30),
                amount = Money(BigDecimal(-100 * i), Currency.getInstance("CZK")),
                description = s"Test Transaction $i",
                counterparty = Some(s"Vendor $i"),
                counterAccount = Some(s"654321/$i"),
                reference = Some(s"REF$i"),
                importBatchId = importBatchId,
                status = TransactionStatus.Imported,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }.toList

    // Test implementation of BankTransactionService that returns pre-configured transactions
    private class TestBankService(
        transactions: List[Transaction] = List.empty,
        failValidation: Boolean = false
    ) extends BankTransactionService:
        override def validateDateRange(
            startDate: LocalDate,
            endDate: LocalDate
        ): ZIO[Any, TransactionImportError, Unit] =
            if failValidation then
                ZIO.fail(TransactionImportError.InvalidDateRange("Test validation error"))
            else ZIO.unit

        override def validateDateRangeForAccount(
            accountId: AccountId,
            startDate: LocalDate,
            endDate: LocalDate
        ): ZIO[Any, TransactionImportError, Unit] =
            if failValidation then
                ZIO.fail(TransactionImportError.InvalidDateRange("Test validation error"))
            else ZIO.unit

        override def fetchTransactions(
            accountId: AccountId,
            startDate: LocalDate,
            endDate: LocalDate,
            importBatchId: ImportBatchId
        ): ZIO[Any, Throwable, List[Transaction]] =
            ZIO.succeed(transactions)
    end TestBankService

    // Spec
    def spec = suite("TransactionImportService Duplicate Handling")(
        test("should skip transactions that already exist") {
            // Given: We have 5 existing transactions in the repository
            val existingTransactions = createTestTransactions(5, importBatchId)

            // And: The bank API will return 10 transactions, including the 5 we already have
            val bankTransactions = createTestTransactions(10, importBatchId)

            // Create an in-memory test environment
            val env =
                for
                    txRepo <- Ref.make(existingTransactions)
                    batchRepo <- Ref.make(List.empty[ImportBatch])
                    nextBatchSequence <- Ref.make(1L)
                yield TestEnv(txRepo, batchRepo, nextBatchSequence, bankTransactions)

            // When: We import transactions
            val result =
                for
                    testEnv <- ZIO.service[TestEnv]
                    transactionImportService = createService(testEnv)
                    importResult <- transactionImportService.importTransactions(
                        accountId,
                        testStartDate,
                        testEndDate
                    )
                    finalTxRepo <- ZIO.service[TestEnv].flatMap(_.transactionRepo.get)
                yield (importResult, finalTxRepo.toList)

            // Then: Check that only new transactions were imported
            result.map { case (importBatch, finalTransactions) =>
                assertTrue(
                    // Import batch should have correct count of total transactions
                    importBatch.transactionCount == 10,
                    // We should now have 10 transactions in the repo, not 15
                    finalTransactions.size == 10,
                    // Import batch should be marked as completed
                    importBatch.status == ImportStatus.Completed,
                    // Import batch should have the correct message
                    importBatch.errorMessage.exists(_.contains("skipped 5 duplicates"))
                )
            }.provide(ZLayer.fromZIO(env))
        },
        test("should handle case where all transactions are duplicates") {
            // Given: We have 5 existing transactions in the repository
            val transactions = createTestTransactions(5, importBatchId)

            // And: The bank API returns the exact same 5 transactions
            val bankTransactions = transactions

            // Create an in-memory test environment
            val env =
                for
                    txRepo <- Ref.make(transactions)
                    batchRepo <- Ref.make(List.empty[ImportBatch])
                    nextBatchSequence <- Ref.make(1L)
                yield TestEnv(txRepo, batchRepo, nextBatchSequence, bankTransactions)

            // When: We import transactions
            val result =
                for
                    testEnv <- ZIO.service[TestEnv]
                    transactionImportService = createService(testEnv)
                    importResult <- transactionImportService.importTransactions(
                        accountId,
                        testStartDate,
                        testEndDate
                    )
                    finalTxRepo <- ZIO.service[TestEnv].flatMap(_.transactionRepo.get)
                yield (importResult, finalTxRepo)

            // Then: Check that no new transactions were imported
            result.map { case (importBatch, finalTransactions) =>
                assertTrue(
                    // Import batch should report all 5 transactions
                    importBatch.transactionCount == 5,
                    // We should still have just 5 transactions in the repo
                    finalTransactions.size == 5,
                    // Import batch should be completed
                    importBatch.status == ImportStatus.Completed,
                    // Import batch should have a message about duplicates
                    importBatch.errorMessage.exists(
                        _.contains("All 5 transactions were already imported")
                    )
                )
            }.provide(ZLayer.fromZIO(env))
        }
    )

    // Test environment with in-memory repositories
    case class TestEnv(
        transactionRepo: Ref[List[Transaction]],
        batchRepo: Ref[List[ImportBatch]],
        nextBatchSequence: Ref[Long],
        bankTransactions: List[Transaction]
    )

    // Create in-memory implementation of TransactionRepository
    private def createTransactionRepo(ref: Ref[List[Transaction]]): TransactionRepository =
        new TransactionRepository:
            override def save(transaction: Transaction): ZIO[Any, String, Unit] =
                ref.update(txs => txs.filterNot(_.id == transaction.id) :+ transaction)

            override def saveAll(transactions: List[Transaction]): ZIO[Any, String, Unit] =
                ref.update(txs =>
                    // Remove existing transactions with same IDs
                    val txsWithoutDuplicates =
                        txs.filterNot(tx => transactions.exists(_.id == tx.id))
                    // Add new transactions
                    txsWithoutDuplicates ++ transactions
                )

            override def findById(id: TransactionId): ZIO[Any, String, Option[Transaction]] =
                ref.get.map(_.find(_.id == id))

            override def findByAccountAndDateRange(
                accountId: AccountId,
                startDate: LocalDate,
                endDate: LocalDate
            ): ZIO[Any, String, List[Transaction]] =
                ref.get.map(_.filter(tx =>
                    tx.accountId == accountId &&
                        !tx.date.isBefore(startDate) &&
                        !tx.date.isAfter(endDate)
                ))

            override def findByImportBatch(
                importBatchId: ImportBatchId
            ): ZIO[Any, String, List[Transaction]] =
                ref.get.map(_.filter(_.importBatchId == importBatchId))

            override def updateStatusByImportBatch(
                importBatchId: ImportBatchId,
                status: TransactionStatus
            ): ZIO[Any, String, Int] =
                ref.modify { txs =>
                    val (toUpdate, unchanged) = txs.partition(_.importBatchId == importBatchId)
                    val updatedTxs = toUpdate.map(_.copy(status = status))
                    (toUpdate.size, unchanged ++ updatedTxs)
                }

            override def countByStatus(status: TransactionStatus): ZIO[Any, String, Int] =
                ref.get.map(_.count(_.status == status))

    // Create in-memory implementation of ImportBatchRepository
    private def createImportBatchRepo(
        ref: Ref[List[ImportBatch]], 
        seqRef: Ref[Long]
    ): ImportBatchRepository = new ImportBatchRepository:
        override def save(importBatch: ImportBatch): ZIO[Any, String, Unit] =
            ref.update(batches => batches.filterNot(_.id == importBatch.id) :+ importBatch)
            
        override def findById(id: ImportBatchId): ZIO[Any, String, Option[ImportBatch]] =
            ref.get.map(_.find(_.id == id))
            
        override def findByAccountId(accountId: AccountId): ZIO[Any, String, List[ImportBatch]] =
            ref.get.map(_.filter(_.accountId == accountId))
            
        override def findByDateRange(
            accountId: AccountId,
            startDate: LocalDate, 
            endDate: LocalDate
        ): ZIO[Any, String, List[ImportBatch]] =
            ref.get.map(_.filter(batch => 
                batch.accountId == accountId &&
                !batch.startDate.isAfter(endDate) && !batch.endDate.isBefore(startDate)
            ))
            
        override def findMostRecentByAccountId(
            accountId: AccountId
        ): ZIO[Any, String, Option[ImportBatch]] =
            ref.get.map(_.filter(_.accountId == accountId)
                .sortBy(_.startTime.toEpochMilli)
                .lastOption)
                
        override def findByStatus(
            status: ImportStatus
        ): ZIO[Any, String, List[ImportBatch]] =
            ref.get.map(_.filter(_.status == status))
                
        override def nextSequenceNumber(
            accountId: AccountId
        ): ZIO[Any, String, Long] =
            seqRef.getAndUpdate(_ + 1)

    // Create service for testing
    private def createService(testEnv: TestEnv): TransactionImportService =
        val transactionRepo = createTransactionRepo(testEnv.transactionRepo)
        val importBatchRepo = createImportBatchRepo(testEnv.batchRepo, testEnv.nextBatchSequence)
        val bankService = new TestBankService(testEnv.bankTransactions)

        TransactionImportServiceLive(
            transactionRepo,
            importBatchRepo,
            bankService
        )
    end createService
end TransactionImportServiceDuplicateSpec
