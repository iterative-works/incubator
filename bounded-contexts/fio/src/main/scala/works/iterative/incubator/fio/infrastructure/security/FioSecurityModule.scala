package works.iterative.incubator.fio.infrastructure.security

import zio.*
import works.iterative.incubator.fio.domain.model.FioAccountRepository

/** Module providing all security services for the Fio integration
  *
  * This module bundles all security-related components for easier integration.
  *
  * Classification: Infrastructure Module
  */
object FioSecurityModule:
    /** Security components with default configuration
      *
      * @return
      *   ZLayer containing all security components
      */
    def defaultLayer: ZLayer[FioAccountRepository, Config.Error, FioTokenManager] =
        FioTokenManagerLive.fullLayer

    /** Security components with custom configuration
      *
      * @param encryptionKey
      *   The encryption key to use
      * @param cacheExpirationMinutes
      *   Cache expiration time in minutes
      * @return
      *   ZLayer containing all security components
      */
    def layerWithConfig(
        encryptionKey: String,
        cacheExpirationMinutes: Int = 30
    ): ZLayer[FioAccountRepository, Nothing, FioTokenManager] =
        // Create a custom security config
        val config = FioSecurityConfig(
            encryptionKey = encryptionKey,
            cacheExpirationMinutes = cacheExpirationMinutes
        )

        // Create the module with the custom config
        val auditLayer = FioTokenAuditServiceLive.layer

        auditLayer >>> FioTokenManagerLive.layerWithConfig(config)
    end layerWithConfig

    /** Generate a new encryption key for setup
      *
      * @return
      *   A secure random encryption key
      */
    def generateEncryptionKey: String =
        FioSecurityConfig.generateSecureKey()
end FioSecurityModule
