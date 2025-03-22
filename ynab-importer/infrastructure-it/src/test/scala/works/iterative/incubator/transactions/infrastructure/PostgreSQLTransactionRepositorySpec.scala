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
import service.{TransactionRepository, TransactionProcessingStateRepository, SourceAccountRepository}
import scala.annotation.nowarn
import zio.test.TestAspect.sequential
import org.flywaydb.core.Flyway

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

    // Schema setup for the test database - create tables directly instead of using Flyway
    @nowarn("msg=unused value of type Int")
    val setupDbSchema = ZIO.serviceWithZIO[Transactor] { xa =>
        xa.transact:
            // Reset
            sql"""
            DROP TABLE IF EXISTS transaction_processing_state CASCADE;
            DROP TABLE IF EXISTS transaction CASCADE;
            DROP TABLE IF EXISTS source_account CASCADE;
            DROP TYPE IF EXISTS transaction_status CASCADE;
            """.update.run()

            sql"""
            -- First create an enum type for TransactionStatus
            CREATE TYPE transaction_status AS ENUM ('Imported', 'Categorized', 'Submitted');
            """.update.run()

            sql"""-- Create the source_account table
            CREATE TABLE source_account (
                id BIGINT PRIMARY KEY,
                account_id VARCHAR(255) NOT NULL,
                bank_id VARCHAR(255) NOT NULL,
                name VARCHAR(255) NOT NULL DEFAULT 'Unnamed Account',
                currency VARCHAR(3) NOT NULL DEFAULT 'CZK',
                ynab_account_id VARCHAR(255),
                active BOOLEAN NOT NULL DEFAULT true,
                last_sync_time TIMESTAMP WITH TIME ZONE,
                UNIQUE(account_id, bank_id)
            );""".update.run()

            sql"""-- Create transaction table (immutable events)
            CREATE TABLE transaction (
                -- Primary key and identifiers
                source_account_id BIGINT NOT NULL,
                transaction_id VARCHAR(255) NOT NULL,
                
                -- Transaction details
                date DATE NOT NULL,
                amount DECIMAL(19, 4) NOT NULL,
                currency VARCHAR(10) NOT NULL,
                
                -- Counterparty information
                counter_account VARCHAR(255),
                counter_bank_code VARCHAR(255),
                counter_bank_name VARCHAR(255),
                
                -- Additional transaction details
                variable_symbol VARCHAR(255),
                constant_symbol VARCHAR(255),
                specific_symbol VARCHAR(255),
                user_identification VARCHAR(255),
                message TEXT,
                transaction_type VARCHAR(255) NOT NULL,
                comment TEXT,
                
                -- Metadata
                imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
                
                -- Composite primary key
                PRIMARY KEY (source_account_id, transaction_id),
                
                -- Foreign key to source_account
                CONSTRAINT fk_transaction_source_account
                    FOREIGN KEY (source_account_id)
                    REFERENCES source_account(id)
            );""".update.run()

            sql"""-- Create transaction_processing_state table
            CREATE TABLE transaction_processing_state (
                -- Reference to transaction
                source_account_id BIGINT NOT NULL,
                transaction_id VARCHAR(255) NOT NULL,
                
                -- Processing state
                status VARCHAR(20) NOT NULL,
                
                -- AI computed/processed fields for YNAB
                suggested_payee_name VARCHAR(255),
                suggested_category VARCHAR(255),
                suggested_memo TEXT,
                
                -- User overrides
                override_payee_name VARCHAR(255),
                override_category VARCHAR(255),
                override_memo TEXT,
                
                -- YNAB integration fields
                ynab_transaction_id VARCHAR(255),
                ynab_account_id VARCHAR(255),
                
                -- Processing timestamps
                processed_at TIMESTAMP WITH TIME ZONE,
                submitted_at TIMESTAMP WITH TIME ZONE,
                
                -- Primary key (same as transaction)
                PRIMARY KEY (source_account_id, transaction_id),
                
                -- Foreign key to transaction
                CONSTRAINT fk_processing_state_transaction
                    FOREIGN KEY (source_account_id, transaction_id)
                    REFERENCES transaction(source_account_id, transaction_id)
            );""".update.run()

            sql"""-- Create indexes for performance
            CREATE INDEX idx_transaction_date ON transaction(date);
            CREATE INDEX idx_transaction_source_account ON transaction(source_account_id);
            CREATE INDEX idx_transaction_imported_at ON transaction(imported_at);
            CREATE INDEX idx_processing_state_status ON transaction_processing_state(status);""".update.run()
        .orDie
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
                results <- transactionRepo.find(TransactionQuery(amountMin = Some(BigDecimal("1000.00")), amountMax = Some(BigDecimal("1000.00"))))
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
