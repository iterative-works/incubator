package works.iterative.incubator.fio.domain.model.error

/** Base trait for all Fio API errors
  *
  * Classification: Domain Error Type
  */
sealed trait FioApiError extends Throwable:
    def message: String
    override def getMessage: String = message
end FioApiError

/** Error for authentication issues (e.g., invalid token)
  */
case class FioAuthenticationError(message: String) extends FioApiError

/** Error for validation issues (e.g., invalid parameters)
  */
case class FioValidationError(message: String) extends FioApiError

/** Error for rate limiting issues
  */
case class FioRateLimitError(message: String) extends FioApiError

/** Error for resource not found issues
  */
case class FioResourceNotFoundError(message: String) extends FioApiError

/** General network error
  */
case class FioNetworkError(cause: Throwable) extends FioApiError:
    override def message: String = s"Network error: ${cause.getMessage}"
    override def getCause: Throwable = cause
end FioNetworkError

/** Error for API server errors
  */
case class FioServerError(message: String) extends FioApiError

/** Error for unexpected response formats
  */
case class FioParsingError(message: String, cause: Option[Throwable] = None) extends FioApiError:
    override def getCause: Throwable = cause.orNull
