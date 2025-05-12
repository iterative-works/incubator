package works.iterative.incubator.budget.infrastructure.persistence

import zio.*
import zio.test.*
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.sqldb.testing.PostgreSQLTestingLayers
import works.iterative.sqldb.testing.MigrateAspects
import works.iterative.sqldb.{FlywayConfig, FlywayMigrationService}
import java.time.{Instant, LocalDate}
import java.util.Currency

/** Integration tests for PostgreSQL repository implementations.
  *
  * These tests run against a real PostgreSQL database in a container to verify that the
  * repositories interact correctly with the database.
  */
object PostgreSQLRepositoriesIT extends ZIOSpecDefault:
    // Test data setup
    val accountId = AccountId("fio", "12345678")
    val importBatchId = ImportBatchId("fio12345", 1L)
    val transactionId = TransactionId(accountId, "tx123")
    val transactionDate = LocalDate.of(2025, 4, 1)
    val amount = Money(BigDecimal("123.45"), Currency.getInstance("CZK"))
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
    end createTestImportBatch

    // Create a test transaction
    def createTestTransaction: Transaction =
        val now = Instant.now
        Transaction(
            id = transactionId,
            date = transactionDate,
            amount = amount,
            description = "Test transaction",
            counterparty = Some("Test counterparty"),
            counterAccount = Some("87654321/1234"),
            reference = Some("VS123456"),
            importBatchId = importBatchId,
            status = TransactionStatus.Imported,
            createdAt = now,
            updatedAt = now
        )
    end createTestTransaction

    // Combined repository layer for testing
    val repositoriesLayer = RepositoryModule.repositories

    // Custom Flyway config that ensures we use the migration scripts from the main module
    val flywayConfig = FlywayConfig(
        // This will ensure we look for migrations in the main module
        locations = List("classpath:db/migration"),
        cleanDisabled = false
    )

    // Configure test layers with our custom Flyway config
    val flywayMigrationLayer = PostgreSQLTestingLayers.dataSourceLayer >>>
        PostgreSQLTestingLayers.postgreSQLDataSourceLayer >>>
        PostgreSQLTestingLayers.transactorLayer >>>
        PostgreSQLTestingLayers.postgreSQLTransactorLayer >+>
        FlywayMigrationService.layerWithConfig(flywayConfig)

    def spec = (suite("PostgreSQL Repository Integration Tests")(
        suite("ImportBatchRepository")(
            test("should store and retrieve an import batch") {
                for
                    // Get repository service
                    repo <- ZIO.service[ImportBatchRepository]

                    // Create test data
                    importBatch = createTestImportBatch

                    // Save the import batch
                    _ <- repo.save(importBatch)

                    // Retrieve the import batch
                    retrieved <- repo.findById(importBatchId)
                yield assertTrue(
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

                    // Find by account ID
                    batches <- repo.findByAccountId(accountId)
                yield assertTrue(
                    batches.nonEmpty,
                    batches.exists(_.id == importBatchId)
                )
            },
            test("should find most recent import batch by account ID") {
                for
                    // Get repository service
                    repo <- ZIO.service[ImportBatchRepository]

                    // Find most recent by account ID
                    mostRecent <- repo.findMostRecentByAccountId(accountId)
                yield assertTrue(
                    mostRecent.isDefined,
                    mostRecent.get.id == importBatchId
                )
            }
        ),
        suite("TransactionRepository")(
            test("should store and retrieve a transaction") {
                for
                    // Get repository services
                    batchRepo <- ZIO.service[ImportBatchRepository]
                    transactionRepo <- ZIO.service[TransactionRepository]

                    // Create test data
                    importBatch = createTestImportBatch
                    transaction = createTestTransaction

                    // Save the import batch first (foreign key constraint)
                    _ <- batchRepo.save(importBatch)

                    // Save the transaction
                    _ <- transactionRepo.save(transaction)

                    // Retrieve the transaction
                    retrieved <- transactionRepo.findById(transactionId)
                yield assertTrue(
                    retrieved.isDefined,
                    retrieved.get.id == transactionId,
                    retrieved.get.date == transactionDate,
                    retrieved.get.amount.amount == amount.amount,
                    retrieved.get.description == "Test transaction",
                    retrieved.get.status == TransactionStatus.Imported
                )
            },
            test("should find transactions by import batch") {
                for
                    // Get repository service
                    transactionRepo <- ZIO.service[TransactionRepository]

                    // Find by import batch
                    transactions <- transactionRepo.findByImportBatch(importBatchId)
                yield assertTrue(
                    transactions.nonEmpty,
                    transactions.exists(_.id == transactionId)
                )
            },
            test("should find transactions by account and date range") {
                for
                    // Get repository service
                    transactionRepo <- ZIO.service[TransactionRepository]

                    // Find by account and date range
                    startDate = transactionDate.minusDays(1)
                    endDate = transactionDate.plusDays(1)
                    transactions <-
                        transactionRepo.findByAccountAndDateRange(accountId, startDate, endDate)
                yield assertTrue(
                    transactions.nonEmpty,
                    transactions.exists(_.id == transactionId)
                )
            },
            test("should update transaction status") {
                for
                    // Get repository service
                    transactionRepo <- ZIO.service[TransactionRepository]

                    // Update status
                    count <- transactionRepo.updateStatusByImportBatch(
                        importBatchId,
                        TransactionStatus.Categorized
                    )

                    // Retrieve the updated transaction
                    retrieved <- transactionRepo.findById(transactionId)
                yield assertTrue(
                    count > 0,
                    retrieved.isDefined,
                    retrieved.get.status == TransactionStatus.Categorized
                )
            }
        ),
        suite("Transaction and ImportBatch integration")(
            test("should handle related operations between repositories") {
                for
                    // Get repository services
                    batchRepo <- ZIO.service[ImportBatchRepository]
                    transactionRepo <- ZIO.service[TransactionRepository]

                    // Create a new import batch with unique ID
                    batchId = ImportBatchId("fio12345", 2L)
                    newBatch = createTestImportBatch.copy(
                        id = batchId,
                        status = ImportStatus.InProgress
                    )

                    // Save the new import batch
                    _ <- batchRepo.save(newBatch)

                    // Create a new transaction linked to this batch
                    txId = TransactionId(accountId, "tx456")
                    newTransaction = createTestTransaction.copy(
                        id = txId,
                        importBatchId = batchId
                    )

                    // Save the transaction
                    _ <- transactionRepo.save(newTransaction)

                    // Find transactions by import batch
                    transactions <- transactionRepo.findByImportBatch(batchId)

                    // Update the batch with transaction count
                    updatedBatch = newBatch.copy(
                        transactionCount = transactions.size,
                        status = ImportStatus.Completed,
                        endTime = Some(Instant.now()),
                        updatedAt = Instant.now()
                    )

                    // Save the updated batch
                    _ <- batchRepo.save(updatedBatch)

                    // Retrieve the updated batch
                    retrievedBatch <- batchRepo.findById(batchId)
                yield assertTrue(
                    transactions.size == 1,
                    transactions.exists(_.id == txId),
                    retrievedBatch.isDefined,
                    retrievedBatch.get.transactionCount == 1,
                    retrievedBatch.get.status == ImportStatus.Completed,
                    retrievedBatch.get.endTime.isDefined
                )
            }
        )
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock @@ MigrateAspects.migrate).provideSomeShared[
        Scope
    ](
        flywayMigrationLayer,
        repositoriesLayer
    )
end PostgreSQLRepositoriesIT
