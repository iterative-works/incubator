package works.iterative.incubator.fio.infrastructure.security

import zio.*
import works.iterative.incubator.fio.domain.model.FioAccount
import works.iterative.incubator.fio.domain.model.FioAccountRepository
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.time.Instant
import scala.collection.concurrent.TrieMap
import java.util.concurrent.TimeUnit

/** Interface for Fio token management service
  *
  * This service provides secure access to Fio API tokens with encryption, caching, and audit
  * logging capabilities.
  *
  * Classification: Infrastructure Security Service
  */
trait FioTokenManager:
    /** Get a token for a specific account
      *
      * @param accountId
      *   The Fio account ID
      * @return
      *   The decrypted token if found
      */
    def getToken(accountId: Long): Task[Option[String]]

    /** Get a token for a source account
      *
      * @param sourceAccountId
      *   The source account ID
      * @return
      *   The decrypted token if found
      */
    def getTokenBySourceAccountId(sourceAccountId: Long): Task[Option[String]]

    /** Store a token for a account
      *
      * @param accountId
      *   The Fio account ID
      * @param token
      *   The token to store (will be encrypted)
      * @return
      *   Unit on success
      */
    def storeToken(accountId: Long, token: String): Task[Unit]

    /** Invalidate cached token for an account
      *
      * @param accountId
      *   The Fio account ID
      * @return
      *   Unit on success
      */
    def invalidateCache(accountId: Long): Task[Unit]

    /** Clear all cached tokens
      *
      * @return
      *   Unit on success
      */
    def clearCache: Task[Unit]
end FioTokenManager

/** Live implementation of FioTokenManager
  *
  * This implementation provides secure token management with AES encryption, an in-memory cache for
  * frequent lookups, and audit logging.
  *
  * Classification: Infrastructure Security Service Implementation
  */
class FioTokenManagerLive(
    repository: FioAccountRepository,
    securityConfig: FioSecurityConfig,
    auditService: FioTokenAuditService
) extends FioTokenManager:
    import FioTokenManagerLive.*

    // In-memory cache for frequent lookups
    private val tokenCache = TrieMap.empty[Long, TokenCacheEntry]
    private val sourceAccountCache = TrieMap.empty[Long, Long] // Maps sourceAccountId to accountId

    // Cache expiration time in milliseconds
    private val cacheExpirationMs = securityConfig.cacheExpirationMinutes * 60 * 1000L

    /** Get a token for a specific account
      */
    override def getToken(accountId: Long): Task[Option[String]] =
        // First check the cache
        ZIO.succeed(tokenCache.get(accountId)).flatMap {
            case Some(entry) =>
                isExpired(entry).flatMap { expired =>
                    if !expired then
                        // Cache hit and not expired
                        for
                            _ <- auditService.logEvent(TokenAuditEvent(
                                timestamp = Instant.now(),
                                eventType = TokenAuditEventType.CacheHit,
                                accountId = accountId,
                                message = "Token retrieved from cache"
                            ))
                            _ <- ZIO.logDebug(s"Cache hit for token access: accountId=$accountId")
                        yield Some(entry.token)
                    else
                        // Cache expired, fetch from repository
                        fetchTokenFromRepository(accountId)
                }

            case None =>
                // Cache miss, fetch from repository
                fetchTokenFromRepository(accountId)
        }

    // Helper method to reduce duplication
    private def fetchTokenFromRepository(accountId: Long): Task[Option[String]] =
        for
            accountOpt <- repository.getById(accountId)
            token <- ZIO.foreach(accountOpt) { account =>
                // Decrypt the token
                val decryptedToken = decrypt(account.token, securityConfig.encryptionKey)
                // Update cache
                updateCache(account.id, decryptedToken) *>
                    // Update source account cache
                    ZIO.succeed(sourceAccountCache.put(account.sourceAccountId, account.id)) *>
                    // Log the access
                    auditService.logEvent(TokenAuditEvent(
                        timestamp = Instant.now(),
                        eventType = TokenAuditEventType.Access,
                        accountId = account.id,
                        sourceAccountId = Some(account.sourceAccountId),
                        message = "Token retrieved from repository"
                    )) *>
                    ZIO.logInfo(s"Token retrieved from repository: accountId=$accountId")
                        .as(decryptedToken)
            }
        yield token

    /** Get a token for a source account
      */
    override def getTokenBySourceAccountId(sourceAccountId: Long): Task[Option[String]] =
        // Check if we have the mapping in cache
        ZIO.succeed(sourceAccountCache.get(sourceAccountId)).flatMap {
            case Some(accountId) =>
                // We have the mapping, get the token by account ID
                getToken(accountId)

            case None =>
                // No mapping in cache, fetch from repository
                repository.getBySourceAccountId(sourceAccountId).flatMap {
                    case Some(account) =>
                        // Update source account cache
                        val _ = sourceAccountCache.put(account.sourceAccountId, account.id)
                        // Get the token
                        getToken(account.id)

                    case None =>
                        // No account found
                        ZIO.none
                }
        }

    /** Store a token for an account
      */
    override def storeToken(accountId: Long, token: String): Task[Unit] =
        for
            accountOpt <- repository.getById(accountId)
            _ <- ZIO.foreach(accountOpt) { account =>
                // Encrypt the token
                val encryptedToken = encrypt(token, securityConfig.encryptionKey)
                // Update the account
                val updatedAccount = account.copy(token = encryptedToken)
                // Save to repository
                repository.update(updatedAccount) *>
                    // Update cache with decrypted token
                    updateCache(accountId, token) *>
                    // Log the update
                    auditService.logEvent(TokenAuditEvent(
                        timestamp = Instant.now(),
                        eventType = TokenAuditEventType.Update,
                        accountId = account.id,
                        sourceAccountId = Some(account.sourceAccountId),
                        message = "Token updated"
                    )) *> ZIO.logInfo(s"Token updated for account: accountId=$accountId")
            }.orElseFail(new RuntimeException(s"Account not found: $accountId"))
        yield ()

    /** Invalidate cached token for an account
      */
    override def invalidateCache(accountId: Long): Task[Unit] =
        for
            _ <- ZIO.succeed {
                val _ = tokenCache.remove(accountId)
                // Also remove from source account cache if present
                val sourceAccountsToRemove = sourceAccountCache.collect {
                    case (sourceId, accId) if accId == accountId => sourceId
                }
                sourceAccountsToRemove.foreach(sourceAccountCache.remove)
            }
            _ <- auditService.logEvent(TokenAuditEvent(
                timestamp = Instant.now(),
                eventType = TokenAuditEventType.Invalidate,
                accountId = accountId,
                message = "Token cache invalidated"
            ))
            _ <- ZIO.logInfo(s"Cache invalidated for account: accountId=$accountId")
        yield ()

    /** Clear all cached tokens
      */
    override def clearCache: Task[Unit] =
        for
            _ <- ZIO.succeed {
                tokenCache.clear()
                sourceAccountCache.clear()
            }
            _ <- ZIO.logInfo("Token cache cleared")
        yield ()

    // Helper method to update the cache
    private def updateCache(accountId: Long, token: String): Task[Unit] =
        for
            currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
            _ <- ZIO.succeed(tokenCache.put(accountId, TokenCacheEntry(token, currentTime)))
        yield ()

    // Helper method to check if a cache entry is expired
    private def isExpired(entry: TokenCacheEntry): Task[Boolean] =
        for
            currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
        yield currentTime - entry.timestamp > cacheExpirationMs
end FioTokenManagerLive

object FioTokenManagerLive:
    // Cache entry containing the token and timestamp
    private case class TokenCacheEntry(token: String, timestamp: Long)

    /** Create a new FioTokenManager
      *
      * @param repository
      *   The Fio account repository
      * @param securityConfig
      *   The security configuration
      * @return
      *   A new FioTokenManager
      */
    def make(
        repository: FioAccountRepository,
        securityConfig: FioSecurityConfig
    ): URIO[FioTokenAuditService, FioTokenManager] =
        for
            auditService <- ZIO.service[FioTokenAuditService]
        yield new FioTokenManagerLive(repository, securityConfig, auditService)

    def layerWithConfig(securityConfig: FioSecurityConfig)
        : ZLayer[FioAccountRepository & FioTokenAuditService, Nothing, FioTokenManager] =
        ZLayer {
            for
                accountRepository <- ZIO.service[FioAccountRepository]
                auditService <- ZIO.service[FioTokenAuditService]
            yield new FioTokenManagerLive(accountRepository, securityConfig, auditService)
        }

    /** ZIO layer for FioTokenManager
      */
    val layer: ZLayer[
        FioAccountRepository & FioTokenAuditService,
        Config.Error,
        FioTokenManager
    ] =
        ZLayer(ZIO.config[FioSecurityConfig]).flatMap(env =>
            layerWithConfig(env.get[FioSecurityConfig])
        )

    /** Full layer with default configuration
      */
    val fullLayer: ZLayer[FioAccountRepository, Config.Error, FioTokenManager] =
        FioTokenAuditServiceLive.layer >>> layer

    /** AES encryption for token
      *
      * @param value
      *   The value to encrypt
      * @param key
      *   The encryption key
      * @return
      *   The encrypted value
      */
    def encrypt(value: String, key: String): String =
        try
            // Use the first 16 bytes of the key as IV for simplicity
            // In production, consider using a proper IV generation and storage strategy
            val keyBytes = key.getBytes("UTF-8").take(32)
            val iv = keyBytes.take(16)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = new SecretKeySpec(keyBytes, "AES")
            val ivSpec = new IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(value.getBytes("UTF-8"))
            Base64.getEncoder.encodeToString(encrypted)
        catch
            case e: Exception =>
                throw new RuntimeException("Error encrypting token", e)

    /** AES decryption for token
      *
      * @param encryptedValue
      *   The encrypted value
      * @param key
      *   The encryption key
      * @return
      *   The decrypted value
      */
    def decrypt(encryptedValue: String, key: String): String =
        try
            // Use the first 16 bytes of the key as IV for simplicity
            val keyBytes = key.getBytes("UTF-8").take(32)
            val iv = keyBytes.take(16)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = new SecretKeySpec(keyBytes, "AES")
            val ivSpec = new IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val original = cipher.doFinal(Base64.getDecoder.decode(encryptedValue))
            new String(original, "UTF-8")
        catch
            case e: Exception =>
                throw new RuntimeException("Error decrypting token", e)
end FioTokenManagerLive
