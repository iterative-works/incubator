package works.iterative.incubator.fio.infrastructure.security

import zio.*
import zio.test.*
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.persistence.InMemoryFioAccountRepository
import java.time.Instant

/** Tests for the FioTokenManager
  *
  * This test suite verifies the functionality of the FioTokenManager.
  */
object FioTokenManagerSpec extends ZIOSpecDefault:

    // Test encryption key
    private val testKey = "test-encryption-key-12345678901234567890"

    // Test account data
    private val testAccount = FioAccount(
        id = 1L,
        sourceAccountId = 100L,
        token = "original-token", // Will be replaced with encrypted version
        lastSyncTime = Some(Instant.now),
        lastFetchedId = Some(12345L)
    )

    // Environment layers for testing
    private val testLayers: ZLayer[
        Any,
        Nothing,
        InMemoryFioAccountRepository & FioTokenAuditService & FioTokenManager
    ] =
        // Start with a fresh repository
        InMemoryFioAccountRepository.testLayer >+>
            // Add audit service
            FioTokenAuditServiceLive.layer >+>
            // Add token manager
            FioTokenManagerLive.layerWithConfig(FioSecurityConfig(testKey))

    // Helper to set up account data
    private def setupTestAccount =
        for
            repo <- ZIO.service[InMemoryFioAccountRepository]
            _ <- repo.reset()
            _ <- repo.create(CreateFioAccount(testAccount.sourceAccountId, testAccount.token))
        yield ()

    override def spec =
        suite("FioTokenManager")(
            test("should encrypt and store tokens") {
                for
                    // Set up test environment
                    _ <- setupTestAccount
                    tokenManager <- ZIO.service[FioTokenManager]

                    // Get account ID for testing
                    repo <- ZIO.service[InMemoryFioAccountRepository]
                    accountOpt <- repo.getBySourceAccountId(testAccount.sourceAccountId)
                    accountId = accountOpt.get.id

                    // Store a token
                    _ <- tokenManager.storeToken(accountId, "test-token-value")

                    // Read account directly to check encryption
                    accountAfterStore <- repo.getById(accountId)
                    encryptedToken = accountAfterStore.get.token

                    // Verify the token is encrypted (not stored as plaintext)
                    isEncrypted = encryptedToken != "test-token-value"

                    // Retrieve token through manager
                    retrievedTokenOpt <- tokenManager.getToken(accountId)
                yield assertTrue(
                    isEncrypted,
                    retrievedTokenOpt.contains("test-token-value")
                )
            },
            test("should retrieve tokens by source account ID") {
                for
                    // Set up test environment
                    _ <- setupTestAccount
                    tokenManager <- ZIO.service[FioTokenManager]

                    // Get account ID for testing
                    repo <- ZIO.service[InMemoryFioAccountRepository]
                    accountOpt <- repo.getBySourceAccountId(testAccount.sourceAccountId)
                    accountId = accountOpt.get.id

                    // Store a token
                    _ <- tokenManager.storeToken(accountId, "source-account-test")

                    // Retrieve token by source account ID
                    retrievedTokenOpt <-
                        tokenManager.getTokenBySourceAccountId(testAccount.sourceAccountId)
                yield assertTrue(retrievedTokenOpt.contains("source-account-test"))
            },
            test("should cache tokens for repeated access") {
                for
                    // Set up test environment
                    _ <- setupTestAccount
                    tokenManager <- ZIO.service[FioTokenManager]
                    repo <- ZIO.service[InMemoryFioAccountRepository]
                    accountOpt <- repo.getBySourceAccountId(testAccount.sourceAccountId)
                    accountId = accountOpt.get.id

                    // Store a token
                    _ <- tokenManager.storeToken(accountId, "cached-token-test")

                    // First access (fills cache)
                    _ <- tokenManager.getToken(accountId)

                    // Access again (should use cache)
                    retrievedTokenOpt <- tokenManager.getToken(accountId)

                    // Invalidate cache
                    _ <- tokenManager.invalidateCache(accountId)

                    // Access should retrieve from repo again
                    retrievedAfterInvalidate <- tokenManager.getToken(accountId)
                yield assertTrue(
                    retrievedTokenOpt.contains("cached-token-test"),
                    retrievedAfterInvalidate.contains("cached-token-test")
                )
            }
        ).provide(testLayers)
end FioTokenManagerSpec
