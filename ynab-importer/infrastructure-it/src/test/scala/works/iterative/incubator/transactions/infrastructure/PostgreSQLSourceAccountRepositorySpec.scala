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

    // Schema setup for the test database
    @nowarn("msg=unused value of type Int")
    val setupDbSchema = ZIO.serviceWithZIO[Transactor] { xa =>
        xa.transact:
            // Reset
            sql"""
            DROP TABLE IF EXISTS source_account CASCADE;
            """.update.run()

            sql"""-- Create the source_account table
            CREATE TABLE source_account (
                id BIGSERIAL PRIMARY KEY,
                account_id VARCHAR(255) NOT NULL,
                bank_id VARCHAR(255) NOT NULL,
                UNIQUE(account_id, bank_id)
            );""".update.run()
        .orDie
    }

    // Create sample source account for testing
    def createSampleSourceAccount(id: Long = 1): SourceAccount =
        SourceAccount(
            id = id,
            accountId = "123456789",
            bankId = "0800"
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
