package works.iterative.incubator.budget.infrastructure.persistence

import zio.*
import zio.test.*
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.*
import works.iterative.sqldb.testing.PostgreSQLTestingLayers
import works.iterative.sqldb.testing.MigrateAspects
import works.iterative.sqldb.FlywayMigrationService
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

    // Create a test Fio account
    def createTestFioAccount(id: Long = 1L): FioAccount =
        val now = Instant.now
        FioAccount(
            id = id,
            sourceAccountId = accountId,
            encryptedToken = "encrypted_token_1234567890",
            lastSyncTime = None,
            lastFetchedId = None,
            createdAt = now,
            updatedAt = now
        )
    end createTestFioAccount

    // Combined repository layer for testing
    val repositoriesLayer = RepositoryModule.allRepositories

    def spec = (
        suite("PostgreSQL Repository Integration Tests")(
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
            ) @@ MigrateAspects.migrateOnce,
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
            ) @@ MigrateAspects.migrateOnce,
            suite("FioAccountRepository")(
                test("should store and retrieve a FioAccount") {
                    for
                        // Get repository service
                        repo <- ZIO.service[FioAccountRepository]

                        // Create test data
                        fioAccount = createTestFioAccount()

                        // Save the account
                        _ <- repo.save(fioAccount)

                        // Retrieve the account
                        retrieved <- repo.findById(fioAccount.id)
                    yield assertTrue(
                        retrieved.isDefined,
                        retrieved.get.id == fioAccount.id,
                        retrieved.get.sourceAccountId == accountId,
                        retrieved.get.encryptedToken == "encrypted_token_1234567890",
                        retrieved.get.lastSyncTime.isEmpty,
                        retrieved.get.lastFetchedId.isEmpty
                    )
                },
                test("should find FioAccount by source account ID") {
                    for
                        // Get repository service
                        repo <- ZIO.service[FioAccountRepository]

                        // Find by source account ID
                        account <- repo.findBySourceAccountId(accountId)
                    yield assertTrue(
                        account.isDefined,
                        account.get.sourceAccountId == accountId
                    )
                },
                test("should update an existing FioAccount") {
                    for
                        // Get repository service
                        repo <- ZIO.service[FioAccountRepository]

                        // Retrieve the account first
                        account <- repo.findById(1L).map(_.getOrElse(createTestFioAccount()))

                        // Update the account with sync information
                        now = Instant.now
                        updatedAccount = account.updateSyncState(now, 12345L)

                        // Save the updated account
                        _ <- repo.save(updatedAccount)

                        // Retrieve the updated account
                        retrieved <- repo.findById(1L)
                    yield assertTrue(
                        retrieved.isDefined,
                        retrieved.get.lastSyncTime.isDefined,
                        retrieved.get.lastFetchedId.isDefined,
                        retrieved.get.lastFetchedId.get == 12345L
                    )
                },
                test("should generate an ID greater than 0") {
                    for
                        // Get repository service
                        repo <- ZIO.service[FioAccountRepository]

                        // Generate new ID
                        id <- repo.nextId()
                    yield assertTrue(
                        // We expect the ID to be greater than 0
                        id > 0
                    )
                },
                test("should create a new account with a generated ID") {
                    for
                        // Get repository service
                        repo <- ZIO.service[FioAccountRepository]

                        // Get next ID
                        nextId <- repo.nextId()

                        // Create a new account
                        newAccount <- FioAccountRepository.createAccount(
                            AccountId("fio", "another-account"),
                            "new_encrypted_token_9876543210"
                        )

                        // Retrieve the newly created account
                        retrieved <- repo.findById(newAccount.id)
                    yield assertTrue(
                        retrieved.isDefined,
                        retrieved.get.id == nextId,
                        retrieved.get.sourceAccountId.bankId == "fio",
                        retrieved.get.sourceAccountId.bankAccountId == "another-account",
                        retrieved.get.encryptedToken == "new_encrypted_token_9876543210"
                    )
                }
            ) @@ MigrateAspects.migrateOnce,
            suite("Transaction and ImportBatch integration")(
                test("should handle related operations between repositories") {
                    for
                        // Get repository services
                        batchRepo <- ZIO.service[ImportBatchRepository]
                        transactionRepo <- ZIO.service[TransactionRepository]

                        // Check for any transactions from this account
                        // Use a wide date range to catch all transactions
                        startDate = LocalDate.of(2020, 1, 1)
                        endDate = LocalDate.of(2030, 12, 31)
                        allTransactions <-
                            transactionRepo.findByAccountAndDateRange(accountId, startDate, endDate)
                        _ <- ZIO.when(allTransactions.nonEmpty)(
                            ZIO.fail(
                                s"Expected empty transaction repository, but found ${allTransactions.size} records: ${allTransactions.map(_.id)}"
                            )
                        )

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
            ) @@ MigrateAspects.migrateOnce
        ) @@ TestAspect.withLiveClock @@ TestAspect.sequential
    ).provideSomeShared[Scope](
        PostgreSQLTestingLayers.flywayMigrationServiceLayer,
        repositoriesLayer
    )
end PostgreSQLRepositoriesIT
