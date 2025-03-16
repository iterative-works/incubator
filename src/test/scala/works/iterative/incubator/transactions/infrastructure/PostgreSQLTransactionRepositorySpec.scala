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
import service.TransactionRepository
import scala.annotation.nowarn
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

    // Setup the repository layer for testing
    val repositoryLayer =
        transactorLayer >+> ZLayer {
            for
                transactor <- ZIO.service[Transactor]
            yield PostgreSQLTransactionRepository(transactor)
        }

    // Schema setup for the test database
    @nowarn("msg=unused value of type Int")
    val setupDbSchema = ZIO.serviceWithZIO[Transactor] { xa =>
        xa.transact:
            // Reset
            sql"""
            DROP TABLE IF EXISTS transaction CASCADE;
            DROP TYPE IF EXISTS transaction_status CASCADE;
            DROP INDEX IF EXISTS idx_transaction_status;
            DROP INDEX IF EXISTS idx_transaction_date;
            DROP INDEX IF EXISTS idx_transaction_imported_at;
            DROP INDEX IF EXISTS idx_transaction_ynab_transaction_id;
            """.update.run()

            sql"""
            -- First create an enum type for TransactionStatus
            CREATE TYPE transaction_status AS ENUM ('Imported', 'Categorized', 'Submitted');
            """.update.run()

            sql"""-- Create the transactions table
            CREATE TABLE transaction (
                -- Composite primary key from TransactionId (using concatenated form for simplicity)
                source_account VARCHAR(255) NOT NULL,
                source_bank VARCHAR(255) NOT NULL,
                transaction_id VARCHAR(255) NOT NULL,

                -- Source data from FIO
                date DATE NOT NULL,
                amount DECIMAL(19, 4) NOT NULL,
                currency VARCHAR(3) NOT NULL,
                counter_account VARCHAR(255),
                counter_bank_code VARCHAR(255),
                counter_bank_name VARCHAR(255),
                variable_symbol VARCHAR(255),
                constant_symbol VARCHAR(255),
                specific_symbol VARCHAR(255),
                user_identification TEXT,
                message TEXT,
                transaction_type VARCHAR(255) NOT NULL,
                comment TEXT,

                -- Processing state
                -- TODO: use enum instead of varchar after next Magnum release (>2.0.0-M1 or >1.3.1)
                -- There is a support for enums in the next release, it's already in master
                -- https://github.com/AugustNagro/magnum/pull/100
                status VARCHAR(255) NOT NULL,

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

                -- Metadata
                imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
                processed_at TIMESTAMP WITH TIME ZONE,
                submitted_at TIMESTAMP WITH TIME ZONE,

                -- Define the primary key
                PRIMARY KEY (source_account, source_bank, transaction_id)
            );""".update.run()

            sql"""-- Create indexes for common query patterns
            CREATE INDEX idx_transaction_status ON transaction (status);""".update.run()
            sql"""CREATE INDEX idx_transaction_date ON transaction (date);""".update.run()
            sql"""CREATE INDEX idx_transaction_imported_at ON transaction (imported_at);""".update.run()
            sql"""CREATE INDEX idx_transaction_ynab_transaction_id ON transaction (ynab_transaction_id) WHERE ynab_transaction_id IS NOT NULL;""".update.run()
        .orDie
    }

    // Create sample transaction for testing
    def createSampleTransaction: Transaction =
        Transaction(
            id = TransactionId(sourceAccount = "123456", sourceBank = "0800", id = "TX123"),
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
            status = TransactionStatus.Imported,
            suggestedPayeeName = None,
            suggestedCategory = None,
            suggestedMemo = None,
            overridePayeeName = None,
            overrideCategory = None,
            overrideMemo = None,
            ynabTransactionId = None,
            ynabAccountId = None,
            importedAt = Instant.now(),
            processedAt = None,
            submittedAt = None
        )

    def spec = suite("PostgreSQLTransactionRepository")(
        test("should save and retrieve a transaction") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[TransactionRepository]
                transaction = createSampleTransaction

                // Execute
                _ <- repository.save(transaction.id, transaction)
                retrieved <- repository.load(transaction.id)
            yield
            // Assert
            assertTrue(
                retrieved.isDefined,
                retrieved.get.id == transaction.id,
                retrieved.get.amount == transaction.amount,
                retrieved.get.date == transaction.date
            )
        },
        test("should find transactions by query") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[TransactionRepository]
                transaction1 = createSampleTransaction
                transaction2 = createSampleTransaction.copy(
                    id = TransactionId("123456", "0800", "TX124"),
                    amount = BigDecimal("1000.00")
                )

                // Execute
                _ <- repository.save(transaction1.id, transaction1)
                _ <- repository.save(transaction2.id, transaction2)
                results <- repository.find(TransactionQuery())
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
                repository <- ZIO.service[TransactionRepository]
                transaction1 = createSampleTransaction
                transaction2 = createSampleTransaction.copy(
                    id = TransactionId("123456", "0800", "TX124"),
                    amount = BigDecimal("1000.00")
                )

                // Execute
                _ <- repository.save(transaction1.id, transaction1)
                _ <- repository.save(transaction2.id, transaction2)
                results <- repository.find(TransactionQuery(amount = Some(1000.0)))
            yield
            // Assert
            assertTrue(
                results.size == 1,
                results.head.id == transaction2.id,
                results.head.amount == transaction2.amount
            )
        },
        test("should update an existing transaction") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[TransactionRepository]
                transaction = createSampleTransaction

                // Execute - first save
                _ <- repository.save(transaction.id, transaction)

                // Update the transaction
                updatedTransaction = transaction.copy(
                    status = TransactionStatus.Categorized,
                    suggestedPayeeName = Some("Test Payee"),
                    suggestedCategory = Some("Test Category")
                )

                // Save the updated transaction and retrieve
                _ <- repository.save(transaction.id, updatedTransaction)
                retrieved <- repository.load(transaction.id)
            yield
            // Assert
            assertTrue(
                retrieved.isDefined,
                retrieved.get.status == TransactionStatus.Categorized,
                retrieved.get.suggestedPayeeName.contains("Test Payee"),
                retrieved.get.suggestedCategory.contains("Test Category")
            )
        }
    ).provideSomeShared[Scope](
        repositoryLayer
    ) @@ sequential
end PostgreSQLTransactionRepositorySpec
