package works.iterative.incubator.budget.domain.service.impl

import zio.*
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.event.*
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.domain.query.TransactionProcessingStateQuery

/** Implementation of the SubmissionService that contains all business logic for transaction submission.
  *
  * This implementation follows the functional core pattern, containing all business logic for
  * transaction submission while delegating infrastructure concerns to repository interfaces.
  *
  * @param processingStateRepository Repository for storing and retrieving transaction processing states
  * @param transactionRepository Repository for retrieving Transaction entities
  * @param ynabSubmitter The component that handles actual submission to YNAB
  * @param eventPublisher Function to publish domain events
  */
case class SubmissionServiceImpl(
    processingStateRepository: TransactionProcessingStateRepository,
    transactionRepository: TransactionRepository,
    ynabSubmitter: YnabSubmitter,
    eventPublisher: DomainEvent => UIO[Unit]
) extends SubmissionService:
    
    /** Submit a batch of transactions to YNAB
      *
      * This workflow:
      * 1. Validates that transactions are ready for submission
      * 2. Submits ready transactions to YNAB
      * 3. Updates transaction processing states with YNAB IDs
      * 4. Emits events for submitted transactions
      *
      * @param transactionIds The IDs of transactions to submit
      * @return A SubmissionResult with counts of successful and failed submissions
      */
    override def submitTransactions(
        transactionIds: Seq[TransactionId]
    ): UIO[SubmissionResult] =
        for
            // Load all processing states
            statesOpt <- ZIO.foreach(transactionIds)(processingStateRepository.load)
            states = statesOpt.flatten
            
            // Validate states for submission
            validationResult <- validateForSubmission(states)
            
            // Handle validation failures
            _ <- ZIO.when(validationResult.invalidTransactions.nonEmpty) {
                for
                    now <- Clock.instant
                    event = SubmissionFailed(
                        reason = s"Failed to validate ${validationResult.invalidTransactions.size} transactions",
                        transactionCount = validationResult.invalidTransactions.size,
                        occurredAt = now
                    )
                    _ <- eventPublisher(event)
                yield ()
            }
            
            // Submit each valid transaction
            submissionResults <- ZIO.foreach(validationResult.validTransactions) { state =>
                submitTransaction(state.transactionId)
            }
            
            // Calculate success/failure counts
            successCount = submissionResults.count(_.submitted)
            submissionErrors = submissionResults.filter(!_.submitted).flatMap(_.error).toSeq
            
            // Publish events for successful submissions
            _ <- ZIO.when(successCount > 0) {
                for
                    // Get the YNAB account ID from one of the successful submissions
                    now <- Clock.instant
                    ynabAccountId = submissionResults
                        .find(_.submitted)
                        .flatMap(_.ynabTransactionId)
                        .getOrElse("unknown")
                    
                    event = TransactionsSubmitted(
                        count = successCount,
                        ynabAccountId = ynabAccountId,
                        occurredAt = now
                    )
                    _ <- eventPublisher(event)
                yield ()
            }
            
            // Publish failure event if any submissions failed (during the actual submission phase)
            _ <- ZIO.when(submissionErrors.nonEmpty) {
                for
                    now <- Clock.instant
                    event = SubmissionFailed(
                        reason = s"Failed to submit ${submissionErrors.size} transactions",
                        transactionCount = submissionErrors.size,
                        occurredAt = now
                    )
                    _ <- eventPublisher(event)
                yield ()
            }
        yield SubmissionResult(
            submittedCount = successCount,
            failedCount = transactionIds.size - successCount,
            errors = validationResult.invalidTransactions.map { case (state, reason) =>
                SubmissionError(state.transactionId, s"Validation failed: $reason")
            }.toSeq ++ submissionErrors
        )

    /** Submit a single transaction to YNAB
      *
      * @param transactionId The ID of the transaction to submit
      * @return The submission results for the transaction
      */
    override def submitTransaction(
        transactionId: TransactionId
    ): UIO[TransactionSubmissionResult] =
        for
            // Load transaction and processing state
            stateOpt <- processingStateRepository.load(transactionId)
            
            result <- stateOpt match
                case None =>
                    ZIO.succeed(TransactionSubmissionResult(
                        transactionId = transactionId,
                        submitted = false,
                        ynabTransactionId = None,
                        error = Some(SubmissionError(transactionId, "Transaction not found"))
                    ))
                    
                case Some(state) if state.status != TransactionStatus.Categorized =>
                    ZIO.succeed(TransactionSubmissionResult(
                        transactionId = transactionId,
                        submitted = false,
                        ynabTransactionId = None,
                        error = Some(SubmissionError(
                            transactionId, 
                            s"Transaction has invalid status: ${state.status}"
                        ))
                    ))
                    
                case Some(state) if state.effectiveCategory.isEmpty || state.effectivePayeeName.isEmpty =>
                    ZIO.succeed(TransactionSubmissionResult(
                        transactionId = transactionId,
                        submitted = false,
                        ynabTransactionId = None,
                        error = Some(SubmissionError(
                            transactionId,
                            "Transaction is missing required fields (category or payee name)"
                        ))
                    ))
                    
                case Some(state) =>
                    // Load transaction to get details for submission
                    transactionRepository.load(transactionId).flatMap {
                        case None => 
                            ZIO.succeed(TransactionSubmissionResult(
                                transactionId = transactionId,
                                submitted = false,
                                ynabTransactionId = None,
                                error = Some(SubmissionError(
                                    transactionId,
                                    "Transaction entity not found"
                                ))
                            ))
                            
                        case Some(tx) =>
                            // Create submission request for YNAB
                            val submissionRequest = YnabSubmissionRequest(
                                amount = tx.amount,
                                date = tx.date,
                                payeeName = state.effectivePayeeName.get,
                                categoryName = state.effectiveCategory.get,
                                memo = state.effectiveMemo.getOrElse("")
                            )
                            
                            // Submit to YNAB
                            ynabSubmitter.submitTransaction(submissionRequest).flatMap {
                                case Left(error) =>
                                    ZIO.succeed(TransactionSubmissionResult(
                                        transactionId = transactionId,
                                        submitted = false,
                                        ynabTransactionId = None,
                                        error = Some(SubmissionError(transactionId, error))
                                    ))
                                    
                                case Right(response) =>
                                    for
                                        // Update processing state with YNAB IDs
                                        updatedState <- ZIO.attempt {
                                            state.withYnabSubmission(
                                                ynabTransactionId = response.transactionId,
                                                ynabAccountId = response.accountId
                                            )
                                        }.orElseFail(s"Failed to update processing state: ${response.transactionId}")
                                         .catchAll(error => 
                                            ZIO.succeed(TransactionSubmissionResult(
                                                transactionId = transactionId,
                                                submitted = false,
                                                ynabTransactionId = None,
                                                error = Some(SubmissionError(transactionId, error.toString))
                                            ))
                                         )
                                         .flatMap {
                                            case result: TransactionSubmissionResult => ZIO.succeed(result)
                                            case updatedState: TransactionProcessingState => 
                                                for
                                                    // Save the updated state
                                                    _ <- processingStateRepository.save(transactionId, updatedState)
                                                    
                                                    // Publish success event
                                                    now <- Clock.instant
                                                    event = TransactionSubmitted(
                                                        transactionId = transactionId,
                                                        ynabTransactionId = response.transactionId,
                                                        ynabAccountId = response.accountId,
                                                        occurredAt = now
                                                    )
                                                    _ <- eventPublisher(event)
                                                yield TransactionSubmissionResult(
                                                    transactionId = transactionId,
                                                    submitted = true,
                                                    ynabTransactionId = Some(response.transactionId),
                                                    error = None
                                                )
                                        }
                                    yield updatedState
                            }
                    }
        yield result

    /** Check if transactions meet requirements for submission
      *
      * @param transactionStates The processing states to validate
      * @return A ValidationResult with valid and invalid transactions
      */
    override def validateForSubmission(
        transactionStates: Seq[TransactionProcessingState]
    ): UIO[ValidationResult] =
        // Partition into valid and invalid states
        val (valid, invalid) = transactionStates.partition { state =>
            state.status == TransactionStatus.Categorized && 
            state.effectiveCategory.isDefined && 
            state.effectivePayeeName.isDefined &&
            !state.isDuplicate
        }
        
        // Add reasons for invalid states
        val invalidWithReasons = invalid.map { state =>
            val reason = 
                if state.isDuplicate then 
                    "Transaction is marked as duplicate"
                else if state.status != TransactionStatus.Categorized then
                    s"Invalid status: ${state.status}"
                else if state.effectiveCategory.isEmpty then
                    "Missing category"
                else if state.effectivePayeeName.isEmpty then
                    "Missing payee name"
                else
                    "Unknown validation error"
                    
            (state, reason)
        }
        
        ZIO.succeed(ValidationResult(valid, invalidWithReasons))

    /** Get statistics about transaction submission status
      *
      * @param sourceAccountId Optional account ID to filter by
      * @return Statistics about transaction counts by status
      */
    override def getSubmissionStatistics(
        sourceAccountId: Option[Long] = None
    ): UIO[SubmissionStatistics] =
        for
            // Create query based on optional source account
            query <- ZIO.succeed(TransactionProcessingStateQuery(
                sourceAccountId = sourceAccountId
            ))
            
            // Get all states matching the query
            states <- processingStateRepository.find(query)
        yield SubmissionStatistics(
            total = states.size,
            imported = states.count(_.status == TransactionStatus.Imported),
            categorized = states.count(_.status == TransactionStatus.Categorized),
            submitted = states.count(_.status == TransactionStatus.Submitted),
            duplicate = states.count(_.isDuplicate)
        )
end SubmissionServiceImpl

/** Companion object for SubmissionServiceImpl */
object SubmissionServiceImpl:
    /** Create a ZLayer for the SubmissionServiceImpl */
    def layer(
        ynabSubmitter: YnabSubmitter,
        eventPublisher: DomainEvent => UIO[Unit]
    ): URLayer[TransactionProcessingStateRepository & TransactionRepository, SubmissionService] =
        ZLayer {
            for
                processingStateRepository <- ZIO.service[TransactionProcessingStateRepository]
                transactionRepository <- ZIO.service[TransactionRepository]
            yield SubmissionServiceImpl(
                processingStateRepository,
                transactionRepository,
                ynabSubmitter,
                eventPublisher
            )
        }
end SubmissionServiceImpl

/** Request for YNAB transaction submission */
case class YnabSubmissionRequest(
    amount: BigDecimal,
    date: java.time.LocalDate,
    payeeName: String,
    categoryName: String,
    memo: String
)

/** Response from YNAB transaction submission */
case class YnabSubmissionResponse(
    transactionId: String,
    accountId: String
)

/** Interface for the component that submits transactions to YNAB */
trait YnabSubmitter:
    def submitTransaction(request: YnabSubmissionRequest): UIO[Either[String, YnabSubmissionResponse]]
end YnabSubmitter