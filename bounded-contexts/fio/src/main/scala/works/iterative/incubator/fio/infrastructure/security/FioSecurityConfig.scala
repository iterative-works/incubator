package works.iterative.incubator.fio.infrastructure.security

import zio.*

/** Configuration for Fio security components
  *
  * This configuration holds security-related settings for the Fio integration, including encryption
  * keys and token security parameters.
  *
  * Classification: Infrastructure Configuration
  */
case class FioSecurityConfig(
    encryptionKey: String, // Key for token encryption
    cacheExpirationMinutes: Int = 30 // Token cache expiration time in minutes
)

object FioSecurityConfig:
    given config: Config[FioSecurityConfig] =
        import Config.*
        (
            string("encryption_key") ?? "Key for token encryption" zip
                int("cache_expiration_minutes").withDefault(
                    30
                ) ?? "Token cache expiration time in minutes"
        ).nested("fio_security").map {
            case (encryptionKey, cacheExpirationMinutes) =>
                FioSecurityConfig(encryptionKey, cacheExpirationMinutes)
        }
    end config

    /** Generate a secure random encryption key
      *
      * This is useful for initial setup or key rotation.
      *
      * @return
      *   A random encryption key suitable for AES-256
      */
    def generateSecureKey(): String =
        val keyBytes = new Array[Byte](32) // 256 bits
        val secureRandom = new java.security.SecureRandom()
        secureRandom.nextBytes(keyBytes)
        java.util.Base64.getEncoder.encodeToString(keyBytes)
    end generateSecureKey
end FioSecurityConfig
