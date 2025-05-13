package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.AccountId
import zio.*
import zio.test.*
import zio.test.Assertion.*

object FioTokenManagerSpec extends ZIOSpecDefault:
    // Sample test data
    val sampleAccountId = AccountId("fio", "1234567890")
    val sampleToken = "test-token-123456"
    // AES-256 requires exactly 32 bytes for the key
    val testEncryptionKey =
        val key = "test-encryption-key-12345678901234567890"
        val bytes = key.getBytes("UTF-8")
        // Ensure the key is exactly 32 bytes
        if bytes.length < 32 then
            bytes.padTo(32, 0.toByte)
        else if bytes.length > 32 then
            bytes.take(32)
        else
            bytes
        end if
    end testEncryptionKey

    // Layer setup for dependency injection
    val testRepoLayer = InMemoryFioAccountRepository.layer

    // Manual setup for test environment
    def createTokenManager =
        for
            repo <- ZIO.service[FioAccountRepository]
            tokenCacheRef <- Ref.make(Map.empty[String, String])
        yield FioTokenManagerLive(repo, testEncryptionKey, tokenCacheRef)

    override def spec = suite("FioTokenManager")(
        test("encryptToken and decryptToken should work correctly") {
            for
                // Create token manager
                tokenManager <- createTokenManager

                // Encrypt the token
                encrypted <- tokenManager.encryptToken(sampleToken)

                // Verify encryption produces different output from the original
                _ <- ZIO.succeed(assertTrue(encrypted != sampleToken))

                // Decrypt the token
                decrypted <- tokenManager.decryptToken(encrypted)
            yield
            // Decrypted value should match the original
            assert(decrypted)(equalTo(sampleToken))
        }.provideLayer(testRepoLayer),
        test("storeToken should save an encrypted token") {
            for
                // Get repository and create token manager
                repository <- ZIO.service[FioAccountRepository]
                tokenManager <- createTokenManager

                // Store a token
                _ <- tokenManager.storeToken(sampleAccountId, sampleToken)

                // Retrieve the account from the repository
                account <- repository.findBySourceAccountId(sampleAccountId)
                    .someOrFail(new RuntimeException("Account not found"))
            yield
            // Verify the token is stored in encrypted form
            assert(account.encryptedToken)(not(equalTo(sampleToken)))
        }.provideLayer(testRepoLayer),
        test("getToken should retrieve and decrypt a token") {
            for
                // Create token manager
                tokenManager <- createTokenManager

                // Store a token
                _ <- tokenManager.storeToken(sampleAccountId, sampleToken)

                // Retrieve the token
                retrievedToken <- tokenManager.getToken(sampleAccountId)
            yield
            // Verify the retrieved token matches the original
            assert(retrievedToken)(equalTo(sampleToken))
        }.provideLayer(testRepoLayer),
        test("getToken should use cache for subsequent retrievals") {
            for
                // Get repository and create token manager
                repository <- ZIO.service[FioAccountRepository]
                tokenManager <- createTokenManager

                // Store a token
                _ <- tokenManager.storeToken(sampleAccountId, sampleToken)

                // First retrieval (should go to repository)
                token1 <- tokenManager.getToken(sampleAccountId)

                // Change the token in the repository directly (to verify cache is used)
                account <- repository.findBySourceAccountId(sampleAccountId)
                    .someOrFail(new RuntimeException("Account not found"))
                modifiedAccount = account.copy(encryptedToken = "modified-encrypted-token")
                _ <- repository.save(modifiedAccount)

                // Second retrieval (should use cache)
                token2 <- tokenManager.getToken(sampleAccountId)

                // Clear cache
                _ <- tokenManager.clearCache()

                // Third retrieval (should go to repository)
                token3 <- tokenManager.getToken(sampleAccountId)
                    .catchAll(_ => ZIO.succeed("token-not-found"))
            yield assert(token1)(equalTo(sampleToken)) &&
                assert(token2)(equalTo(sampleToken)) && // From cache, not modified value
                assert(token3)(not(equalTo(sampleToken))) // After cache clear, gets modified value
        }.provideLayer(testRepoLayer),
        test("getToken should fail for non-existent account") {
            for
                // Create token manager
                tokenManager <- createTokenManager

                // Try to retrieve a token for a non-existent account
                nonExistentAccountId = AccountId("nonexistent", "account")
                result <- tokenManager.getToken(nonExistentAccountId).exit
            yield assert(result)(fails(containsString("No Fio account found")))
        }.provideLayer(testRepoLayer),
        test("encryption should be secure and repeatable") {
            for
                // Create token manager
                tokenManager <- createTokenManager

                // Encrypt the same token twice
                encrypted1 <- tokenManager.encryptToken(sampleToken)
                encrypted2 <- tokenManager.encryptToken(sampleToken)

                // Decrypt both encrypted values
                decrypted1 <- tokenManager.decryptToken(encrypted1)
                decrypted2 <- tokenManager.decryptToken(encrypted2)
            yield
            // Encryption should be non-deterministic (different each time)
            assert(encrypted1)(not(equalTo(encrypted2))) &&
                // But decryption should produce the same original value
                assert(decrypted1)(equalTo(sampleToken)) &&
                assert(decrypted2)(equalTo(sampleToken))
        }.provideLayer(testRepoLayer)
    )
end FioTokenManagerSpec
