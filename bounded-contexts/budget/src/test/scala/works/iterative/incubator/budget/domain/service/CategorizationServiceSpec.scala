package works.iterative.incubator.budget.domain.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.time.{LocalDate, Instant}
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.event.{TransactionCategorized, TransactionsCategorized, CategoryUpdated, BulkCategoryUpdated}

object CategorizationServiceSpec extends ZIOSpecDefault:
    // Test implementation of the CategorizationService
    private class TestCategorizationService extends CategorizationService:
        private var transactions: Map[TransactionId, Transaction] = Map.empty
        private var processingStates: Map[TransactionId, TransactionProcessingState] = Map.empty
        private var categories: Map[String, Category] = Map(
            "groceries" -> Category("groceries", "Groceries", None),
            "dining" -> Category("dining", "Dining Out", None),
            "transport" -> Category("transport", "Transportation", None)
        )
        private var events: List[Any] = List.empty

        // Initialize with some test transactions
        def init: UIO[Unit] = ZIO.succeed {
            val sourceAccountId = 1L
            
            // Create some test transactions
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
            
            processingStates = Map(
                tx1Id -> TransactionProcessingState.initial(tx1),
                tx2Id -> TransactionProcessingState.initial(tx2),
                tx3Id -> TransactionProcessingState.initial(tx3)
            )
        }

        def categorizeTransactions(
            transactionIds: Seq[TransactionId]
        ): UIO[CategorizationResult] =
            for
                categorizations <- ZIO.foreach(transactionIds) { id => 
                    categorizeTransaction(id)
                }
                validCats = categorizations.flatten
                avgConfidence <- calculateAverageConfidence(validCats)
                _ <- ZIO.succeed {
                    events = TransactionsCategorized(
                        transactionCount = validCats.size,
                        sourceAccountId = 1L, // Using a fixed source account ID for the test
                        averageConfidence = avgConfidence.getOrElse(ConfidenceScore(0.0)),
                        occurredAt = Instant.now()
                    ) :: events
                }
            yield CategorizationResult(
                categorizedCount = validCats.size,
                failedCount = transactionIds.size - validCats.size,
                averageConfidence = avgConfidence
            )

        def categorizeTransaction(
            transactionId: TransactionId
        ): UIO[Option[TransactionCategorization]] =
            ZIO.succeed(processingStates.get(transactionId)).flatMap {
                case None => ZIO.succeed(None)
                case Some(state) =>
                    for
                        transaction <- ZIO.succeed(transactions.getOrElse(
                            transactionId,
                            throw new IllegalStateException(s"Transaction $transactionId not found")
                        ))
                        catResult = transaction.userIdentification match
                            case Some(id) if id.contains("Grocery") => 
                                TransactionCategorization(
                                    transactionId = transactionId,
                                    categoryId = Some("groceries"),
                                    payeeName = Some("Grocery Store"),
                                    memo = transaction.message,
                                    confidence = Some(ConfidenceScore(0.9))
                                )
                            case Some(id) if id.contains("UBER") => 
                                TransactionCategorization(
                                    transactionId = transactionId,
                                    categoryId = Some("transport"),
                                    payeeName = Some("Uber"),
                                    memo = transaction.message,
                                    confidence = Some(ConfidenceScore(0.85))
                                )
                            case Some(id) if id.contains("Restaurant") => 
                                TransactionCategorization(
                                    transactionId = transactionId,
                                    categoryId = Some("dining"),
                                    payeeName = Some("Restaurant"),
                                    memo = transaction.message,
                                    confidence = Some(ConfidenceScore(0.95))
                                )
                            case _ => 
                                TransactionCategorization(
                                    transactionId = transactionId,
                                    categoryId = None,
                                    payeeName = None,
                                    memo = transaction.message,
                                    confidence = None
                                )
                        _ <- 
                            if catResult.categoryId.isDefined then
                                ZIO.succeed {
                                    val updatedState = state.withAICategorization(
                                        payeeName = catResult.payeeName,
                                        category = catResult.categoryId,
                                        memo = catResult.memo,
                                        categoryConfidence = catResult.confidence,
                                        payeeConfidence = Some(ConfidenceScore(0.8))
                                    )
                                    processingStates = processingStates + (transactionId -> updatedState)
                                    
                                    events = TransactionCategorized(
                                        transactionId = transactionId,
                                        category = catResult.categoryId.get,
                                        payeeName = catResult.payeeName.getOrElse("Unknown"),
                                        byAI = true,
                                        occurredAt = Instant.now()
                                    ) :: events
                                }
                            else ZIO.unit
                    yield 
                        if catResult.categoryId.isDefined then Some(catResult) else None
            }

        def updateCategory(
            transactionId: TransactionId,
            categoryId: String,
            memo: Option[String] = None,
            payeeName: Option[String] = None
        ): UIO[Option[TransactionProcessingState]] =
            ZIO.succeed(processingStates.get(transactionId)).flatMap {
                case None => ZIO.succeed(None)
                case Some(state) =>
                    ZIO.succeed {
                        val updatedState = state.withUserOverrides(
                            payeeName = payeeName,
                            category = Some(categoryId),
                            memo = memo
                        )
                        processingStates = processingStates + (transactionId -> updatedState)
                        
                        events = CategoryUpdated(
                            transactionId = transactionId,
                            oldCategory = state.effectiveCategory,
                            newCategory = categoryId,
                            occurredAt = Instant.now()
                        ) :: events
                        
                        Some(updatedState)
                    }
            }

        def bulkUpdateCategory(
            filter: TransactionFilter,
            categoryId: String,
            memo: Option[String] = None,
            payeeName: Option[String] = None
        ): UIO[Int] =
            for
                matchingIds <- ZIO.succeed {
                    transactions.filter { case (id, tx) =>
                        val sourceAccountMatch = filter.sourceAccountId.forall(_ == id.sourceAccountId)
                        val descriptionMatch = filter.descriptionContains.forall(pattern => 
                            tx.message.exists(_.contains(pattern)) || 
                            tx.userIdentification.exists(_.contains(pattern))
                        )
                        val counterPartyMatch = filter.counterPartyContains.forall(pattern => 
                            tx.counterAccount.exists(_.contains(pattern)) || 
                            tx.counterBankName.exists(_.contains(pattern))
                        )
                        val transactionTypeMatch = filter.transactionType.forall(_ == tx.transactionType)
                        
                        sourceAccountMatch && descriptionMatch && counterPartyMatch && transactionTypeMatch
                    }.keys.toSeq
                }
                updates <- ZIO.foreach(matchingIds)(id => updateCategory(id, categoryId, memo, payeeName))
                updatedCount = updates.flatten.size
                _ <- ZIO.succeed {
                    if updatedCount > 0 then
                        events = BulkCategoryUpdated(
                            count = updatedCount,
                            category = categoryId,
                            filterCriteria = s"Filter: ${filter.toString}",
                            occurredAt = Instant.now()
                        ) :: events
                }
            yield updatedCount

        def calculateAverageConfidence(
            categorizations: Seq[TransactionCategorization]
        ): UIO[Option[ConfidenceScore]] =
            ZIO.succeed {
                val confidences = categorizations.flatMap(_.confidence.map(_.value))
                if confidences.isEmpty then None
                else Some(ConfidenceScore(confidences.sum / confidences.size))
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
    end TestCategorizationService

    // Test layer
    private val testLayer = ZLayer.succeed(new TestCategorizationService)

    // Accessor for test operations
    private def testService = ZIO.service[TestCategorizationService]

    // Tests
    override def spec =
        suite("CategorizationService")(
            test("categorizeTransactions should assign categories with confidence scores") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    transactions <- testService.flatMap(_.getTransactions)
                    result <- CategorizationService.categorizeTransactions(transactions.keys.toSeq)
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    categorizedStates = processingStates.values.filter(_.status == TransactionStatus.Categorized)
                yield
                    assertTrue(
                        result.categorizedCount == 3,
                        result.failedCount == 0,
                        result.averageConfidence.isDefined,
                        categorizedStates.size == 3,
                        categorizedStates.forall(_.effectiveCategory.isDefined),
                        categorizedStates.forall(_.categoryConfidence.isDefined)
                    )
            },
            
            test("updateCategory should correctly update a transaction's category") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    transactions <- testService.flatMap(_.getTransactions)
                    transactionId = transactions.keys.head
                    result <- CategorizationService.updateCategory(
                        transactionId = transactionId,
                        categoryId = "dining",
                        memo = Some("Lunch with friends"),
                        payeeName = Some("Restaurant XYZ")
                    )
                    events <- testService.flatMap(_.getEvents)
                    categoryUpdatedEvents = events.collect { case e: CategoryUpdated => e }
                yield
                    assertTrue(
                        result.isDefined,
                        result.get.effectiveCategory.contains("dining"),
                        result.get.effectiveMemo.contains("Lunch with friends"),
                        result.get.effectivePayeeName.contains("Restaurant XYZ"),
                        categoryUpdatedEvents.nonEmpty,
                        categoryUpdatedEvents.head.transactionId == transactionId,
                        categoryUpdatedEvents.head.newCategory == "dining"
                    )
            },
            
            test("bulkUpdateCategory should update categories for transactions matching criteria") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    updatedCount <- CategorizationService.bulkUpdateCategory(
                        filter = TransactionFilter(
                            descriptionContains = Some("UBER")
                        ),
                        categoryId = "transport",
                        memo = Some("Ride sharing"),
                        payeeName = Some("UBER")
                    )
                    processingStates <- testService.flatMap(_.getProcessingStates)
                    updatedStates = processingStates.values.filter(_.effectiveCategory.contains("transport"))
                    events <- testService.flatMap(_.getEvents)
                    bulkUpdateEvents = events.collect { case e: BulkCategoryUpdated => e }
                yield
                    assertTrue(
                        updatedCount > 0,
                        updatedStates.size == updatedCount,
                        bulkUpdateEvents.nonEmpty,
                        bulkUpdateEvents.head.count == updatedCount,
                        bulkUpdateEvents.head.category == "transport"
                    )
            },
            
            test("TransactionsCategorized event should be published after categorization") {
                for
                    _ <- testService.flatMap(_.reset)
                    _ <- testService.flatMap(_.init)
                    transactions <- testService.flatMap(_.getTransactions)
                    _ <- CategorizationService.categorizeTransactions(transactions.keys.toSeq)
                    events <- testService.flatMap(_.getEvents)
                    categorizedEvents = events.collect { case e: TransactionsCategorized => e }
                yield
                    assertTrue(
                        categorizedEvents.nonEmpty,
                        categorizedEvents.head.transactionCount == transactions.size,
                        categorizedEvents.head.sourceAccountId == 1L
                    )
            }
        ).provideLayer(testLayer) @@ sequential
    end spec
end CategorizationServiceSpec