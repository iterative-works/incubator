package works.iterative.incubator.budget.infrastructure.adapter.fio

import zio.*
import java.time.Duration

/** Configuration for Fio Bank API integration.
  *
  * Contains settings for API endpoints, security, timeouts, and retry policies.
  *
  * Category: Configuration Layer: Infrastructure
  */
case class FioConfig(
    /** Base URL for the Fio Bank API */
    baseUrl: String,

    /** Maximum date range allowed in days */
    maxDateRangeDays: Int,

    /** Connection timeout in seconds */
    connectionTimeoutSeconds: Int,

    /** Request timeout in seconds */
    requestTimeoutSeconds: Int,

    /** Maximum number of retries for failed requests */
    maxRetries: Int,

    /** Initial backoff duration for retry in seconds */
    initialBackoffSeconds: Int,

    /** Maximum backoff duration for retry in seconds */
    maxBackoffSeconds: Int,

    /** Secret key for token encryption (must be capable of producing 32 bytes for AES-256) */
    encryptionKey: String
)

/** Companion object for FioConfig.
  */
object FioConfig:
    /** Default configuration values. */
    val defaultConfig = FioConfig(
        baseUrl = "https://fioapi.fio.cz/v1/rest",
        maxDateRangeDays = 90,
        connectionTimeoutSeconds = 10,
        requestTimeoutSeconds = 30,
        maxRetries = 3,
        initialBackoffSeconds = 5,
        maxBackoffSeconds = 60,
        encryptionKey = "default-key-must-be-replaced-in-production" // Should be overridden
    )

    /** Creates a ZIO schedule for retry with exponential backoff based on config.
      *
      * @param config
      *   The Fio API configuration
      * @return
      *   A schedule for retries
      */
    def retrySchedule(config: FioConfig): Schedule[Any, Throwable, Any] =
        val filter: Throwable => Boolean = {
            case _: java.net.ConnectException       => true
            case _: java.io.IOException             => true
            case _: java.net.SocketTimeoutException => true
            case e: Throwable
                if e.getMessage != null &&
                    (e.getMessage.contains("status code: 409") ||
                        e.getMessage.contains("Too Many Requests")) => true
            case _ => false
        }

        Schedule.exponential(Duration.ofSeconds(config.initialBackoffSeconds))
            .whileOutput(_ <= Duration.ofSeconds(config.maxBackoffSeconds))
            .upTo(Duration.ofMinutes(5))
            .whileInput(filter) &&
        Schedule.recurs(config.maxRetries.toLong)
    end retrySchedule

    /** Creates a layer with the default configuration.
      *
      * @return
      *   A ZLayer providing FioConfig
      */
    val layer: ULayer[FioConfig] = ZLayer.succeed(defaultConfig)

    /** Creates a layer with a custom configuration.
      *
      * @param config
      *   The custom configuration
      * @return
      *   A ZLayer providing FioConfig
      */
    def live(config: FioConfig): ULayer[FioConfig] = ZLayer.succeed(config)
end FioConfig
