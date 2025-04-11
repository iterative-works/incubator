package works.iterative.incubator.fio.integration

import zio.*
import zio.test.*
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.security.*
import java.time.Instant

/** Integration tests for the Fio security infrastructure.
  *
  * These tests focus on the token management and security features, testing encryption, caching,
  * and audit logging with a complete integration of all security components.
  */
object FioSecurityIntegrationSpec extends ZIOSpecDefault:
    // Security configuration for tests
    private val securityConfig = FioSecurityConfig(
        encryptionKey = "test-integration-key-32-chars-long",
        cacheExpirationMinutes = 1 // Short expiration for testing
    )

    // Define test account and tokens
    private val testAccount1 = FioAccount(
        id = 1L,
        token = "encrypted-token-placeholder-1", // Will be replaced
        sourceAccountId = 1001L,
        lastFetchedId = None,
        lastSyncTime = None
    )

    private val testAccount2 = FioAccount(
        id = 2L,
        token = "encrypted-token-placeholder-2", // Will be replaced
        sourceAccountId = 1002L,
        lastFetchedId = None,
        lastSyncTime = None
    )

    private val testToken1 = "abcdef1234567890abcdef1234567890abcdef1234567890"
    private val testToken2 = "zyxwvu0987654321zyxwvu0987654321zyxwvu0987654321"

    // In-memory repository for testing
    private val testRepositoryLayer = ZLayer.succeed {
        new FioAccountRepository:
            private val accounts = collection.mutable.Map[Long, FioAccount]()

            // Initialize with test accounts
            {
                val encrypted1 = encrypt(testToken1, securityConfig.encryptionKey)
                val encrypted2 = encrypt(testToken2, securityConfig.encryptionKey)

                accounts(1L) = testAccount1.copy(token = encrypted1)
                accounts(2L) = testAccount2.copy(token = encrypted2)
            }

            override def getAll(): Task[List[FioAccount]] =
                ZIO.succeed(accounts.values.toList)

            override def getById(id: Long): Task[Option[FioAccount]] =
                ZIO.succeed(accounts.get(id))

            override def getBySourceAccountId(sourceAccountId: Long): Task[Option[FioAccount]] =
                ZIO.succeed(accounts.values.find(_.sourceAccountId == sourceAccountId))

            override def create(command: CreateFioAccount): Task[Long] =
                ZIO.succeed {
                    val id = accounts.size + 1L
                    val account = FioAccount(
                        id = id,
                        sourceAccountId = command.sourceAccountId,
                        token = command.token,
                        lastFetchedId = None,
                        lastSyncTime = None
                    )
                    accounts(id) = account
                    id
                }

            // Override repository methods
            // FioAccountRepository doesn't have a save method, only create/update

            override def update(account: FioAccount): Task[Unit] =
                ZIO.succeed {
                    accounts(account.id) = account
                }

            override def delete(id: Long): Task[Unit] =
                ZIO.succeed {
                    val _ = accounts.remove(id)
                    ()
                }

            override def updateLastFetched(id: Long, lastId: Long, syncTime: Instant): Task[Unit] =
                ZIO.succeed {
                    accounts.get(id).foreach { account =>
                        accounts(id) = account.copy(
                            lastFetchedId = Some(lastId),
                            lastSyncTime = Some(syncTime)
                        )
                    }
                }
    }

    // Set up audit service for testing
    private val auditServiceLayer = ZLayer.succeed {
        // In-memory implementation for testing
        new FioTokenAuditService:
            private val events = Unsafe.unsafely {
                Ref.unsafe.make(List.empty[TokenAuditEvent])
            }

            override def logEvent(event: TokenAuditEvent): Task[Unit] =
                events.update(event :: _)

            override def getRecentEvents(limit: Int): Task[List[TokenAuditEvent]] =
                events.get.map(_.take(limit))

            override def getEventsForAccount(
                accountId: Long,
                limit: Int
            ): Task[List[TokenAuditEvent]] =
                events.get.map(_.filter(_.accountId == accountId).take(limit))
    }

    // Token manager layer for testing
    private val tokenManagerLayer = ZLayer {
        for
            repo <- ZIO.service[FioAccountRepository]
            audit <- ZIO.service[FioTokenAuditService]
        yield new FioTokenManagerLive(repo, securityConfig, audit)
    }

    // Helper method to encrypt tokens for testing
    private def encrypt(value: String, key: String): String =
        try
            // Use the first 16 bytes of the key as IV for simplicity
            val keyBytes = key.getBytes("UTF-8").take(32)
            val iv = keyBytes.take(16)

            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val ivSpec = new javax.crypto.spec.IvParameterSpec(iv)

            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(value.getBytes("UTF-8"))
            java.util.Base64.getEncoder.encodeToString(encrypted)
        catch
            case e: Exception =>
                throw new RuntimeException("Error encrypting token", e)

    // Helper method to decrypt tokens for testing
    private def decrypt(encryptedValue: String, key: String): String =
        try
            // Use the first 16 bytes of the key as IV for simplicity
            val keyBytes = key.getBytes("UTF-8").take(32)
            val iv = keyBytes.take(16)

            val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val ivSpec = new javax.crypto.spec.IvParameterSpec(iv)

            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val original = cipher.doFinal(java.util.Base64.getDecoder.decode(encryptedValue))
            new String(original, "UTF-8")
        catch
            case e: Exception =>
                throw new RuntimeException("Error decrypting token", e)

    def spec = suite("Fio Security Integration Tests")(
        test("Token manager should decrypt and return token") {
            for
                manager <- ZIO.service[FioTokenManager]
                tokenOpt <- manager.getToken(1L)
            yield assertTrue(tokenOpt.contains(testToken1))
        },
        test("Token manager should retrieve token by source account ID") {
            for
                manager <- ZIO.service[FioTokenManager]
                tokenOpt <- manager.getTokenBySourceAccountId(1002L)
            yield assertTrue(tokenOpt.contains(testToken2))
        },
        test("Token manager should cache tokens for repeated access") {
            for
                manager <- ZIO.service[FioTokenManager]
                auditService <- ZIO.service[FioTokenAuditService]

                // First access should hit the repository
                _ <- manager.getToken(1L)

                // Second access should hit the cache
                _ <- manager.getToken(1L)

                // Check the audit log
                events <- auditService.getEventsForAccount(1L, 10)
                accessEvents = events.filter(_.eventType == TokenAuditEventType.Access)
                cacheHitEvents = events.filter(_.eventType == TokenAuditEventType.CacheHit)
            yield assertTrue(
                accessEvents.size == 1 &&
                    cacheHitEvents.size == 1
            )
        },
        test("Token manager should update token") {
            for
                manager <- ZIO.service[FioTokenManager]
                repository <- ZIO.service[FioAccountRepository]

                // Store new token
                newToken = "new-token-value-with-sufficient-length-for-security"
                _ <- manager.storeToken(1L, newToken)

                // Verify directly from repository
                account <- repository.getById(1L).map(_.get)
                decryptedToken = decrypt(account.token, securityConfig.encryptionKey)
            yield assertTrue(decryptedToken == newToken)
        },
        test("Token manager should invalidate cache when requested") {
            for
                manager <- ZIO.service[FioTokenManager]
                auditService <- ZIO.service[FioTokenAuditService]

                // First access to populate cache
                _ <- manager.getToken(2L)

                // Invalidate the cache
                _ <- manager.invalidateCache(2L)

                // Next access should hit repository again
                _ <- manager.getToken(2L)

                // Check audit log
                events <- auditService.getEventsForAccount(2L, 10)
                accessEvents = events.filter(_.eventType == TokenAuditEventType.Access)
                invalidateEvents = events.filter(_.eventType == TokenAuditEventType.Invalidate)
            yield assertTrue(
                accessEvents.size == 2 &&
                    invalidateEvents.size == 1
            )
        },
        test("Token manager cache should expire after configured time") {
            for
                manager <- ZIO.service[FioTokenManager]
                auditService <- ZIO.service[FioTokenAuditService]

                // First access to populate cache
                token1 <- manager.getToken(1L)

                // Clear existing audit events for this test to simplify assertions
                _ <- auditService.logEvent(TokenAuditEvent(
                    timestamp = Instant.now(),
                    eventType = TokenAuditEventType.Invalidate,
                    accountId = 1L,
                    message = "Clearing log for cache expiration test"
                ))

                // Access again immediately - should use cache
                _ <- manager.getToken(1L)

                // Check audit log for cache hit
                eventsAfterCacheHit <- auditService.getEventsForAccount(1L, 10)
                cacheHits = eventsAfterCacheHit.count(_.eventType == TokenAuditEventType.CacheHit)

                // Advance time beyond the cache expiration
                _ <- TestClock.adjust((securityConfig.cacheExpirationMinutes + 1).minutes)

                // Access again after cache expiration - should hit repository
                token2 <- manager.getToken(1L)

                // Check audit log for repository access after expiration
                eventsAfterExpiration <- auditService.getEventsForAccount(1L, 10)
                accessEvents = eventsAfterExpiration.count(e =>
                    e.eventType == TokenAuditEventType.Access &&
                        e.message == "Token retrieved from repository" &&
                        // Filter by recent events (after our clear event)
                        e.timestamp.isAfter(eventsAfterCacheHit.head.timestamp)
                )
            yield assertTrue(
                // Both token accesses should return the same value
                token1.contains(testToken1) && token2.contains(testToken1) &&
                    // Should have at least one cache hit before expiration
                    cacheHits >= 1 &&
                    // Should have a repository access after expiration
                    accessEvents >= 1
            )
        }
    ).provideLayer(
        ZLayer.make[FioTokenManager & FioTokenAuditService & FioAccountRepository](
            testRepositoryLayer,
            auditServiceLayer,
            tokenManagerLayer
        )
    )
end FioSecurityIntegrationSpec
