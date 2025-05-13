package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.AccountId
import zio.*
import java.time.Instant
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import java.util.Base64

/** Service for secure management of Fio Bank API tokens.
  *
  * Provides functionality for securely storing, retrieving, and encrypting Fio API tokens. Uses
  * AES-256 encryption and implements token caching to minimize database lookups.
  *
  * Category: Infrastructure Service Layer: Infrastructure
  */
trait FioTokenManager:
    /** Retrieves a Fio API token for the specified account.
      *
      * @param accountId
      *   The account ID to retrieve the token for
      * @return
      *   A ZIO effect that completes with the token or fails with an error
      */
    def getToken(accountId: AccountId): ZIO[Any, String, String]

    /** Stores a new Fio API token for the specified account.
      *
      * @param accountId
      *   The account ID to store the token for
      * @param token
      *   The unencrypted Fio API token
      * @return
      *   A ZIO effect that completes with Unit or fails with an error
      */
    def storeToken(accountId: AccountId, token: String): ZIO[Any, String, Unit]

    /** Encrypts a token for secure storage.
      *
      * @param token
      *   The raw token to encrypt
      * @return
      *   The encrypted token as a string
      */
    def encryptToken(token: String): ZIO[Any, String, String]

    /** Decrypts a token for use.
      *
      * @param encryptedToken
      *   The encrypted token to decrypt
      * @return
      *   The decrypted raw token
      */
    def decryptToken(encryptedToken: String): ZIO[Any, String, String]

    /** Clears the token cache for testing or when updates occur outside this service.
      *
      * @return
      *   A ZIO effect that completes with Unit
      */
    def clearCache(): UIO[Unit]
end FioTokenManager

/** Live implementation of FioTokenManager.
  *
  * Provides secure token management with encryption and caching.
  *
  * @param repository
  *   The repository for FioAccount entities
  * @param encryptionKey
  *   The key used for token encryption (must be exactly 32 bytes for AES-256)
  * @param tokenCacheRef
  *   Thread-safe reference to the token cache
  */
final case class FioTokenManagerLive(
    repository: FioAccountRepository,
    encryptionKey: Array[Byte],
    tokenCacheRef: Ref[Map[String, String]]
) extends FioTokenManager:

    /** Retrieves a Fio API token for the specified account.
      *
      * First checks the cache, then falls back to the repository if needed.
      *
      * @param accountId
      *   The account ID to retrieve the token for
      * @return
      *   A ZIO effect that completes with the token or fails with an error
      */
    override def getToken(accountId: AccountId): ZIO[Any, String, String] =
        val accountKey = accountId.toString

        // First check if the token is in the cache
        tokenCacheRef.get.flatMap { cache =>
            cache.get(accountKey) match
                case Some(token) =>
                    // Log the token access (for audit)
                    ZIO.logInfo(s"Token retrieved from cache for account $accountKey") *>
                        ZIO.succeed(token)

                case None =>
                    // If not in cache, retrieve from repository and decrypt
                    for
                        accountOpt <- repository.findBySourceAccountId(accountId)
                        account <- ZIO.fromOption(accountOpt).mapError(_ =>
                            s"No Fio account found for source account $accountKey"
                        )
                        decryptedToken <- decryptToken(account.encryptedToken)
                        // Add to cache for future use
                        _ <- tokenCacheRef.update(cache => cache + (accountKey -> decryptedToken))
                        // Log the token access (for audit)
                        _ <- ZIO.logInfo(
                            s"Token retrieved from repository for account $accountKey"
                        )
                    yield decryptedToken
        }
    end getToken

    /** Stores a new Fio API token for the specified account.
      *
      * @param accountId
      *   The account ID to store the token for
      * @param token
      *   The unencrypted Fio API token
      * @return
      *   A ZIO effect that completes with Unit or fails with an error
      */
    override def storeToken(accountId: AccountId, token: String): ZIO[Any, String, Unit] =
        val accountKey = accountId.toString

        for
            encryptedToken <- encryptToken(token)
            accountOpt <- repository.findBySourceAccountId(accountId)
            _ <- {
                accountOpt match
                    case Some(account) =>
                        // Update existing account
                        val updatedAccount = account.copy(
                            encryptedToken = encryptedToken,
                            updatedAt = Instant.now()
                        )
                        repository.save(updatedAccount)
                    case None =>
                        // Create new account
                        FioAccountRepository.createAccount(accountId, encryptedToken).unit
            }.provide(ZLayer.succeed(repository))
            // Update cache
            _ <- tokenCacheRef.update(cache => cache + (accountKey -> token))
            // Log the token update (for audit)
            _ <- ZIO.logInfo(s"Token stored for account $accountKey")
        yield ()
        end for
    end storeToken

    /** Encrypts a token for secure storage.
      *
      * Uses AES-256 encryption in CBC mode with a random initialization vector (IV). The IV is
      * prepended to the encrypted data for decryption.
      *
      * @param token
      *   The raw token to encrypt
      * @return
      *   The encrypted token as a Base64-encoded string
      */
    override def encryptToken(token: String): ZIO[Any, String, String] =
        ZIO.attemptBlocking {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = new SecretKeySpec(encryptionKey, "AES")

            // Generate a random IV
            val ivBytes = new Array[Byte](16)
            new SecureRandom().nextBytes(ivBytes)
            val ivSpec = new IvParameterSpec(ivBytes)

            // Initialize the cipher for encryption
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            // Encrypt the token
            val encrypted = cipher.doFinal(token.getBytes("UTF-8"))

            // Combine IV and encrypted data
            val combined = new Array[Byte](ivBytes.length + encrypted.length)
            java.lang.System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length)
            java.lang.System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length)

            // Base64 encode the result
            Base64.getEncoder.encodeToString(combined)
        }.mapError(e => s"Token encryption failed: ${e.getMessage}")

    /** Decrypts a token for use.
      *
      * @param encryptedToken
      *   The Base64-encoded encrypted token to decrypt
      * @return
      *   The decrypted raw token
      */
    override def decryptToken(encryptedToken: String): ZIO[Any, String, String] =
        ZIO.attemptBlocking {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = new SecretKeySpec(encryptionKey, "AES")

            // Decode from Base64
            val combined = Base64.getDecoder.decode(encryptedToken)

            // Extract IV
            val ivBytes = combined.take(16)
            val ivSpec = new IvParameterSpec(ivBytes)

            // Extract encrypted data
            val encrypted = combined.drop(16)

            // Initialize the cipher for decryption
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            // Decrypt the token
            new String(cipher.doFinal(encrypted), "UTF-8")
        }.mapError(e => s"Token decryption failed: ${e.getMessage}")

    /** Clears the token cache.
      *
      * @return
      *   A ZIO effect that completes with Unit
      */
    override def clearCache(): UIO[Unit] =
        tokenCacheRef.set(Map.empty) *> ZIO.logInfo("Token cache cleared")
end FioTokenManagerLive

/** Companion object for FioTokenManager.
  */
object FioTokenManager:
    /** Retrieves a Fio API token for the specified account.
      *
      * @param accountId
      *   The account ID to retrieve the token for
      * @return
      *   A ZIO effect that requires FioTokenManager and returns the token or fails with an error
      */
    def getToken(accountId: AccountId): ZIO[FioTokenManager, String, String] =
        ZIO.serviceWithZIO(_.getToken(accountId))

    /** Stores a new Fio API token for the specified account.
      *
      * @param accountId
      *   The account ID to store the token for
      * @param token
      *   The unencrypted Fio API token
      * @return
      *   A ZIO effect that requires FioTokenManager and returns Unit or fails with an error
      */
    def storeToken(accountId: AccountId, token: String): ZIO[FioTokenManager, String, Unit] =
        ZIO.serviceWithZIO(_.storeToken(accountId, token))

    /** Encrypts a token for secure storage.
      *
      * @param token
      *   The raw token to encrypt
      * @return
      *   A ZIO effect that requires FioTokenManager and returns the encrypted token as a string
      */
    def encryptToken(token: String): ZIO[FioTokenManager, String, String] =
        ZIO.serviceWithZIO(_.encryptToken(token))

    /** Decrypts a token for use.
      *
      * @param encryptedToken
      *   The encrypted token to decrypt
      * @return
      *   A ZIO effect that requires FioTokenManager and returns the decrypted raw token
      */
    def decryptToken(encryptedToken: String): ZIO[FioTokenManager, String, String] =
        ZIO.serviceWithZIO(_.decryptToken(encryptedToken))

    /** Clears the token cache.
      *
      * @return
      *   A ZIO effect that requires FioTokenManager and completes with Unit
      */
    def clearCache(): URIO[FioTokenManager, Unit] =
        ZIO.serviceWithZIO(_.clearCache())

    /** Creates a FioTokenManager layer with the specified configuration.
      *
      * @param encryptionKey
      *   The key to use for encryption (must be exactly 32 bytes for AES-256)
      * @return
      *   A ZLayer that provides a FioTokenManager
      */
    def live(encryptionKey: Array[Byte]): ZLayer[FioAccountRepository, Nothing, FioTokenManager] =
        ZLayer.scoped {
            for
                repository <- ZIO.service[FioAccountRepository]
                tokenCacheRef <- Ref.make(Map.empty[String, String])
                manager = FioTokenManagerLive(repository, encryptionKey, tokenCacheRef)
            yield manager
        }

    /** Creates a FioTokenManager layer using the default secret key from configuration.
      *
      * @return
      *   A ZLayer that provides a FioTokenManager
      */
    val layer: ZLayer[FioAccountRepository, Throwable, FioTokenManager] =
        ZLayer {
            for
                config <- ZIO.config[FioConfig]
                repository <- ZIO.service[FioAccountRepository]
                bytes <- ZIO.attempt(config.encryptionKey.getBytes("UTF-8"))
                // Ensure the key is exactly 32 bytes (AES-256)
                key = if bytes.length < 32 then
                    // Pad with zeros if too short
                    bytes.padTo(32, 0.toByte)
                else if bytes.length > 32 then
                    // Truncate if too long
                    bytes.take(32)
                else
                    bytes
                tokenCacheRef <- Ref.make(Map.empty[String, String])
                manager = FioTokenManagerLive(repository, key, tokenCacheRef)
            yield manager
        }
end FioTokenManager
