package works.iterative.incubator.budget.domain.service.impl

import zio.*
import zio.test.*
import zio.test.TestAspect.*
import java.time.{LocalDate, Instant}
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.event.*
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.domain.query.TransactionProcessingStateQuery

object SubmissionServiceImplSpec extends ZIOSpecDefault:
    // Test YNAB submitter implementation
    class TestYnabSubmitter extends YnabSubmitter:
        def submitTransaction(request: YnabSubmissionRequest)
            : UIO[Either[String, YnabSubmissionResponse]] =
            // Simple implementation that always succeeds, generating IDs based on request
            ZIO.succeed {
                val accountId = "ynab-account-1"
                val transactionId =
                    s"ynab-tx-${Math.abs(request.payeeName.hashCode()).toString.take(8)}"

                Right(YnabSubmissionResponse(
                    transactionId = transactionId,
                    accountId = accountId
                ))
            }
    end TestYnabSubmitter

    // Mock repositories
    class MockTransactionRepository extends TransactionRepository:
        private var transactions = Map.empty[TransactionId, Transaction]

        def save(key: TransactionId, value: Transaction): UIO[Unit] =
            ZIO.succeed { transactions = transactions + (key -> value) }

        def load(id: TransactionId): UIO[Option[Transaction]] =
            ZIO.succeed(transactions.get(id))

        def find[Q](query: Q): UIO[Seq[Transaction]] =
            ZIO.succeed(transactions.values.toSeq)

        // Add the specific find method with the correct FilterArg type
        def find(filter: works.iterative.incubator.budget.domain.query.TransactionQuery)
            : UIO[Seq[Transaction]] =
            ZIO.succeed(transactions.values.toSeq)

        // Initialize with test data
        def init: UIO[Unit] = ZIO.succeed {
            val sourceAccountId = 1L

            val tx1 = Transaction(
                id = TransactionId(sourceAccountId, "tx-1"),
                date = LocalDate.of(2023, 1, 15),
                amount = BigDecimal("100.50"),
                currency = "CZK",
                counterAccount = Some("123456789"),
                counterBankCode = Some("0800"),
                counterBankName = Some("Grocery Store Bank"),
                variableSymbol = Some("12345"),
                constantSymbol = None,
                specificSymbol = None,
                userIdentification = Some("Grocery Store"),
                message = Some("Weekly shopping"),
                transactionType = "PAYMENT",
                comment = None,
                importedAt = Instant.now()
            )

            val tx2 = Transaction(
                id = TransactionId(sourceAccountId, "tx-2"),
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

            val tx3 = Transaction(
                id = TransactionId(sourceAccountId, "tx-3"),
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
                tx1.id -> tx1,
                tx2.id -> tx2,
                tx3.id -> tx3
            )
        }

        // Test helper
        def getAll: UIO[Map[TransactionId, Transaction]] = ZIO.succeed(transactions)
    end MockTransactionRepository

    class MockProcessingStateRepository extends TransactionProcessingStateRepository:
        private var states = Map.empty[TransactionId, TransactionProcessingState]

        def find(query: TransactionProcessingStateQuery): UIO[Seq[TransactionProcessingState]] =
            ZIO.succeed {
                states.values.filter { state =>
                    query.sourceAccountId.forall(_ == state.transactionId.sourceAccountId) &&
                    query.status.forall(_ == state.status)
                }.toSeq
            }

        def save(key: TransactionId, value: TransactionProcessingState): UIO[Unit] =
            ZIO.succeed { states = states + (key -> value) }

        def load(id: TransactionId): UIO[Option[TransactionProcessingState]] =
            ZIO.succeed(states.get(id))

        def findReadyToSubmit(): UIO[Seq[TransactionProcessingState]] =
            ZIO.succeed(states.values.filter(_.isReadyForSubmission).toSeq)

        // Initialize with different states
        def init(transactions: Map[TransactionId, Transaction]): UIO[Unit] =
            ZIO.succeed {
                // Create different states for different transactions
                val entries = transactions.toList.zipWithIndex.map { case ((id, tx), idx) =>
                    if idx == 0
                    then // First transaction is already categorized and ready for submission
                        id -> TransactionProcessingState.initial(tx).withAICategorization(
                            payeeName = Some("Grocery Store"),
                            category = Some("groceries"),
                            memo = Some("Weekly shopping"),
                            categoryConfidence = Some(ConfidenceScore(0.9))
                        )
                    else if idx == 1 then // Second transaction is categorized but missing a payee
                        id -> TransactionProcessingState.initial(tx).withAICategorization(
                            payeeName = None,
                            category = Some("transport"),
                            memo = Some("Ride 123"),
                            categoryConfidence = Some(ConfidenceScore(0.85))
                        )
                    else // Third transaction is still in Imported state
                        id -> TransactionProcessingState.initial(tx)
                }
                states = entries.toMap
            }

        // Test helper
        def getAll: UIO[Map[TransactionId, TransactionProcessingState]] = ZIO.succeed(states)
    end MockProcessingStateRepository

    // Event collector for testing
    class EventCollector:
        private var events = List.empty[DomainEvent]

        def publishEvent(event: DomainEvent): UIO[Unit] =
            ZIO.succeed { events = event :: events }

        def getEvents: UIO[List[DomainEvent]] = ZIO.succeed(events)

        def clear: UIO[Unit] = ZIO.succeed { events = List.empty }
    end EventCollector

    // Setup test environment
    def testEnvironment =
        for
            txRepo <- ZIO.succeed(new MockTransactionRepository)
            _ <- txRepo.init
            transactions <- txRepo.getAll
            stateRepo <- ZIO.succeed(new MockProcessingStateRepository)
            _ <- stateRepo.init(transactions)
            ynabSubmitter <- ZIO.succeed(new TestYnabSubmitter)
            eventCollector <- ZIO.succeed(new EventCollector)
            service = SubmissionServiceImpl(
                stateRepo,
                txRepo,
                ynabSubmitter,
                eventCollector.publishEvent
            )
        yield (service, txRepo, stateRepo, ynabSubmitter, eventCollector, transactions)

    // Tests
    override def spec =
        suite("SubmissionServiceImpl")(
            test(
                "submitTransactions should process valid transactions and mark them as Submitted"
            ) {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, eventCollector, transactions) = env
                    _ <- eventCollector.clear

                    // Submit all transactions
                    result <- service.submitTransactions(transactions.keys.toSeq)

                    // Check the results
                    states <- stateRepo.getAll
                    submittedStates = states.values.filter(_.status == TransactionStatus.Submitted)

                    // Check the events
                    events <- eventCollector.getEvents
                    submittedEvents = events.collect { case e: TransactionSubmitted => e }
                    batchEvent = events.collectFirst { case e: TransactionsSubmitted => e }
                    failedEvent = events.collectFirst { case e: SubmissionFailed => e }
                yield assertTrue(
                    result.submittedCount == 1, // Only one transaction is fully ready
                    result.failedCount == 2, // Two transactions fail validation
                    submittedStates.size == 1, // One transaction should be Submitted
                    submittedStates.forall(_.ynabTransactionId.isDefined),
                    submittedEvents.size == 1, // One TransactionSubmitted event
                    batchEvent.isDefined, // One batch event
                    failedEvent.isDefined // One failure event
                )
            },
            test("validateForSubmission should identify transactions not ready for submission") {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, _, transactions) = env

                    // Load states for validation
                    statesOpt <- ZIO.foreach(transactions.keys)(stateRepo.load)
                    states = statesOpt.flatten

                    // Validate states
                    result = service.validateForSubmission(states.toSeq)
                yield assertTrue(
                    result.validTransactions.size == 1, // Only one transaction is ready
                    result.invalidTransactions.size == 2, // Two transactions are not ready
                    result.validTransactions.head.effectiveCategory.isDefined,
                    result.validTransactions.head.effectivePayeeName.isDefined,
                    result.validTransactions.head.status == TransactionStatus.Categorized
                )
            },
            test("submitTransaction should handle a successful submission") {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, eventCollector, transactions) = env
                    _ <- eventCollector.clear

                    // Find the categorized transaction
                    states <- stateRepo.getAll
                    readyTxId = states.values.find(s =>
                        s.status == TransactionStatus.Categorized &&
                            s.effectiveCategory.isDefined &&
                            s.effectivePayeeName.isDefined
                    ).map(_.transactionId).get

                    // Submit the transaction
                    result <- service.submitTransaction(readyTxId)

                    // Check the updated state
                    updatedState <- stateRepo.load(readyTxId)

                    // Check the event
                    events <- eventCollector.getEvents
                    submittedEvent = events.collectFirst { case e: TransactionSubmitted => e }
                yield assertTrue(
                    result.submitted,
                    result.ynabTransactionId.isDefined,
                    result.error.isEmpty,
                    updatedState.isDefined,
                    updatedState.get.status == TransactionStatus.Submitted,
                    updatedState.get.ynabTransactionId.isDefined,
                    submittedEvent.isDefined,
                    submittedEvent.get.transactionId == readyTxId
                )
            },
            test("submitTransaction should fail for transactions missing required fields") {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, eventCollector, transactions) = env
                    _ <- eventCollector.clear

                    // Find the transaction missing a payee
                    states <- stateRepo.getAll
                    incompleteId = states.values.find(s =>
                        s.status == TransactionStatus.Categorized &&
                            s.effectivePayeeName.isEmpty
                    ).map(_.transactionId).get

                    // Attempt to submit
                    result <- service.submitTransaction(incompleteId)
                yield assertTrue(
                    !result.submitted,
                    result.error.isDefined,
                    result.error.get.reason.contains("missing required fields")
                )
            },
            test("getSubmissionStatistics should report correct counts by status") {
                for
                    env <- testEnvironment
                    (service, _, _, _, _, _) = env

                    // Get statistics before any submissions
                    stats <- service.getSubmissionStatistics()
                yield assertTrue(
                    stats.total == 3,
                    stats.imported == 1, // One transaction is still in Imported state
                    stats.categorized == 2, // Two transactions are in Categorized state
                    stats.submitted == 0 // No transactions have been submitted yet
                )
            }
        ) @@ sequential
    end spec
end SubmissionServiceImplSpec
