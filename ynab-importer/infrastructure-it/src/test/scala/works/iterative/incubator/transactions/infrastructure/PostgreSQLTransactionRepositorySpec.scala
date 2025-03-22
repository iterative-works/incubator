package works.iterative.incubator.transactions
package infrastructure

import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.augustnagro.magnum.magzio.*
import java.time.{LocalDate, Instant}
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import service.{
    TransactionRepository,
    TransactionProcessingStateRepository,
    SourceAccountRepository
}
import zio.test.TestAspect.sequential

object PostgreSQLTransactionRepositorySpec extends ZIOSpecDefault:
    private val postgresImage = DockerImageName.parse("postgres:17-alpine")

    // Define a layer for test container
    val postgresContainer =
        ZLayer {
            ZIO.acquireRelease {
                ZIO.attempt {
                    val container = PostgreSQLContainer(postgresImage)
                    container.start()
                    container
                }
            }(container =>
                ZIO.attempt {
                    container.stop()
                }.orDie
            )
        }

    // Create a DataSource from the container
    val dataSourceLayer =
        postgresContainer >>> ZLayer.fromZIO {
            for
                container <- ZIO.service[PostgreSQLContainer]
                _ <- ZIO.attempt(Class.forName("org.postgresql.Driver"))
                dataSource <- ZIO.acquireRelease(ZIO.attempt {
                    val config = new HikariConfig()
                    config.setJdbcUrl(container.jdbcUrl)
                    config.setUsername(container.username)
                    config.setPassword(container.password)
                    config.setMaximumPoolSize(5)
                    new HikariDataSource(config)
                })(dataSource => ZIO.attempt(dataSource.close()).orDie)
            yield dataSource
        }

    // Create a Transactor from the DataSource
    val transactorLayer =
        dataSourceLayer.flatMap(env => Transactor.layer(env.get[DataSource]))

    // Setup the repository layers for testing
    val transactionRepositoryLayer =
        transactorLayer >+> ZLayer {
            for
                transactor <- ZIO.service[Transactor]
            yield PostgreSQLTransactionRepository(transactor)
        }

    val processingStateRepositoryLayer =
        transactorLayer >+> ZLayer {
            for
                transactor <- ZIO.service[Transactor]
            yield PostgreSQLTransactionProcessingStateRepository(transactor)
        }

    val sourceAccountRepositoryLayer =
        transactorLayer >+> ZLayer {
            for
                transactor <- ZIO.service[Transactor]
            yield PostgreSQLSourceAccountRepository(transactor)
        }

    val repositoryLayer =
        transactionRepositoryLayer ++
            processingStateRepositoryLayer ++
            sourceAccountRepositoryLayer

    // Create PostgreSQLDataSource from DataSource for Flyway
    val postgreSQLDataSourceLayer = dataSourceLayer >>> ZLayer {
        ZIO.service[DataSource].map(ds => PostgreSQLDataSource(ds))
    }

    // Create FlywayMigrationService layer
    val flywayMigrationServiceLayer = postgreSQLDataSourceLayer >>> FlywayMigrationService.layer

    // Use Flyway to manage schema setup and teardown for tests
    val setupDbSchema = ZIO.scoped {
        for
            // Get the migration service
            migrationService <-
                ZIO.service[FlywayMigrationService].provideSome[Scope](flywayMigrationServiceLayer)
            // First clean the database to ensure a fresh state
            _ <- migrationService.clean()
            // Then run migrations to set up the schema
            result <- migrationService.migrate()
            _ <- ZIO.log(s"Migration complete: ${result.migrationsExecuted} migrations executed")
        yield ()
    }

    // Create sample transaction for testing
    def createSampleTransaction: Transaction =
        Transaction(
            id = TransactionId(sourceAccountId = 1L, transactionId = "TX123"),
            date = LocalDate.now(),
            amount = BigDecimal("500.00"),
            currency = "CZK",
            counterAccount = Some("987654"),
            counterBankCode = Some("0100"),
            counterBankName = Some("Test Bank"),
            variableSymbol = Some("VS123"),
            constantSymbol = Some("CS123"),
            specificSymbol = Some("SS123"),
            userIdentification = Some("User Test"),
            message = Some("Test payment"),
            transactionType = "PAYMENT",
            comment = Some("Test comment"),
            importedAt = Instant.now()
        )

    // Create sample transaction processing state for testing
    def createSampleProcessingState(transaction: Transaction): TransactionProcessingState =
        TransactionProcessingState.initial(transaction)

    def spec = suite("PostgreSQLTransactionRepository")(
        test("should save and retrieve a transaction") {
            for
                // Setup
                _ <- setupDbSchema
                transactionRepo <- ZIO.service[TransactionRepository]
                stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                // Create a source account first (to satisfy foreign key)
                sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                sourceAccount = SourceAccount(
                    id = 1L,
                    accountId = "123456",
                    bankId = "0800",
                    name = "Test Account",
                    currency = "CZK"
                )
                _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                // Create a transaction
                transaction = createSampleTransaction
                processingState = createSampleProcessingState(transaction)

                // Execute
                _ <- transactionRepo.save(transaction.id, transaction)
                _ <- stateRepo.save(processingState.transactionId, processingState)
                retrievedTx <- transactionRepo.load(transaction.id)
                retrievedState <- stateRepo.load(transaction.id)
            yield
            // Assert
            assertTrue(
                retrievedTx.isDefined,
                retrievedTx.get.id == transaction.id,
                retrievedTx.get.amount == transaction.amount,
                retrievedTx.get.date == transaction.date,
                retrievedState.isDefined,
                retrievedState.get.status == TransactionStatus.Imported
            )
        },
        test("should find transactions by query") {
            for
                // Setup
                _ <- setupDbSchema
                transactionRepo <- ZIO.service[TransactionRepository]
                stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                // Create a source account first (to satisfy foreign key)
                sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                sourceAccount = SourceAccount(
                    id = 1L,
                    accountId = "123456",
                    bankId = "0800",
                    name = "Test Account",
                    currency = "CZK"
                )
                _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                transaction1 = createSampleTransaction
                transaction2 = createSampleTransaction.copy(
                    id = TransactionId(sourceAccountId = 1L, transactionId = "TX124"),
                    amount = BigDecimal("1000.00")
                )
                processingState1 = createSampleProcessingState(transaction1)
                processingState2 = createSampleProcessingState(transaction2)

                // Execute
                _ <- transactionRepo.save(transaction1.id, transaction1)
                _ <- transactionRepo.save(transaction2.id, transaction2)
                _ <- stateRepo.save(processingState1.transactionId, processingState1)
                _ <- stateRepo.save(processingState2.transactionId, processingState2)
                results <- transactionRepo.find(TransactionQuery())
            yield
            // Assert
            assertTrue(
                results.size >= 2,
                results.exists(t => t.id == transaction1.id),
                results.exists(t => t.id == transaction2.id)
            )
        },
        test("should find transactions by filter") {
            for
                // Setup
                _ <- setupDbSchema
                transactionRepo <- ZIO.service[TransactionRepository]
                stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                // Create a source account first (to satisfy foreign key)
                sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                sourceAccount = SourceAccount(
                    id = 1L,
                    accountId = "123456",
                    bankId = "0800",
                    name = "Test Account",
                    currency = "CZK"
                )
                _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                transaction1 = createSampleTransaction
                transaction2 = createSampleTransaction.copy(
                    id = TransactionId(sourceAccountId = 1L, transactionId = "TX124"),
                    amount = BigDecimal("1000.00")
                )
                processingState1 = createSampleProcessingState(transaction1)
                processingState2 = createSampleProcessingState(transaction2)

                // Execute
                _ <- transactionRepo.save(transaction1.id, transaction1)
                _ <- transactionRepo.save(transaction2.id, transaction2)
                _ <- stateRepo.save(processingState1.transactionId, processingState1)
                _ <- stateRepo.save(processingState2.transactionId, processingState2)
                results <- transactionRepo.find(TransactionQuery(
                    amountMin = Some(BigDecimal("1000.00")),
                    amountMax = Some(BigDecimal("1000.00"))
                ))
            yield
            // Assert
            assertTrue(
                results.size == 1,
                results.head.id == transaction2.id,
                results.head.amount == transaction2.amount
            )
        },
        test("should update transaction processing state") {
            for
                // Setup
                _ <- setupDbSchema
                transactionRepo <- ZIO.service[TransactionRepository]
                stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                // Create a source account first (to satisfy foreign key)
                sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                sourceAccount = SourceAccount(
                    id = 1L,
                    accountId = "123456",
                    bankId = "0800",
                    name = "Test Account",
                    currency = "CZK"
                )
                _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                transaction = createSampleTransaction
                processingState = createSampleProcessingState(transaction)

                // Execute - first save
                _ <- transactionRepo.save(transaction.id, transaction)
                _ <- stateRepo.save(processingState.transactionId, processingState)

                // Update the processing state
                updatedState = processingState.withAICategorization(
                    payeeName = Some("Test Payee"),
                    category = Some("Test Category"),
                    memo = Some("Test Memo")
                )

                // Save the updated state and retrieve
                _ <- stateRepo.save(updatedState.transactionId, updatedState)
                retrievedState <- stateRepo.load(transaction.id)
            yield
            // Assert
            assertTrue(
                retrievedState.isDefined,
                retrievedState.get.status == TransactionStatus.Categorized,
                retrievedState.get.suggestedPayeeName.contains("Test Payee"),
                retrievedState.get.suggestedCategory.contains("Test Category")
            )
        }
    ).provideSomeShared[Scope](
        repositoryLayer
    ) @@ sequential
end PostgreSQLTransactionRepositorySpec
