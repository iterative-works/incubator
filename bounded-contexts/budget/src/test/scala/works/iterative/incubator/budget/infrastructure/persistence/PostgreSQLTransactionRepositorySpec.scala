package works.iterative.incubator.budget.infrastructure.persistence

import zio.*
import zio.test.*
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.TransactionRepository
import works.iterative.sqldb.testing.PostgreSQLTestingLayers
import works.iterative.sqldb.testing.MigrateAspects
import java.time.{Instant, LocalDate}
import java.util.Currency

object PostgreSQLTransactionRepositorySpec extends ZIOSpecDefault:
    // Test data setup
    val accountId = AccountId("fio", "12345678")
    val importBatchId = ImportBatchId("fio12345", 1L)
    val transactionId = TransactionId(accountId, "tx123")
    val transactionDate = LocalDate.of(2025, 4, 1)
    val amount = Money(BigDecimal("123.45"), Currency.getInstance("CZK"))
    
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

    // Repository layer for testing
    val repositoryLayer = PostgreSQLTransactionRepository.layer

    def spec = (suite("PostgreSQLTransactionRepository")(
        test("should save and retrieve a transaction") {
            for
                // Get repository service
                repo <- ZIO.service[TransactionRepository]
                
                // Create test data
                transaction = createTestTransaction
                
                // Save the transaction
                _ <- repo.save(transaction)
                
                // Retrieve the transaction
                retrieved <- repo.findById(transactionId)
            yield
                assertTrue(
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
                repo <- ZIO.service[TransactionRepository]
                
                // Create test data
                transaction = createTestTransaction
                
                // Save the transaction
                _ <- repo.save(transaction)
                
                // Find by import batch
                transactions <- repo.findByImportBatch(importBatchId)
            yield
                assertTrue(
                    transactions.nonEmpty,
                    transactions.exists(_.id == transactionId)
                )
        },
        
        test("should find transactions by account and date range") {
            for
                // Get repository service
                repo <- ZIO.service[TransactionRepository]
                
                // Create test data
                transaction = createTestTransaction
                
                // Save the transaction
                _ <- repo.save(transaction)
                
                // Find by account and date range
                startDate = transactionDate.minusDays(1)
                endDate = transactionDate.plusDays(1)
                transactions <- repo.findByAccountAndDateRange(accountId, startDate, endDate)
            yield
                assertTrue(
                    transactions.nonEmpty,
                    transactions.exists(_.id == transactionId)
                )
        },
        
        test("should update transaction status") {
            for
                // Get repository service
                repo <- ZIO.service[TransactionRepository]
                
                // Create test data
                transaction = createTestTransaction
                
                // Save the transaction
                _ <- repo.save(transaction)
                
                // Update status
                count <- repo.updateStatusByImportBatch(importBatchId, TransactionStatus.Categorized)
                
                // Retrieve the updated transaction
                retrieved <- repo.findById(transactionId)
            yield
                assertTrue(
                    count > 0,
                    retrieved.isDefined,
                    retrieved.get.status == TransactionStatus.Categorized
                )
        }
    ) @@ TestAspect.sequential @@ TestAspect.withLiveClock @@ MigrateAspects.migrate).provideSomeShared[
        Scope
    ](
        PostgreSQLTestingLayers.flywayMigrationServiceLayer,
        repositoryLayer
    )
end PostgreSQLTransactionRepositorySpec