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

object CategorizationServiceImplSpec extends ZIOSpecDefault:
    // Simple test categorization strategy
    val testCategorizationStrategy = new CategorizationStrategy:
        override def categorize(transaction: Transaction): UIO[TransactionCategorization] =
            ZIO.succeed {
                val categoryId = transaction.userIdentification match
                    case Some(id) if id.contains("Grocery")    => Some("groceries")
                    case Some(id) if id.contains("UBER")       => Some("transport")
                    case Some(id) if id.contains("Restaurant") => Some("dining")
                    case _                                     => None

                val confidence = if categoryId.isDefined then Some(ConfidenceScore(0.85)) else None

                TransactionCategorization(
                    transactionId = transaction.id,
                    categoryId = categoryId,
                    payeeName = transaction.userIdentification,
                    memo = transaction.message,
                    confidence = confidence
                )
            }
    end testCategorizationStrategy

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
                states.values
                    .filter { state =>
                        query.sourceAccountId.forall(_ == state.transactionId.sourceAccountId) &&
                        query.status.forall(_ == state.status) &&
                        // Removed minAmount and maxAmount checks as they're not in TransactionProcessingStateQuery
                        // Also need to remove startDate and endDate checks
                        true
                    }
                    .toSeq
            }

        def save(key: TransactionId, value: TransactionProcessingState): UIO[Unit] =
            ZIO.succeed { states = states + (key -> value) }

        def load(id: TransactionId): UIO[Option[TransactionProcessingState]] =
            ZIO.succeed(states.get(id))

        def findReadyToSubmit(): UIO[Seq[TransactionProcessingState]] =
            ZIO.succeed(states.values.filter(_.isReadyForSubmission).toSeq)

        // Initialize states for test transactions
        def init(transactions: Map[TransactionId, Transaction]): UIO[Unit] =
            ZIO.succeed {
                states = transactions.map { case (id, tx) =>
                    id -> TransactionProcessingState.initial(tx)
                }
            }

        // Test helper
        def getAll: UIO[Map[TransactionId, TransactionProcessingState]] = ZIO.succeed(states)

        // Reference to transaction repo for filtering queries
        private var transactions: Map[TransactionId, Transaction] = Map.empty
        def setTransactions(txs: Map[TransactionId, Transaction]): UIO[Unit] =
            ZIO.succeed { transactions = txs }
    end MockProcessingStateRepository

    class MockCategoryRepository extends CategoryRepository:
        private val categories = Map(
            "groceries" -> Category("groceries", "Groceries", None),
            "dining" -> Category("dining", "Dining Out", None),
            "transport" -> Category("transport", "Transportation", None)
        )

        def save(key: String, value: Category): UIO[Unit] = ZIO.unit

        def load(id: String): UIO[Option[Category]] =
            ZIO.succeed(categories.get(id))

        def find[Q](query: Q): UIO[Seq[Category]] =
            ZIO.succeed(categories.values.toSeq)

        // Add the specific find method with the correct FilterArg type
        def find(filter: works.iterative.incubator.budget.domain.query.CategoryQuery)
            : UIO[Seq[Category]] =
            ZIO.succeed(categories.values.toSeq)
    end MockCategoryRepository

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
            _ <- stateRepo.setTransactions(transactions)
            categoryRepo <- ZIO.succeed(new MockCategoryRepository)
            eventCollector <- ZIO.succeed(new EventCollector)
            service = CategorizationServiceImpl(
                txRepo,
                stateRepo,
                categoryRepo,
                testCategorizationStrategy,
                eventCollector.publishEvent
            )
        yield (service, txRepo, stateRepo, categoryRepo, eventCollector, transactions)

    // Tests
    override def spec =
        suite("CategorizationServiceImpl")(
            test("categorizeTransactions should assign categories with confidence scores") {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, eventCollector, transactions) = env

                    // Run categorization
                    result <- service.categorizeTransactions(transactions.keys.toSeq)

                    // Check the results
                    states <- stateRepo.getAll
                    categorizedStates =
                        states.values.filter(_.status == TransactionStatus.Categorized)

                    // Check the events
                    events <- eventCollector.getEvents
                    txCategorizedEvents = events.collect { case e: TransactionCategorized => e }
                    batchEvent = events.collectFirst { case e: TransactionsCategorized => e }
                yield assertTrue(
                    result.categorizedCount == 3, // All 3 transactions should get categorized
                    result.averageConfidence.isDefined,
                    categorizedStates.size == 3,
                    categorizedStates.forall(_.effectiveCategory.isDefined),
                    categorizedStates.forall(_.categoryConfidence.isDefined),
                    txCategorizedEvents.size == 3,
                    batchEvent.isDefined,
                    batchEvent.get.transactionCount == 3
                )
            },
            test("updateCategory should correctly update a transaction's category") {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, eventCollector, transactions) = env
                    _ <- eventCollector.clear

                    // Select a transaction to update
                    transactionId = transactions.keys.head

                    // Update the category
                    result <- service.updateCategory(
                        transactionId = transactionId,
                        categoryId = "dining",
                        memo = Some("Lunch with friends"),
                        payeeName = Some("Restaurant XYZ")
                    )

                    // Check the state was updated
                    updatedState <- stateRepo.load(transactionId)

                    // Check the event was published
                    events <- eventCollector.getEvents
                    categoryUpdatedEvent = events.collectFirst { case e: CategoryUpdated => e }
                yield assertTrue(
                    result.isDefined,
                    updatedState.isDefined,
                    updatedState.get.effectiveCategory.contains("dining"),
                    updatedState.get.effectiveMemo.contains("Lunch with friends"),
                    updatedState.get.effectivePayeeName.contains("Restaurant XYZ"),
                    categoryUpdatedEvent.isDefined,
                    categoryUpdatedEvent.get.transactionId == transactionId,
                    categoryUpdatedEvent.get.newCategory == "dining"
                )
            },
            test("bulkUpdateCategory should update categories for transactions matching criteria") {
                for
                    env <- testEnvironment
                    (service, _, stateRepo, _, eventCollector, transactions) = env
                    _ <- eventCollector.clear

                    // Define a filter for UBER transactions
                    filter = TransactionFilter(
                        descriptionContains = Some("UBER")
                    )

                    // Perform bulk update
                    updatedCount <- service.bulkUpdateCategory(
                        filter = filter,
                        categoryId = "transport",
                        memo = Some("Ride sharing"),
                        payeeName = Some("UBER")
                    )

                    // Check the states were updated
                    states <- stateRepo.getAll
                    updatedStates = states.values.filter(_.effectiveCategory.contains("transport"))

                    // Check the event was published
                    events <- eventCollector.getEvents
                    bulkUpdateEvent = events.collectFirst { case e: BulkCategoryUpdated => e }
                yield assertTrue(
                    updatedCount == 1, // Only one transaction matches UBER
                    updatedStates.size == 1,
                    bulkUpdateEvent.isDefined,
                    bulkUpdateEvent.get.count == 1,
                    bulkUpdateEvent.get.category == "transport"
                )
            },
            test("calculateAverageConfidence should correctly average confidence scores") {
                for
                    env <- testEnvironment
                    (service, _, _, _, _, _) = env

                    // Create test categorizations with varying confidences
                    categorizations = Seq(
                        TransactionCategorization(
                            transactionId = TransactionId(1, "test-1"),
                            categoryId = Some("groceries"),
                            payeeName = Some("Test"),
                            memo = None,
                            confidence = Some(ConfidenceScore(0.7))
                        ),
                        TransactionCategorization(
                            transactionId = TransactionId(1, "test-2"),
                            categoryId = Some("transport"),
                            payeeName = Some("Test"),
                            memo = None,
                            confidence = Some(ConfidenceScore(0.9))
                        )
                    )

                    // Calculate average confidence
                    avgConfidence <- service.calculateAverageConfidence(categorizations)
                yield assertTrue(
                    avgConfidence.isDefined,
                    avgConfidence.get.value == 0.8 // (0.7 + 0.9) / 2 = 0.8
                )
            }
        ) @@ sequential
    end spec
end CategorizationServiceImplSpec
