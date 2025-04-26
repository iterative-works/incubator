package works.iterative.incubator.budget.domain.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.time.{LocalDate, Instant}
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.event.{TransactionSubmitted, TransactionsSubmitted, SubmissionFailed}

object SubmissionServiceSpec extends ZIOSpecDefault:
    // Test implementation of the SubmissionService
    private class TestSubmissionService extends SubmissionService:
        private var transactions: Map[TransactionId, Transaction] = Map.empty
        private var processingStates: Map[TransactionId, TransactionProcessingState] = Map.empty
        private var events: List[Any] = List.empty

        // Initialize with some test transactions
        def init: UIO[Unit] = ZIO.succeed {
            val sourceAccountId = 1L
            
            // Create some test transactions with different states
            val tx1Id = TransactionId(sourceAccountId, "tx-1")
            val tx1 = Transaction(
                id = tx1Id,
                date = LocalDate.of(2023, 1, 15),
                amount = BigDecimal("100.50"),
                currency = "CZK",
                counterAccount = Some("123456789"),
                counterBankCode = Some("0800"),
                counterBankName = Some("Česká spořitelna"),
                variableSymbol = Some("12345"),
                constantSymbol = None,
                specificSymbol = None,
                userIdentification = Some("Grocery Store"),
                message = Some("Weekly shopping"),
                transactionType = "PAYMENT",
                comment = None,
                importedAt = Instant.now()
            )
            
            val tx2Id = TransactionId(sourceAccountId, "tx-2")
            val tx2 = Transaction(
                id = tx2Id,
                date = LocalDate.of(2023, 1, 16),
                amount = BigDecimal("250.75"),
                currency = "CZK",
                counterAccount = Some("987654321"),
                counterBankCode = Some("0300"),
                counterBankName = Some("ČSOB"),
                userIdentification = Some("UBER"),
                message = Some("Ride 123"),
                variableSymbol = None,
                constantSymbol = None,
                specificSymbol = None,
                transactionType = "PAYMENT",
                comment = None,
                importedAt = Instant.now()
            )
            
            val tx3Id = TransactionId(sourceAccountId, "tx-3")
            val tx3 = Transaction(
                id = tx3Id,
                date = LocalDate.of(2023, 1, 17),
                amount = BigDecimal("520.00"),
                currency = "CZK",
                counterAccount = Some("555555555"),
                counterBankCode = Some("0100"),
                counterBankName = Some("KB"),
                userIdentification = Some("Restaurant ABC"),
                message = Some("Dinner"),
                variableSymbol = None,
                constantSymbol = None,
                specificSymbol = None,
                transactionType = "PAYMENT",
                comment = None,
                importedAt = Instant.now()
            )
            
            transactions = Map(
                tx1Id -> tx1,
                tx2Id -> tx2,
                tx3Id -> tx3
            )
            
            // Create processing states with different statuses
            val tx1State = TransactionProcessingState.initial(tx1)
                .withAICategorization(
                    payeeName = Some("Grocery Store"),
                    category = Some("groceries"),
                    memo = Some("Weekly shopping"),
                    categoryConfidence = Some(ConfidenceScore(0.9))
                )
                
            val tx2State = TransactionProcessingState.initial(tx2)
                .withAICategorization(
                    payeeName = Some("UBER"),
                    category = Some("transport"),
                    memo = Some("Ride 123"),
                    categoryConfidence = Some(ConfidenceScore(0.85))
                )
                
            // This one remains as Imported (not categorized)
            val tx3State = TransactionProcessingState.initial(tx3)
            
            processingStates = Map(
                tx1Id -> tx1State,
                tx2Id -> tx2State,
                tx3Id -> tx3State
            )
        }

        def submitTransactions(
            transactionIds: Seq[TransactionId]
        ): UIO[SubmissionResult] =
            for
                states <- ZIO.foreach(transactionIds)(id => ZIO.succeed(processingStates.get(id))).map(_.flatten)
                validationResult <- validateForSubmission(states)
                submissionResults <- ZIO.foreach(validationResult.validTransactions)(state => 
                    submitTransaction(state.transactionId)
                )
                successCount = submissionResults.count(_.submitted)
                errors = submissionResults.filter(!_.submitted).flatMap(_.error)
                
                _ <- ZIO.when(successCount > 0) {
                    ZIO.succeed {
                        events = TransactionsSubmitted(
                            count = successCount,
                            ynabAccountId = "ynab-account-1", // Using a fixed YNAB account ID for tests
                            occurredAt = Instant.now()
                        ) :: events
                    }
                }
                
                _ <- ZIO.when(errors.nonEmpty) {
                    ZIO.succeed {
                        events = SubmissionFailed(
                            reason = s"Failed to submit ${errors.size} transactions",
                            transactionCount = errors.size,
                            occurredAt = Instant.now()
                        ) :: events
                    }
                }
            yield SubmissionResult(
                submittedCount = successCount,
                failedCount = transactionIds.size - successCount,
                errors = errors.toSeq
            )

        def submitTransaction(
            transactionId: TransactionId
        ): UIO[TransactionSubmissionResult] =
            ZIO.succeed(processingStates.get(transactionId)).flatMap {
                case None => 
                    ZIO.succeed(
                        TransactionSubmissionResult(
                            transactionId = transactionId,
                            submitted = false,
                            ynabTransactionId = None,
                            error = Some(SubmissionError(transactionId, "Transaction not found"))
                        )
                    )
                case Some(state) if state.status != TransactionStatus.Categorized =>
                    ZIO.succeed(
                        TransactionSubmissionResult(
                            transactionId = transactionId,
                            submitted = false,
                            ynabTransactionId = None,
                            error = Some(SubmissionError(transactionId, s"Transaction has invalid status: ${state.status}"))
                        )
                    )
                case Some(state) if state.effectiveCategory.isEmpty || state.effectivePayeeName.isEmpty =>
                    ZIO.succeed(
                        TransactionSubmissionResult(
                            transactionId = transactionId,
                            submitted = false,
                            ynabTransactionId = None,
                            error = Some(SubmissionError(
                                transactionId, 
                                "Transaction is missing required fields (category or payee name)"
                            ))
                        )
                    )
                case Some(state) =>
                    // Simulate successful submission to YNAB
                    ZIO.succeed {
                        val ynabTransactionId = s"ynab-${transactionId.sourceAccountId}-${transactionId.transactionId}"
                        val ynabAccountId = s"ynab-account-${transactionId.sourceAccountId}"
                        
                        try
                            val updatedState = state.withYnabSubmission(
                                ynabTransactionId = ynabTransactionId,
                                ynabAccountId = ynabAccountId
                            )
                            
                            processingStates = processingStates + (transactionId -> updatedState)
                            
                            events = TransactionSubmitted(
                                transactionId = transactionId,
                                ynabTransactionId = ynabTransactionId,
                                ynabAccountId = ynabAccountId,
                                occurredAt = Instant.now()
                            ) :: events
                            
                            TransactionSubmissionResult(
                                transactionId = transactionId,
                                submitted = true,
                                ynabTransactionId = Some(ynabTransactionId),
                                error = None
                            )
                        catch
                            case e: IllegalStateException =>
                                TransactionSubmissionResult(
                                    transactionId = transactionId,
                                    submitted = false,
                                    ynabTransactionId = None,
                                    error = Some(SubmissionError(transactionId, e.getMessage))
                                )
                    }
            }

        def validateForSubmission(
            transactionStates: Seq[TransactionProcessingState]
        ): UIO[ValidationResult] =
            ZIO.succeed {
                val (valid, invalid) = transactionStates.partition { state =>
                    state.status == TransactionStatus.Categorized && 
                    state.effectiveCategory.isDefined && 
                    state.effectivePayeeName.isDefined
                }
                
                val invalidWithReasons = invalid.map { state =>
                    val reason = 
                        if state.status != TransactionStatus.Categorized then 
                            s"Invalid status: ${state.status}"
                        else if state.effectiveCategory.isEmpty then
                            "Missing category"
                        else if state.effectivePayeeName.isEmpty then 
                            "Missing payee name"
                        else 
                            "Unknown validation error"
                            
                    (state, reason)
                }
                
                ValidationResult(valid, invalidWithReasons)
            }

        def getSubmissionStatistics(
            sourceAccountId: Option[Long] = None
        ): UIO[SubmissionStatistics] =
            ZIO.succeed {
                val relevantStates = sourceAccountId match
                    case Some(id) => processingStates.values.filter(_.transactionId.sourceAccountId == id)
                    case None => processingStates.values
                    
                SubmissionStatistics(
                    total = relevantStates.size,
                    imported = relevantStates.count(_.status == TransactionStatus.Imported),
                    categorized = relevantStates.count(_.status == TransactionStatus.Categorized),
                    submitted = relevantStates.count(_.status == TransactionStatus.Submitted),
                    duplicate = relevantStates.count(_.isDuplicate)
                )
            }

        // Test helpers
        def getTransactions: UIO[Map[TransactionId, Transaction]] = ZIO.succeed(transactions)
        def getProcessingStates: UIO[Map[TransactionId, TransactionProcessingState]] = ZIO.succeed(processingStates)
        def getEvents: UIO[List[Any]] = ZIO.succeed(events)
        def reset: UIO[Unit] = ZIO.succeed {
            transactions = Map.empty
            processingStates = Map.empty
            events = List.empty
        }
    end TestSubmissionService

    // Test layer
    private val testLayer = ZLayer.succeed(new TestSubmissionService)

    // Accessor for test operations
    private def testService = ZIO.service[TestSubmissionService]

    // Tests
    override def spec =
        suite("SubmissionService")(
            test("submitTransactions should mark transactions as Submitted") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    transactions <- testService.flatMap(_.getTransactions)
                    // Submit all transactions
                    result <- SubmissionService.submitTransactions(transactions.keys.toSeq)
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    submittedStates = processingStates.values.filter(_.status == TransactionStatus.Submitted)
                yield
                    assertTrue(
                        result.submittedCount == 2, // Only categorized transactions should be submitted
                        result.failedCount == 1,    // One transaction was not categorized
                        submittedStates.size == 2,  
                        submittedStates.forall(_.ynabTransactionId.isDefined)
                    )
            },
            
            test("validateForSubmission should identify transactions not ready for submission") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    validationResult <- SubmissionService.validateForSubmission(processingStates.values.toSeq)
                yield
                    assertTrue(
                        validationResult.validTransactions.size == 2,   // Two transactions are categorized and ready
                        validationResult.invalidTransactions.size == 1  // One is still in Imported state
                    )
            },
            
            test("submission should fail for transactions with missing category") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    transactions <- testService.flatMap(_.getTransactions)
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    // Get the third transaction that's not categorized
                    txId = transactions.keys.find(id => processingStates.get(id).exists(_.status == TransactionStatus.Imported)).get
                    result <- SubmissionService.submitTransaction(txId)
                    events <- testService.flatMap(_.getEvents)
                    failedEvents = events.collect { case e: SubmissionFailed => e }
                yield
                    assertTrue(
                        !result.submitted,
                        result.error.isDefined,
                        result.ynabTransactionId.isEmpty
                    )
            },
            
            test("TransactionsSubmitted event should be published after successful submission") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    transactions <- testService.flatMap(_.getTransactions)
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    categorizedIds = transactions.keys.filter(id => 
                        processingStates.get(id).exists(_.status == TransactionStatus.Categorized)
                    )
                    _ <- SubmissionService.submitTransactions(categorizedIds.toSeq)
                    events <- testService.flatMap(_.getEvents)
                    submittedEvents = events.collect { case e: TransactionsSubmitted => e }
                yield
                    assertTrue(
                        submittedEvents.nonEmpty,
                        submittedEvents.head.count == categorizedIds.size
                    )
            },
            
            test("getSubmissionStatistics should report correct counts by status") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    // Submit the categorized transactions
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    categorizedIds = processingStates.values.filter(_.status == TransactionStatus.Categorized)
                        .map(_.transactionId).toSeq
                    _ <- SubmissionService.submitTransactions(categorizedIds)
                    // Get statistics
                    stats <- SubmissionService.getSubmissionStatistics()
                yield
                    assertTrue(
                        stats.total == 3,
                        stats.imported == 1,
                        stats.categorized == 0, // All categorized transactions are now submitted
                        stats.submitted == 2    
                    )
            }
        ).provideLayer(testLayer) @@ sequential
    end spec
end SubmissionServiceSpec