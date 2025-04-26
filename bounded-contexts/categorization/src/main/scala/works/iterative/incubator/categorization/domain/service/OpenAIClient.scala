package works.iterative.incubator.categorization.domain.service

import works.iterative.incubator.categorization.domain.model.OpenAIError
import works.iterative.incubator.categorization.domain.model.PayeeCleanupRule
import zio.*

/** Interface for interacting with OpenAI API for payee cleanup operations
  *
  * This service is responsible for communicating with OpenAI API to:
  *   - Clean up payee names based on transaction context
  *   - Generate payee cleanup rules based on patterns identified
  *   - Extract structured information from AI responses
  *
  * Classification: Infrastructure Port (Domain Interface)
  */
trait OpenAIClient:
    /** Clean up a payee name using OpenAI's language models
      *
      * @param original
      *   The original messy payee name
      * @param context
      *   Additional transaction context like counterAccount, message, etc.
      * @return
      *   An effect that produces a cleaned payee name and optionally a suggested rule
      */
    def cleanupPayee(
        original: String,
        context: Map[String, String]
    ): IO[OpenAIError, (String, Option[PayeeCleanupRule])]

    /** Check if the OpenAI client is configured and can connect to API
      *
      * @return
      *   An effect that completes successfully if the client is healthy
      */
    def healthCheck(): IO[OpenAIError, Unit]
end OpenAIClient
