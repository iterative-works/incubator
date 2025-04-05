package works.iterative.incubator.ynab

/** YNAB Configuration
  *
  * @deprecated
  *   This file is maintained for backward compatibility. Use the configuration classes in the
  *   infrastructure.config package instead.
  */

// Re-export configuration classes for backwards compatibility
type YnabConfig = infrastructure.config.YnabConfig
val YnabConfig = infrastructure.config.YnabConfig

type SecretApiToken = infrastructure.config.SecretApiToken
val SecretApiToken = infrastructure.config.SecretApiToken

type YnabConfigService = infrastructure.config.YnabConfigService

type YnabConfigError = infrastructure.config.YnabConfigError
val YnabNotConfigured = infrastructure.config.YnabNotConfigured
val YnabInvalidToken = infrastructure.config.YnabInvalidToken
val YnabConfigSaveError = infrastructure.config.YnabConfigSaveError
