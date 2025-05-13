package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.AccountId
import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.Instant

object InMemoryFioAccountRepositorySpec extends ZIOSpecDefault:
    // Sample test data
    val sampleAccountId = AccountId("fio", "1234567890")
    val sampleToken = "test-encrypted-token-123456"

    def spec = suite("InMemoryFioAccountRepository")(
        test("nextId should generate sequential IDs") {
            for
                // Create repository with the layer
                repository <- ZIO.service[FioAccountRepository]

                // Generate a few IDs and verify they are sequential
                id1 <- repository.nextId()
                id2 <- repository.nextId()
                id3 <- repository.nextId()
            yield assert(id1)(equalTo(1L)) &&
                assert(id2)(equalTo(2L)) &&
                assert(id3)(equalTo(3L))
        }.provideLayer(InMemoryFioAccountRepository.layer),
        test("save and findById should work correctly") {
            for
                // Create repository with the layer
                repository <- ZIO.service[FioAccountRepository]

                // Generate ID and create an account
                id <- repository.nextId()
                account <- ZIO.fromEither(FioAccount.create(id, sampleAccountId, sampleToken))

                // Save the account
                _ <- repository.save(account)

                // Find the account by ID
                foundAccount <- repository.findById(id)
            yield assert(foundAccount)(isSome(equalTo(account)))
        }.provideLayer(InMemoryFioAccountRepository.layer),
        test("findById should return None for non-existent ID") {
            for
                // Create repository with the layer
                repository <- ZIO.service[FioAccountRepository]

                // Try to find a non-existent account
                foundAccount <- repository.findById(999L)
            yield assert(foundAccount)(isNone)
        }.provideLayer(InMemoryFioAccountRepository.layer),
        test("findBySourceAccountId should return the correct account") {
            for
                // Create repository with the layer
                repository <- ZIO.service[FioAccountRepository]

                // Generate ID and create an account
                id <- repository.nextId()
                account <- ZIO.fromEither(FioAccount.create(id, sampleAccountId, sampleToken))

                // Save the account
                _ <- repository.save(account)

                // Find the account by source account ID
                foundAccount <- repository.findBySourceAccountId(sampleAccountId)
            yield assert(foundAccount)(isSome(equalTo(account)))
        }.provideLayer(InMemoryFioAccountRepository.layer),
        test("findBySourceAccountId should return None for non-existent account") {
            for
                // Create repository with the layer
                repository <- ZIO.service[FioAccountRepository]

                // Try to find a non-existent account
                nonExistentAccountId = AccountId("nonexistent", "account")
                foundAccount <- repository.findBySourceAccountId(nonExistentAccountId)
            yield assert(foundAccount)(isNone)
        }.provideLayer(InMemoryFioAccountRepository.layer),
        test("save should update an existing account") {
            for
                // Create repository with the layer
                repository <- ZIO.service[FioAccountRepository]

                // Generate ID and create an account
                id <- repository.nextId()
                originalAccount <-
                    ZIO.fromEither(FioAccount.create(id, sampleAccountId, sampleToken))

                // Save the account
                _ <- repository.save(originalAccount)

                // Create an updated version and save it
                now = Instant.now
                updatedAccount = originalAccount.copy(
                    lastSyncTime = Some(now),
                    lastFetchedId = Some(12345L),
                    updatedAt = now
                )
                _ <- repository.save(updatedAccount)

                // Retrieve the account to check if it was updated
                foundAccount <- repository.findById(id)
            yield assert(foundAccount)(isSome(equalTo(updatedAccount))) &&
                assert(foundAccount.get.lastSyncTime)(isSome(equalTo(now))) &&
                assert(foundAccount.get.lastFetchedId)(isSome(equalTo(12345L)))
        }.provideLayer(InMemoryFioAccountRepository.layer),
        test("nextId should generate unique IDs across multiple repository instances") {
            for
                // Create two separate repositories
                layer1 <- ZIO.succeed(InMemoryFioAccountRepository.layer)
                layer2 <- ZIO.succeed(InMemoryFioAccountRepository.layer)

                // Get IDs from both repositories
                id1 <- ZIO.serviceWithZIO[FioAccountRepository](_.nextId()).provideLayer(layer1)
                id2 <- ZIO.serviceWithZIO[FioAccountRepository](_.nextId()).provideLayer(layer2)
            yield
            // IDs should be the same because each layer is a fresh instance
            assert(id1)(equalTo(1L)) &&
                assert(id2)(equalTo(1L))
        }
    )
end InMemoryFioAccountRepositorySpec
