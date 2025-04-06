package works.iterative.incubator.transactions
package infrastructure

import zio.*
import zio.test.*
import com.augustnagro.magnum.magzio.*
import works.iterative.incubator.transactions.domain.repository.SourceAccountRepository
import works.iterative.incubator.transactions.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery
import zio.test.TestAspect.sequential
import works.iterative.incubator.transactions.infrastructure.persistence.PostgreSQLSourceAccountRepository

object PostgreSQLSourceAccountRepositorySpec extends ZIOSpecDefault:
    import PostgreSQLLayers.*
    import MigrateAspects.*

    // Setup the repository layer for testing
    val repositoryLayer =
        transactorLayer >>> ZLayer {
            for
                transactor <- ZIO.service[Transactor]
            yield PostgreSQLSourceAccountRepository(transactor)
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

    // Create sample CreateSourceAccount for testing
    def createSampleCreateSourceAccount(): CreateSourceAccount =
        CreateSourceAccount(
            accountId = "123456789",
            bankId = "0800",
            name = "Test Account",
            currency = "CZK",
            ynabAccountId = None,
            active = true
        )

    def spec = {
        suite("PostgreSQLSourceAccountRepository")(
            test("should save and retrieve a source account") {
                for
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
            },
            test("should create a new source account with generated ID") {
                for
                    repository <- ZIO.service[SourceAccountRepository]
                    createAccount = createSampleCreateSourceAccount()

                    // Execute - create new account
                    id <- repository.create(createAccount)

                    // Retrieve the created account
                    retrieved <- repository.load(id)
                yield
                // Assert
                assertTrue(
                    id > 0, // Generated ID should be positive
                    retrieved.isDefined,
                    retrieved.get.id == id,
                    retrieved.get.accountId == createAccount.accountId,
                    retrieved.get.bankId == createAccount.bankId,
                    retrieved.get.name == createAccount.name,
                    retrieved.get.currency == createAccount.currency,
                    retrieved.get.active == createAccount.active
                )
            }
        ) @@ sequential @@ migrate
    }.provideSomeShared[Scope](
        flywayMigrationServiceLayer,
        repositoryLayer
    )
end PostgreSQLSourceAccountRepositorySpec
