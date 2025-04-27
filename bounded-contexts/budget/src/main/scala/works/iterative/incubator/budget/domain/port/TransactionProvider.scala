package works.iterative.incubator.budget.domain.port

import zio.*
import java.time.LocalDate

import works.iterative.incubator.budget.domain.model.*

/** Port interface for retrieving transactions from external financial sources.
  *
  * This port abstracts the interaction with external transaction data providers like banks, credit
  * card companies, or financial institutions. It handles the connection, authentication, data
  * retrieval, and transformation to the domain model.
  *
  * Implementations of this interface are responsible for:
  *   - Establishing connections to specific financial institutions
  *   - Authenticating with the required credentials
  *   - Retrieving transaction data based on date ranges
  *   - Converting external transaction formats to domain model
  *   - Handling connection failures and retries
  */
trait TransactionProvider:
    /** Retrieves transactions from the external provider within the specified date range.
      *
      * @param sourceAccount
      *   The account from which to retrieve transactions
      * @param fromDate
      *   The starting date for transaction retrieval (inclusive)
      * @param toDate
      *   The ending date for transaction retrieval (inclusive)
      * @return
      *   A ZIO effect containing a list of transactions or a provider error
      */
    def getTransactions(
        sourceAccount: SourceAccount,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ZIO[Any, TransactionProviderError, List[Transaction]]

    /** Checks if a connection to the provider can be established.
      *
      * @param sourceAccount
      *   The account to check connection for
      * @return
      *   A ZIO effect indicating success or a provider error
      */
    def testConnection(sourceAccount: SourceAccount): ZIO[Any, TransactionProviderError, Unit]
end TransactionProvider

/** Represents errors that can occur during transaction provider interactions.
  */
sealed trait TransactionProviderError

object TransactionProviderError:
    /** Authentication with the provider failed */
    final case class AuthenticationFailed(message: String) extends TransactionProviderError

    /** Connection to the provider could not be established */
    final case class ConnectionFailed(message: String) extends TransactionProviderError

    /** Provider API rate limit was exceeded */
    case object RateLimitExceeded extends TransactionProviderError

    /** Error while retrieving transaction data */
    final case class DataRetrievalError(message: String) extends TransactionProviderError

    /** Error while transforming external data to domain model */
    final case class DataTransformationError(message: String) extends TransactionProviderError

    /** Provider returned an unexpected or unknown error */
    final case class UnexpectedError(cause: String) extends TransactionProviderError
end TransactionProviderError
