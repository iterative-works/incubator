package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.timeout
import java.time.{Instant, LocalDate}

object InMemoryImportBatchRepositorySpec extends ZIOSpecDefault:
  // Sample data for testing
  private val accountId = AccountId("bank123", "account456")
  private val validStartDate = LocalDate.now().minusDays(30)
  private val validEndDate = LocalDate.now().minusDays(15)

  // Create a test import batch
  private def createTestBatch(id: ImportBatchId = ImportBatchId("test-account", 1L)): ImportBatch =
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

  // Helper function to create a batch directly without using ImportBatchRepository.createBatch
  private def createAndSaveBatch(repo: ImportBatchRepository): ZIO[Any, String, ImportBatch] =
    for {
      // Generate the next sequence number using repo
      seqNum <- repo.nextSequenceNumber(accountId)

      // Create batch ID
      batchId = ImportBatchId(accountId.toString.take(8), seqNum)

      // Create the import batch entity
      batchEither = ImportBatch.create(accountId, validStartDate, validEndDate, batchId)
      batch <- ZIO.fromEither(batchEither)

      // Save the batch to the repository
      _ <- repo.save(batch)
    } yield batch

  def spec = suite("InMemoryImportBatchRepository")(
    test("should save and retrieve an import batch") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        // Create a batch with a fixed ID for this test
        batch = ImportBatch(
          id = ImportBatchId("test-save-retrieve", 999L),
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
        _ <- repo.save(batch)
        result <- repo.findById(batch.id)
      yield assertTrue(
        result.isDefined,
        result.get == batch
      )
    },

    test("should return None for non-existent ID") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        nonExistentId = ImportBatchId("nonexistent", 999L)
        result <- repo.findById(nonExistentId)
      yield assert(result)(isNone)
    },

    test("should find batches by account ID") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        // Use direct approach instead of ImportBatchRepository.createBatch
        batch1 <- createAndSaveBatch(repo)
        batch2 <- createAndSaveBatch(repo)
        // Batches are already saved, no need for explicit save
        results <- repo.findByAccountId(accountId)
      yield
        assertTrue(
          results.size >= 2,
          results.contains(batch1),
          results.contains(batch2)
        )
    },

    test("should find most recent batch by account ID") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        // Create first batch
        batch1 <- createAndSaveBatch(repo)

        // Create a second batch with manually set timestamps that are newer
        now = Instant.now()
        laterTimestamp = now.plusSeconds(60) // One minute later

        // Create another batch
        batch2 <- createAndSaveBatch(repo)
        // Update with later timestamp
        _ <- repo.save(batch2.copy(createdAt = laterTimestamp, updatedAt = laterTimestamp))

        // Get most recent batch
        result <- repo.findMostRecentByAccountId(accountId)
      yield
        assertTrue(
          result.isDefined,
          // Verify it's the second batch with later timestamp
          result.get.id.sequenceNumber == batch2.id.sequenceNumber
        )
    },

    test("should correctly handle concurrent writes to the same batch") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        // First create a batch directly
        batch <- createAndSaveBatch(repo)

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
        assertTrue(
          result.isDefined,
          result.map(_.id).contains(batch.id),
          result.exists(b =>
            b.status == ImportStatus.NotStarted ||
            b.status == ImportStatus.InProgress ||
            b.status == ImportStatus.Completed
          )
        )
    },

    test("should correctly handle concurrent reads and writes") {
      for
        repo <- ZIO.service[ImportBatchRepository]

        // Create a batch directly
        initialBatch <- createAndSaveBatch(repo)

        // Create a fiber that updates the batch status repeatedly
        updateFiber <- ZIO.foreach(1 to 100) { i =>
          val status = if i % 2 == 0 then ImportStatus.InProgress else ImportStatus.Completed
          repo.save(initialBatch.copy(status = status))
        }.fork

        // Create a fiber that reads the batch status repeatedly
        readFiber <- ZIO.foreach(1 to 100) { _ =>
          repo.findById(initialBatch.id)
        }.fork

        // Wait for both fibers to complete
        _ <- updateFiber.join
        results <- readFiber.join
      yield
        assertTrue(
          // All reads should return Some result
          results.forall(_.isDefined),
          // All reads should have the correct ID
          results.flatMap(_.map(_.id)).forall(_ == initialBatch.id)
        )
    } @@ timeout(5.seconds),

    test("should generate sequence numbers") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        // Get sequence numbers for the same account
        seq1 <- repo.nextSequenceNumber(accountId)
        seq2 <- repo.nextSequenceNumber(accountId)
        seq3 <- repo.nextSequenceNumber(accountId)

        // Get sequence number for a different account
        otherAccountId = AccountId("other", "account")
        otherSeq1 <- repo.nextSequenceNumber(otherAccountId)
      yield
        assert(seq1)(equalTo(1L)) &&
        assert(seq2)(equalTo(2L)) &&
        assert(seq3)(equalTo(3L)) &&
        assert(otherSeq1)(equalTo(1L)) // Different account starts at 1
    },

    test("should create batches with sequential IDs") {
      for
        repo <- ZIO.service[ImportBatchRepository]

        // Create batches directly
        batch1 <- createAndSaveBatch(repo)
        batch2 <- createAndSaveBatch(repo)

        // Find the batches by ID
        foundBatch1 <- repo.findById(batch1.id)
        foundBatch2 <- repo.findById(batch2.id)
      yield
        assert(batch1.id.accountId.startsWith(accountId.toString.take(8)))(isTrue) &&
        assert(batch1.id.sequenceNumber)(equalTo(1L)) &&
        assert(batch2.id.accountId.startsWith(accountId.toString.take(8)))(isTrue) &&
        assert(batch2.id.sequenceNumber)(equalTo(2L)) &&
        assert(foundBatch1)(isSome(equalTo(batch1))) &&
        assert(foundBatch2)(isSome(equalTo(batch2)))
    },

    test("should handle concurrent batch creation") {
      for
        repo <- ZIO.service[ImportBatchRepository]
        // Create multiple concurrent batches for the same account
        fibers <- ZIO.collectAllPar(
          (1 to 5).map(_ => createAndSaveBatch(repo))
        )

        // Find the batches by account
        batches <- repo.findByAccountId(accountId)
      yield
        // All batches should have been created with unique sequence numbers
        assert(batches.size)(equalTo(5)) &&
        // Sequence numbers should be 1 through 5, but may not be in order due to concurrent creation
        assert(batches.map(_.id.sequenceNumber).sorted)(equalTo(List(1L, 2L, 3L, 4L, 5L)))
    }
  ).provide(InMemoryImportBatchRepository.layer) @@ timeout(10.seconds)
