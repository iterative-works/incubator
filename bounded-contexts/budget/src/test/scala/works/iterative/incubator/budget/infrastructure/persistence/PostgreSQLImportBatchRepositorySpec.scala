package works.iterative.incubator.budget.infrastructure.persistence

import zio.*
import zio.test.*
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.ImportBatchRepository
import works.iterative.sqldb.testing.PostgreSQLTestingLayers
import works.iterative.sqldb.testing.MigrateAspects
import java.time.{Instant, LocalDate}

object PostgreSQLImportBatchRepositorySpec extends ZIOSpecDefault:
    // Test data setup
    val accountId = AccountId("fio", "12345678")
    val importBatchId = ImportBatchId("fio12345", 1L)
    val startDate = LocalDate.of(2025, 4, 1)
    val endDate = LocalDate.of(2025, 4, 15)
    
    // Create a test import batch
    def createTestImportBatch: ImportBatch =
        val now = Instant.now
        ImportBatch(
            id = importBatchId,
            accountId = accountId,
            startDate = startDate,
            endDate = endDate,
            status = ImportStatus.NotStarted,
            transactionCount = 0,
            errorMessage = None,
            startTime = now,
            endTime = None,
            createdAt = now,
            updatedAt = now
        )

    // Repository layer for testing
    val repositoryLayer = PostgreSQLImportBatchRepository.layer

    def spec = (suite("PostgreSQLImportBatchRepository")(
        test("should save and retrieve an import batch") {
            for
                // Get repository service
                repo <- ZIO.service[ImportBatchRepository]
                
                // Create test data
                importBatch = createTestImportBatch
                
                // Save the import batch
                _ <- repo.save(importBatch)
                
                // Retrieve the import batch
                retrieved <- repo.findById(importBatchId)
            yield
                assertTrue(
                    retrieved.isDefined,
                    retrieved.get.id == importBatchId,
                    retrieved.get.accountId == accountId,
                    retrieved.get.startDate == startDate,
                    retrieved.get.endDate == endDate,
                    retrieved.get.status == ImportStatus.NotStarted
                )
        },
        
        test("should find import batches by account ID") {
            for
                // Get repository service
                repo <- ZIO.service[ImportBatchRepository]
                
                // Create test data
                importBatch = createTestImportBatch
                
                // Save the import batch
                _ <- repo.save(importBatch)
                
                // Find by account ID
                batches <- repo.findByAccountId(accountId)
            yield
                assertTrue(
                    batches.nonEmpty,
                    batches.exists(_.id == importBatchId)
                )
        },
        
        test("should find most recent import batch by account ID") {
            for
                // Get repository service
                repo <- ZIO.service[ImportBatchRepository]
                
                // Create test data
                importBatch = createTestImportBatch
                
                // Save the import batch
                _ <- repo.save(importBatch)
                
                // Find most recent by account ID
                mostRecent <- repo.findMostRecentByAccountId(accountId)
            yield
                assertTrue(
                    mostRecent.isDefined,
                    mostRecent.get.id == importBatchId
                )
        },
        
        test("should find import batches by date range") {
            for
                // Get repository service
                repo <- ZIO.service[ImportBatchRepository]
                
                // Create test data
                importBatch = createTestImportBatch
                
                // Save the import batch
                _ <- repo.save(importBatch)
                
                // Find by date range
                queryStartDate = startDate.plusDays(2)
                queryEndDate = endDate.minusDays(2)
                batches <- repo.findByDateRange(accountId, queryStartDate, queryEndDate)
            yield
                assertTrue(
                    batches.nonEmpty,
                    batches.exists(_.id == importBatchId)
                )
        },
        
        test("should generate next sequence number") {
            for
                // Get repository service
                repo <- ZIO.service[ImportBatchRepository]
                
                // Create test data
                importBatch = createTestImportBatch
                
                // Save the import batch
                _ <- repo.save(importBatch)
                
                // Get next sequence number
                nextSeqNum <- repo.nextSequenceNumber(accountId)
            yield
                assertTrue(nextSeqNum > 1)
        }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock @@ MigrateAspects.migrate).provideSomeShared[
        Scope
    ](
        PostgreSQLTestingLayers.flywayMigrationServiceLayer,
        repositoryLayer
    )
end PostgreSQLImportBatchRepositorySpec