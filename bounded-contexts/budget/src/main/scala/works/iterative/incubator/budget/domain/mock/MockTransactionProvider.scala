package works.iterative.incubator.budget.domain.mock

import zio.*
import java.time.LocalDate

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.port.*
import works.iterative.incubator.budget.domain.port.{TransactionProvider, TransactionProviderError}

/** Mock implementation of the TransactionProvider port for testing purposes.
  *
  * This mock provides configurable behavior for testing transaction import scenarios including:
  *   - Predefined transaction sets for specific accounts
  *   - Configurable errors for testing failure scenarios
  *   - Tracking of method invocations for verification
  */
case class MockTransactionProvider(
    data: Ref[Map[String, List[Transaction]]],
    errors: Ref[MockErrorConfig],
    invocations: Ref[List[ProviderInvocation]]
) extends TransactionProvider:
    /** Retrieves pre-configured transactions for the given account and date range.
      *
      * @param sourceAccount
      *   The account from which to retrieve transactions
      * @param fromDate
      *   Starting date for the request
      * @param toDate
      *   Ending date for the request
      * @return
      *   ZIO effect with transactions or configured error
      */
    def getTransactions(
        sourceAccount: SourceAccount,
        fromDate: LocalDate,
        toDate: LocalDate
    ): ZIO[Any, TransactionProviderError, List[Transaction]] =
        for
            config <- errors.get
            _ <- invocations.update(_ :+ ProviderInvocation.GetTransactions(
                sourceAccount,
                fromDate,
                toDate
            ))
            result <- config.getTransactionsError match
                case Some(error) => ZIO.fail(error)
                case None =>
                    data.get.map { dataMap =>
                        dataMap
                            .getOrElse(sourceAccount.id.toString, List.empty)
                            .filter(tx =>
                                !tx.date.isBefore(fromDate) && !tx.date.isAfter(toDate)
                            )
                    }
        yield result

    /** Tests connection to the mock provider.
      *
      * @param sourceAccount
      *   The account to check connection for
      * @return
      *   ZIO effect with success or configured error
      */
    def testConnection(sourceAccount: SourceAccount): ZIO[Any, TransactionProviderError, Unit] =
        for
            config <- errors.get
            _ <- invocations.update(_ :+ ProviderInvocation.TestConnection(sourceAccount))
            result <- config.connectionError match
                case Some(error) => ZIO.fail(error)
                case None        => ZIO.unit
        yield result

    // Configuration methods for testing

    /** Configures the mock to return specific transactions for an account.
      *
      * @param accountId
      *   The account ID to configure transactions for
      * @param transactions
      *   List of transactions to return
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setTransactionsForAccount(
        accountId: String,
        transactions: List[Transaction]
    ): UIO[Unit] =
        data.update(map => map + (accountId -> transactions))

    /** Configures an error to be thrown on getTransactions calls.
      *
      * @param error
      *   The error to be thrown
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setGetTransactionsError(error: TransactionProviderError): UIO[Unit] =
        errors.update(config => config.copy(getTransactionsError = Some(error)))

    /** Configures an error to be thrown on testConnection calls.
      *
      * @param error
      *   The error to be thrown
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setConnectionError(error: TransactionProviderError): UIO[Unit] =
        errors.update(config => config.copy(connectionError = Some(error)))

    /** Clears all configured errors.
      *
      * @return
      *   ZIO effect updating the mock configuration
      */
    def clearErrors: UIO[Unit] =
        errors.set(MockErrorConfig())

    /** Records all method invocations for verification.
      *
      * @return
      *   ZIO effect with a list of recorded invocations
      */
    def getInvocations: UIO[List[ProviderInvocation]] =
        invocations.get

    /** Resets the mock to its initial state.
      *
      * @return
      *   ZIO effect resetting the mock
      */
    def reset: UIO[Unit] =
        data.set(Map.empty) *>
            errors.set(MockErrorConfig()) *>
            invocations.set(List.empty)
end MockTransactionProvider

object MockTransactionProvider:
    /** Creates a new MockTransactionProvider with empty data.
      *
      * @return
      *   ZIO effect creating the mock
      */
    def make: UIO[MockTransactionProvider] =
        for
            dataRef <- Ref.make(Map.empty[String, List[Transaction]])
            errorsRef <- Ref.make(MockErrorConfig())
            invocationsRef <- Ref.make(List.empty[ProviderInvocation])
        yield MockTransactionProvider(dataRef, errorsRef, invocationsRef)

    /** Creates a MockTransactionProvider with pre-loaded data.
      *
      * @param initialData
      *   Map of account IDs to transaction lists
      * @return
      *   ZIO effect creating the mock with data
      */
    def withData(
        initialData: Map[String, List[Transaction]]
    ): UIO[MockTransactionProvider] =
        for
            mock <- make
            _ <- mock.data.set(initialData)
        yield mock

    /** Creates a ZIO layer for the MockTransactionProvider.
      *
      * @return
      *   ZLayer containing the MockTransactionProvider as a TransactionProvider
      */
    val layer: ULayer[TransactionProvider] =
        ZLayer.scoped {
            ZIO.acquireRelease(
                make.map(provider => provider.asInstanceOf[TransactionProvider])
            )(_ => ZIO.unit)
        }

    /** Creates a ZIO layer for the MockTransactionProvider, exposing the mock interfaces.
      *
      * @return
      *   ZLayer containing the MockTransactionProvider
      */
    val mockLayer: ULayer[MockTransactionProvider] =
        ZLayer.scoped {
            ZIO.acquireRelease(
                make
            )(_.reset)
        }
end MockTransactionProvider

/** Configuration for error simulation in the mock provider. */
case class MockErrorConfig(
    getTransactionsError: Option[TransactionProviderError] = None,
    connectionError: Option[TransactionProviderError] = None
)

/** Records method invocations for verification in tests. */
sealed trait ProviderInvocation

object ProviderInvocation:
    /** Records a getTransactions call */
    case class GetTransactions(
        sourceAccount: SourceAccount,
        fromDate: LocalDate,
        toDate: LocalDate
    ) extends ProviderInvocation

    /** Records a testConnection call */
    case class TestConnection(
        sourceAccount: SourceAccount
    ) extends ProviderInvocation
end ProviderInvocation
