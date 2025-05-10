package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.{Instant, LocalDate}

object InMemoryImportBatchRepositorySpec extends ZIOSpecDefault:
  // Sample data for testing
  private val accountId = AccountId("bank123", "account456")
  private val validStartDate = LocalDate.now().minusDays(30)
  private val validEndDate = LocalDate.now().minusDays(15)

  // Create a test import batch
  private def createTestBatch(id: ImportBatchId = ImportBatchId.generate()): ImportBatch =
    ImportBatch(
      id = id,
      accountId = accountId,
      startDate = validStartDate,
      endDate = validEndDate,
      status = ImportStatus.NotStarted,
      transactionCount = 0,
      errorMessage = None,
      startTime = Instant.now(),
      endTime = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

  def spec = suite("InMemoryImportBatchRepository")(
    test("should save and retrieve an import batch") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        batch = createTestBatch()
        _ <- repo.save(batch)
        result <- repo.findById(batch.id)
      yield assert(result)(isSome(equalTo(batch)))
    },

    test("should return None for non-existent ID") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        nonExistentId = ImportBatchId.generate()
        result <- repo.findById(nonExistentId)
      yield assert(result)(isNone)
    },

    test("should find batches by account ID") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        batch1 = createTestBatch()
        batch2 = createTestBatch()
        _ <- repo.save(batch1)
        _ <- repo.save(batch2)
        results <- repo.findByAccountId(accountId)
      yield assert(results.size)(equalTo(2)) &&
        assert(results)(contains(batch1)) &&
        assert(results)(contains(batch2))
    },

    test("should find most recent batch by account ID") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        olderBatch = createTestBatch().copy(createdAt = Instant.now().minusSeconds(60))
        newerBatch = createTestBatch().copy(createdAt = Instant.now())
        _ <- repo.save(olderBatch)
        _ <- repo.save(newerBatch)
        result <- repo.findMostRecentByAccountId(accountId)
      yield assert(result)(isSome(equalTo(newerBatch)))
    },

    test("should correctly handle concurrent writes") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        batch = createTestBatch()
        // Create multiple concurrent save operations for the same batch ID but with different statuses
        fiber1 <- repo.save(batch.copy(status = ImportStatus.NotStarted)).fork
        fiber2 <- repo.save(batch.copy(status = ImportStatus.InProgress)).fork
        fiber3 <- repo.save(batch.copy(status = ImportStatus.Completed)).fork
        // Wait for all fibers to complete
        _ <- fiber1.join
        _ <- fiber2.join
        _ <- fiber3.join
        // Read the batch to check the final state
        result <- repo.findById(batch.id)
      yield 
        // One of the statuses should have been saved
        // We can't predict which one due to race conditions, but exactly one should win
        assert(result.map(_.id))(isSome(equalTo(batch.id))) &&
        assert(result.isDefined)(isTrue) &&
        assert(
          result.exists(b =>
            b.status == ImportStatus.NotStarted ||
            b.status == ImportStatus.InProgress ||
            b.status == ImportStatus.Completed
          )
        )(isTrue)
    },

    test("should correctly handle concurrent reads and writes") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        batchId = ImportBatchId.generate()
        // Create a batch with initial status
        initialBatch = createTestBatch(batchId).copy(status = ImportStatus.NotStarted)
        _ <- repo.save(initialBatch)
        
        // Create a fiber that updates the batch status repeatedly
        updateFiber <- ZIO.foreach(1 to 100) { i =>
          val status = if i % 2 == 0 then ImportStatus.InProgress else ImportStatus.Completed
          repo.save(initialBatch.copy(status = status))
        }.fork
        
        // Create a fiber that reads the batch status repeatedly
        readFiber <- ZIO.foreach(1 to 100) { _ =>
          repo.findById(batchId)
        }.fork
        
        // Wait for both fibers to complete
        _ <- updateFiber.join
        results <- readFiber.join
      yield 
        // All reads should return Some result
        assert(results.forall(_.isDefined))(isTrue) &&
        // All reads should have the correct ID
        assert(results.flatMap(_.map(_.id)))(forall(equalTo(batchId)))
    }
  ).provide(InMemoryImportBatchRepository.layer)