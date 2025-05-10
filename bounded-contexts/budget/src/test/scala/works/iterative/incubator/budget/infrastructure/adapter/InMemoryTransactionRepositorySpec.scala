package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.TransactionRepository
import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.{Instant, LocalDate}
import java.util.Currency

object InMemoryTransactionRepositorySpec extends ZIOSpecDefault:
  // Sample data for testing
  private val accountId = AccountId("bank123", "account456")
  private val importBatchId = ImportBatchId.generate()
  private val transactionDate = LocalDate.now().minusDays(7)
  
  // Create a test transaction
  private def createTestTransaction(
    id: TransactionId = TransactionId.generate(),
    status: TransactionStatus = TransactionStatus.Imported
  ): Transaction =
    Transaction(
      id = id,
      accountId = accountId,
      date = transactionDate,
      amount = Money(BigDecimal(-100), Currency.getInstance("CZK")),
      description = "Test transaction",
      counterparty = Some("Test Merchant"),
      counterAccount = Some("1234567890/1234"),
      reference = Some("REF123"),
      importBatchId = importBatchId,
      status = status,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  def spec = suite("InMemoryTransactionRepository")(
    test("should save and retrieve a transaction") {
      for
        repo <- ZIO.service[TransactionRepository]
        tx = createTestTransaction()
        _ <- repo.save(tx)
        result <- repo.findById(tx.id)
      yield assert(result)(isSome(equalTo(tx)))
    },

    test("should return None for non-existent ID") {
      for
        repo <- ZIO.service[TransactionRepository]
        nonExistentId = TransactionId.generate()
        result <- repo.findById(nonExistentId)
      yield assert(result)(isNone)
    },

    test("should find transactions by account and date range") {
      for
        repo <- ZIO.service[TransactionRepository]
        tx1 = createTestTransaction()
        tx2 = createTestTransaction()
        _ <- repo.saveAll(List(tx1, tx2))
        startDate = transactionDate.minusDays(1)
        endDate = transactionDate.plusDays(1)
        results <- repo.findByAccountAndDateRange(accountId, startDate, endDate)
      yield assertTrue(
        results.size == 2,
        results.contains(tx1),
        results.contains(tx2)
      )
    },

    test("should find transactions by import batch") {
      for
        repo <- ZIO.service[TransactionRepository]
        tx1 = createTestTransaction()
        tx2 = createTestTransaction()
        _ <- repo.saveAll(List(tx1, tx2))
        results <- repo.findByImportBatch(importBatchId)
      yield assertTrue(
        results.size == 2,
        results.contains(tx1),
        results.contains(tx2)
      )
    },

    test("should update status of transactions by import batch") {
      for
        repo <- ZIO.service[TransactionRepository]
        tx1 = createTestTransaction()
        tx2 = createTestTransaction()
        _ <- repo.saveAll(List(tx1, tx2))
        updatedCount <- repo.updateStatusByImportBatch(importBatchId, TransactionStatus.Categorized)
        results <- repo.findByImportBatch(importBatchId)
      yield assertTrue(
        updatedCount == 2,
        results.forall(_.status == TransactionStatus.Categorized)
      )
    },

    test("should count transactions by status") {
      for
        repo <- ZIO.service[TransactionRepository]
        tx1 = createTestTransaction(status = TransactionStatus.Imported)
        tx2 = createTestTransaction(status = TransactionStatus.Categorized)
        tx3 = createTestTransaction(status = TransactionStatus.Imported)
        _ <- repo.saveAll(List(tx1, tx2, tx3))
        importedCount <- repo.countByStatus(TransactionStatus.Imported)
        categorizedCount <- repo.countByStatus(TransactionStatus.Categorized)
      yield assertTrue(
        importedCount == 2,
        categorizedCount == 1
      )
    },

    test("should handle concurrent saves of transactions with the same ID") {
      for
        repo <- ZIO.service[TransactionRepository]
        txId = TransactionId.generate()
        
        // Create multiple transactions with the same ID but different statuses
        tx1 = createTestTransaction(id = txId, status = TransactionStatus.Imported)
        tx2 = createTestTransaction(id = txId, status = TransactionStatus.Categorized)
        tx3 = createTestTransaction(id = txId, status = TransactionStatus.Validated)
        
        // Execute concurrent saves
        fiber1 <- repo.save(tx1).fork
        fiber2 <- repo.save(tx2).fork
        fiber3 <- repo.save(tx3).fork
        
        // Wait for all saves to complete
        _ <- fiber1.join
        _ <- fiber2.join
        _ <- fiber3.join
        
        // Get the transaction to see which status was saved
        result <- repo.findById(txId)
      yield
        // The transaction ID must match
        assertTrue(
          result.map(_.id).contains(txId),
          // One of the statuses must be saved (which one depends on race conditions)
          result.map(_.status).isDefined,
          result.exists(tx =>
            tx.status == TransactionStatus.Imported ||
            tx.status == TransactionStatus.Categorized ||
            tx.status == TransactionStatus.Validated
          )
        )
    },

    test("should handle concurrent saveAll operations atomically") {
      for
        repo <- ZIO.service[TransactionRepository]
        
        // Create multiple batches of transactions with overlapping IDs
        batch1 = (1 to 10).map(_ => createTestTransaction()).toList
        batch2 = (1 to 10).map(_ => createTestTransaction()).toList
        
        // Create a third batch that contains updated versions of some transactions from batch1
        commonTxIds = batch1.take(5).map(_.id)
        batch3 = commonTxIds.map(id => 
          createTestTransaction(id = id, status = TransactionStatus.Categorized)
        )
        
        // Execute concurrent saveAll operations
        fiber1 <- repo.saveAll(batch1).fork
        fiber2 <- repo.saveAll(batch2).fork
        fiber3 <- repo.saveAll(batch3).fork
        
        // Wait for all saves to complete
        _ <- fiber1.join
        _ <- fiber2.join
        _ <- fiber3.join
        
        // Count total transactions (should be batch1 + batch2 = 20 total)
        // The 5 overlapping transactions should have one status or the other
        allTxs <- ZIO.foreach(batch1.map(_.id) ++ batch2.map(_.id).distinct)(repo.findById)
        
        // Check the status of the overlapping transactions
        overlappingTxs <- ZIO.foreach(commonTxIds)(repo.findById)
      yield
        // The actual count will be equal to the sum of distinct transaction IDs
        // because concurrent operations can cause some transactions to be overwritten
        assert(allTxs.flatten.size)(equalTo((batch1.map(_.id) ++ batch2.map(_.id)).distinct.size)) &&
        assert(overlappingTxs.forall(_.isDefined))(isTrue) &&
        assert(overlappingTxs.flatten.forall(tx =>
          tx.status == TransactionStatus.Imported || tx.status == TransactionStatus.Categorized
        ))(isTrue)
    },

    test("should handle concurrent reads and status updates") {
      for
        repo <- ZIO.service[TransactionRepository]
        
        // Create a batch of transactions with the same import batch ID
        txs = (1 to 100).map(_ => createTestTransaction()).toList
        _ <- repo.saveAll(txs)
        
        // Create a fiber that updates status repeatedly
        updateFiber <- ZIO.foreach(1 to 5) { i =>
          val status = if i % 2 == 0 then TransactionStatus.Categorized else TransactionStatus.Validated
          repo.updateStatusByImportBatch(importBatchId, status)
        }.fork
        
        // Create a fiber that reads transactions repeatedly
        readFiber <- ZIO.foreach(1 to 100) { _ =>
          repo.findByImportBatch(importBatchId)
        }.fork
        
        // Wait for both fibers to complete
        _ <- updateFiber.join
        results <- readFiber.join
      yield
        // Use ZIO Test's assertion builder for combining assertions
        assert(results.forall(_.size == txs.size))(isTrue) &&
        assert(results.forall(result =>
          result.map(_.status).distinct.size == 1
        ))(isTrue)
    }
  ).provide(InMemoryTransactionRepository.layer)