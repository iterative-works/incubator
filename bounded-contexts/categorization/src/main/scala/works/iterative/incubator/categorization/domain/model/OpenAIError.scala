package works.iterative.incubator.categorization.domain.model

/** Domain-specific errors related to OpenAI operations
  *
  * This error hierarchy defines all the domain-relevant error types that can occur
  * when interacting with OpenAI API for payee cleanup operations.
  */
sealed trait OpenAIError extends Throwable:
    def message: String
    override def getMessage: String = message
end OpenAIError

object OpenAIError:
    /** Authentication or authorization failed with OpenAI API */
    case class AuthenticationError(message: String) extends OpenAIError

    /** API rate limit exceeded */
    case class RateLimitError(message: String) extends OpenAIError

    /** Temporary service unavailability */
    case class ServiceUnavailableError(message: String) extends OpenAIError
    
    /** Invalid request format or parameters */
    case class ValidationError(message: String) extends OpenAIError
    
    /** Error parsing response from OpenAI */
    case class ResponseParsingError(message: String, cause: Option[Throwable] = None) extends OpenAIError:
        cause.foreach(initCause)
    
    /** The model failed to generate a valid response */
    case class ModelError(message: String) extends OpenAIError
    
    /** Connection issues or timeouts */
    case class ConnectionError(message: String, cause: Option[Throwable] = None) extends OpenAIError:
        cause.foreach(initCause)
    
    /** Unexpected error */
    case class UnexpectedError(message: String, cause: Option[Throwable] = None) extends OpenAIError:
        cause.foreach(initCause)
end OpenAIError