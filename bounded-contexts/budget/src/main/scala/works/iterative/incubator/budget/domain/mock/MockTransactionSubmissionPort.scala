package works.iterative.incubator.budget.domain.mock

import zio.*

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.port.*

/** Mock implementation of the TransactionSubmissionPort port for testing purposes.
  *
  * This mock provides configurable behavior for testing transaction submission scenarios including:
  *   - Configurable validation rules
  *   - Simulated submission responses
  *   - Error simulation
  *   - Submission tracking for verification
  */
case class MockTransactionSubmissionPort(
    config: Ref[SubmissionConfig],
    submittedTransactions: Ref[Map[TransactionId, String]],
    invocations: Ref[List[SubmissionInvocation]]
):
    /** Validates if transactions are ready for submission based on configuration.
      *
      * @param transaction
      *   The transaction to validate
      * @param processingState
      *   The current processing state
      * @return
      *   ZIO effect with validation result
      */
    def validateForSubmission(
        transaction: Transaction,
        processingState: TransactionProcessingState
    ): ZIO[Any, Nothing, ValidationResult] =
        for
            cfg <- config.get
            _ <- invocations.update(_ :+ SubmissionInvocation.Validate(transaction, processingState))
            result <- ZIO.succeed {
                val validationErrors = List.newBuilder[String]

                // Check for required category
                if processingState.effectiveCategory.isEmpty && cfg.requireCategory then
                    validationErrors += "Category is required"

                // Check for minimum amount
                if transaction.amount < cfg.minimumAmount && cfg.checkMinimumAmount then
                    validationErrors += s"Amount must be at least ${cfg.minimumAmount}"

                // Check for message/comment length
                if transaction.message.getOrElse("").isEmpty && 
                   transaction.comment.getOrElse("").isEmpty && 
                   cfg.minimumDescriptionLength > 0 then
                    validationErrors += s"Message or comment must be at least ${cfg.minimumDescriptionLength} characters"

                // Check custom validation
                cfg.customValidation.foreach { validator =>
                    validator(transaction, processingState).foreach(validationErrors += _)
                }

                val errors = validationErrors.result()
                if errors.isEmpty then ValidationResult.Valid
                else ValidationResult.Invalid(errors)
            }
        yield result

    /** Submits a single transaction to the mock external system.
      *
      * @param transaction
      *   The transaction to submit
      * @param processingState
      *   The current processing state
      * @return
      *   ZIO effect with submission result or configured error
      */
    def submitTransaction(
        transaction: Transaction,
        processingState: TransactionProcessingState
    ): ZIO[Any, SubmissionError, TransactionSubmissionResult] =
        for
            cfg <- config.get
            _ <- invocations.update(_ :+ SubmissionInvocation.SubmitOne(transaction, processingState))
            
            // Check if we should fail this submission
            _ <- cfg.singleSubmissionError match
                case Some(error) => ZIO.fail(error)
                case None => ZIO.unit
                
            // Check if this transaction was already submitted
            alreadySubmitted <- submittedTransactions.get.map(_.contains(transaction.id))
            
            // Generate an external ID for successful submissions
            externalId <- if alreadySubmitted then
                submittedTransactions.get.map(_.get(transaction.id))
            else
                ZIO.succeed(Some(s"ext-${scala.util.Random.alphanumeric.take(8).mkString}"))
                
            // Record the submission
            _ <- if !alreadySubmitted && externalId.isDefined then
                submittedTransactions.update(_ + (transaction.id -> externalId.get))
            else 
                ZIO.unit
            
            // Create the result
            result = (alreadySubmitted, externalId) match
                case (true, Some(id)) => 
                    TransactionSubmissionResult(
                        transactionId = transaction.id,
                        externalId = Some(id),
                        status = TransactionSubmissionStatus.AlreadySubmitted
                    )
                case (_, Some(id)) =>
                    TransactionSubmissionResult(
                        transactionId = transaction.id,
                        externalId = Some(id),
                        status = TransactionSubmissionStatus.Submitted
                    )
                case _ =>
                    TransactionSubmissionResult(
                        transactionId = transaction.id,
                        externalId = None,
                        status = TransactionSubmissionStatus.Failed("Failed to generate external ID")
                    )
        yield result

    /** Submits multiple transactions to the mock external system.
      *
      * @param transactions
      *   The transactions to submit with their processing states
      * @return
      *   ZIO effect with submission results or configured error
      */
    def submitTransactions(
        transactions: List[(Transaction, TransactionProcessingState)]
    ): ZIO[Any, SubmissionError, List[TransactionSubmissionResult]] =
        for
            cfg <- config.get
            _ <- invocations.update(_ :+ SubmissionInvocation.SubmitBatch(transactions))
            
            // Check if we should fail the batch submission
            _ <- cfg.batchSubmissionError match
                case Some(error) => ZIO.fail(error)
                case None => ZIO.unit
                
            // Validate the batch first if required
            _ <- if cfg.validateBeforeSubmission then
                ZIO.foreach(transactions) { case (tx, state) =>
                    validateForSubmission(tx, state).flatMap {
                        case ValidationResult.Valid => ZIO.unit
                        case ValidationResult.Invalid(reasons) =>
                            if cfg.failOnValidationError then
                                ZIO.fail(SubmissionError.ValidationFailed(List(tx.id)))
                            else ZIO.unit
                    }
                }
            else ZIO.unit
            
            // Process each transaction
            results <- ZIO.foreach(transactions) { case (tx, state) =>
                submitTransaction(tx, state).catchAll { error =>
                    ZIO.succeed(
                        TransactionSubmissionResult(
                            transactionId = tx.id,
                            externalId = None,
                            status = TransactionSubmissionStatus.Failed(error.toString)
                        )
                    )
                }
            }
        yield results

    /** Tests connection to the mock external system.
      *
      * @return
      *   ZIO effect with success or configured error
      */
    def testConnection(): ZIO[Any, SubmissionError, Unit] =
        for
            cfg <- config.get
            _ <- invocations.update(_ :+ SubmissionInvocation.TestConnection)
            result <- cfg.connectionError match
                case Some(error) => ZIO.fail(error)
                case None => ZIO.unit
        yield result

    // Configuration methods for testing

    /** Sets whether categories are required for submission.
      *
      * @param required
      *   Whether categories should be required
      * @return
      *   ZIO effect updating the mock configuration
      */
    def requireCategory(required: Boolean): UIO[Unit] =
        config.update(_.copy(requireCategory = required))

    /** Sets a minimum amount for transactions to be valid.
      *
      * @param amount
      *   The minimum transaction amount
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setMinimumAmount(amount: BigDecimal): UIO[Unit] =
        config.update(cfg => cfg.copy(minimumAmount = amount, checkMinimumAmount = true))

    /** Sets a minimum description length for transactions to be valid.
      *
      * @param length
      *   The minimum description length
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setMinimumDescriptionLength(length: Int): UIO[Unit] =
        config.update(_.copy(minimumDescriptionLength = length))

    /** Sets a custom validation function for transactions.
      *
      * @param validator
      *   Function that returns validation errors
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setCustomValidation(
        validator: (Transaction, TransactionProcessingState) => List[String]
    ): UIO[Unit] =
        config.update(_.copy(customValidation = Some(validator)))

    /** Configures whether validation should run before submission.
      *
      * @param validate
      *   Whether to validate before submitting
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setValidateBeforeSubmission(validate: Boolean): UIO[Unit] =
        config.update(_.copy(validateBeforeSubmission = validate))

    /** Configures whether to fail on validation errors.
      *
      * @param fail
      *   Whether to fail on validation errors
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setFailOnValidationError(fail: Boolean): UIO[Unit] =
        config.update(_.copy(failOnValidationError = fail))

    /** Configures an error for single transaction submission.
      *
      * @param error
      *   The error to throw
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setSingleSubmissionError(error: SubmissionError): UIO[Unit] =
        config.update(_.copy(singleSubmissionError = Some(error)))

    /** Configures an error for batch transaction submission.
      *
      * @param error
      *   The error to throw
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setBatchSubmissionError(error: SubmissionError): UIO[Unit] =
        config.update(_.copy(batchSubmissionError = Some(error)))

    /** Configures an error for connection testing.
      *
      * @param error
      *   The error to throw
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setConnectionError(error: SubmissionError): UIO[Unit] =
        config.update(_.copy(connectionError = Some(error)))

    /** Clears all configured errors.
      *
      * @return
      *   ZIO effect clearing error configurations
      */
    def clearErrors: UIO[Unit] =
        config.update(cfg =>
            cfg.copy(
                singleSubmissionError = None,
                batchSubmissionError = None,
                connectionError = None
            )
        )

    /** Gets a list of all submitted transactions.
      *
      * @return
      *   ZIO effect with map of transaction IDs to external IDs
      */
    def getSubmittedTransactions: UIO[Map[TransactionId, String]] =
        submittedTransactions.get

    /** Gets all recorded invocations for verification.
      *
      * @return
      *   ZIO effect with list of recorded invocations
      */
    def getInvocations: UIO[List[SubmissionInvocation]] =
        invocations.get

    /** Resets the mock to its initial state.
      *
      * @return
      *   ZIO effect resetting the mock
      */
    def reset: UIO[Unit] =
        config.set(SubmissionConfig.default) *>
        submittedTransactions.set(Map.empty) *>
        invocations.set(List.empty)
end MockTransactionSubmissionPort

object MockTransactionSubmissionPort:
    /** Creates a new MockTransactionSubmissionPort with default configuration.
      *
      * @return
      *   ZIO effect creating the mock
      */
    def make: UIO[MockTransactionSubmissionPort] =
        for
            configRef <- Ref.make(SubmissionConfig.default)
            submittedRef <- Ref.make(Map.empty[TransactionId, String])
            invocationsRef <- Ref.make(List.empty[SubmissionInvocation])
        yield MockTransactionSubmissionPort(configRef, submittedRef, invocationsRef)

    /** Creates a preconfigured mock for specific scenarios.
      *
      * @param scenarioName
      *   Name of the predefined scenario to use
      * @return
      *   ZIO effect creating a configured mock
      */
    def forScenario(scenarioName: String): UIO[MockTransactionSubmissionPort] =
        for
            mock <- make
            _ <- scenarioName match
                case "successful-submission" =>
                    mock.setValidateBeforeSubmission(true) *>
                    mock.requireCategory(true)
                case "validation-failure" =>
                    mock.setValidateBeforeSubmission(true) *>
                    mock.requireCategory(true) *>
                    mock.setFailOnValidationError(true)
                case "connection-failure" =>
                    mock.setConnectionError(
                        SubmissionError.ConnectionFailed("Cannot connect to YNAB API")
                    )
                case _ => ZIO.unit
        yield mock

    /** Creates a ZIO layer for the MockTransactionSubmissionPort.
      *
      * @return
      *   ZLayer containing the MockTransactionSubmissionPort as a TransactionSubmissionPort
      */
    val layer: ULayer[TransactionSubmissionPort] =
        ZLayer.scoped {
            ZIO.acquireRelease(
                make.map(provider => provider.asInstanceOf[TransactionSubmissionPort])
            )(_ => ZIO.unit)
        }

    /** Creates a ZIO layer for the MockTransactionSubmissionPort, exposing the mock interfaces.
      *
      * @return
      *   ZLayer containing the MockTransactionSubmissionPort
      */
    val mockLayer: ULayer[MockTransactionSubmissionPort] =
        ZLayer.scoped {
            ZIO.acquireRelease(
                make
            )(_.reset)
        }
end MockTransactionSubmissionPort

/** Configuration for the MockTransactionSubmissionPort. */
case class SubmissionConfig(
    requireCategory: Boolean = true,
    checkMinimumAmount: Boolean = false,
    minimumAmount: BigDecimal = BigDecimal(0),
    minimumDescriptionLength: Int = 1,
    customValidation: Option[(Transaction, TransactionProcessingState) => List[String]] = None,
    validateBeforeSubmission: Boolean = true,
    failOnValidationError: Boolean = false,
    singleSubmissionError: Option[SubmissionError] = None,
    batchSubmissionError: Option[SubmissionError] = None,
    connectionError: Option[SubmissionError] = None
)

object SubmissionConfig:
    /** Default configuration that requires categories but performs minimal validation. */
    val default: SubmissionConfig = SubmissionConfig()
end SubmissionConfig

/** Records method invocations for verification in tests. */
sealed trait SubmissionInvocation

object SubmissionInvocation:
    /** Records a validateForSubmission call */
    case class Validate(
        transaction: Transaction,
        processingState: TransactionProcessingState
    ) extends SubmissionInvocation

    /** Records a submitTransaction call */
    case class SubmitOne(
        transaction: Transaction,
        processingState: TransactionProcessingState
    ) extends SubmissionInvocation

    /** Records a submitTransactions call */
    case class SubmitBatch(
        transactions: List[(Transaction, TransactionProcessingState)]
    ) extends SubmissionInvocation

    /** Records a testConnection call */
    case object TestConnection extends SubmissionInvocation
end SubmissionInvocation