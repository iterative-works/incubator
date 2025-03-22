package works.iterative.incubator.transactions
package infrastructure

import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.augustnagro.magnum.magzio.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import service.SourceAccountRepository
import scala.annotation.nowarn
import zio.test.TestAspect.sequential
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

object PostgreSQLSourceAccountRepositorySpec extends ZIOSpecDefault:
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
            yield PostgreSQLSourceAccountRepository(transactor)
        }

    // Create PostgreSQLDataSource from DataSource for Flyway
    val postgreSQLDataSourceLayer = dataSourceLayer >>> ZLayer {
        ZIO.service[DataSource].map(ds => PostgreSQLDataSource(ds))
    }
    
    // For tests, we'll manually create the schema directly
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

    // Create sample source account for testing
    def createSampleSourceAccount(id: Long = 1): SourceAccount =
        SourceAccount(
            id = id,
            accountId = "123456789",
            bankId = "0800",
            name = "Test Account",
            currency = "CZK",
            ynabAccountId = None,
            active = true,
            lastSyncTime = None
        )

    def spec = suite("PostgreSQLSourceAccountRepository")(
        test("should save and retrieve a source account") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[SourceAccountRepository]
                sourceAccount = createSampleSourceAccount()

                // Execute
                _ <- repository.save(sourceAccount.id, sourceAccount)
                retrieved <- repository.load(sourceAccount.id)
            yield
            // Assert
            assertTrue(
                retrieved.isDefined,
                retrieved.get.id == sourceAccount.id,
                retrieved.get.accountId == sourceAccount.accountId,
                retrieved.get.bankId == sourceAccount.bankId
            )
        },
        test("should find source accounts by query") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[SourceAccountRepository]
                sourceAccount1 = createSampleSourceAccount(1)
                sourceAccount2 = createSampleSourceAccount(2).copy(accountId = "987654321")

                // Execute
                _ <- repository.save(sourceAccount1.id, sourceAccount1)
                _ <- repository.save(sourceAccount2.id, sourceAccount2)
                results <- repository.find(SourceAccountQuery())
            yield
            // Assert
            assertTrue(
                results.size == 2,
                results.exists(a => a.id == sourceAccount1.id),
                results.exists(a => a.id == sourceAccount2.id)
            )
        },
        test("should find source accounts by filter") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[SourceAccountRepository]
                sourceAccount1 = createSampleSourceAccount(1)
                sourceAccount2 = createSampleSourceAccount(2).copy(accountId = "987654321")

                // Execute
                _ <- repository.save(sourceAccount1.id, sourceAccount1)
                _ <- repository.save(sourceAccount2.id, sourceAccount2)
                results <- repository.find(SourceAccountQuery(accountId = Some("987654321")))
            yield
            // Assert
            assertTrue(
                results.size == 1,
                results.head.id == sourceAccount2.id,
                results.head.accountId == "987654321"
            )
        },
        test("should update an existing source account") {
            for
                // Setup
                _ <- setupDbSchema
                repository <- ZIO.service[SourceAccountRepository]
                sourceAccount = createSampleSourceAccount()

                // Execute - first save
                _ <- repository.save(sourceAccount.id, sourceAccount)

                // Update the source account
                updatedSourceAccount = sourceAccount.copy(bankId = "0100")

                // Save the updated source account and retrieve
                _ <- repository.save(sourceAccount.id, updatedSourceAccount)
                retrieved <- repository.load(sourceAccount.id)
            yield
            // Assert
            assertTrue(
                retrieved.isDefined,
                retrieved.get.bankId == "0100"
            )
        }
    ).provideSomeShared[Scope](
        repositoryLayer
    ) @@ sequential
end PostgreSQLSourceAccountRepositorySpec
