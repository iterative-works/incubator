package works.iterative.incubator.budget.domain.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import java.time.{LocalDate, Instant}
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.event.{TransactionImported, ImportCompleted, DuplicateTransactionDetected}

object ImportServiceSpec extends ZIOSpecDefault:
    // Test implementation of the ImportService
    private class TestImportService extends ImportService:
        private var transactions: Map[TransactionId, Transaction] = Map.empty
        private var processingStates: Map[TransactionId, TransactionProcessingState] = Map.empty
        private var events: List[Any] = List.empty

        def importTransactions(sourceAccountId: Long, rawTransactions: Seq[RawTransaction]): UIO[(Int, Seq[String])] =
            for
                results <- ZIO.foreach(rawTransactions) { raw => 
                    importTransaction(raw, sourceAccountId)
                }
                importedTransactions = results.flatten
                importedCount = importedTransactions.size
                duplicateIds = rawTransactions.map(_.externalId).diff(importedTransactions.map(_.id.transactionId))
                _ <- ZIO.succeed {
                    events = ImportCompleted(
                        sourceAccountId = sourceAccountId,
                        count = importedCount,
                        occurredAt = Instant.now()
                    ) :: events
                }
            yield (importedCount, duplicateIds)

        def checkForDuplicate(transactionId: TransactionId): UIO[Boolean] =
            ZIO.succeed(transactions.contains(transactionId))

        def importTransaction(rawTransaction: RawTransaction, sourceAccountId: Long): UIO[Option[Transaction]] =
            for
                transactionId <- ZIO.succeed(TransactionId(sourceAccountId, rawTransaction.externalId))
                isDuplicate <- checkForDuplicate(transactionId)
                result <- if isDuplicate then
                    ZIO.succeed {
                        events = DuplicateTransactionDetected(
                            externalId = rawTransaction.externalId,
                            sourceAccountId = sourceAccountId,
                            existingTransactionId = transactionId,
                            occurredAt = Instant.now()
                        ) :: events
                        None
                    }
                else
                    ZIO.succeed {
                        val transaction = Transaction(
                            id = transactionId,
                            date = rawTransaction.date,
                            amount = rawTransaction.amount,
                            currency = rawTransaction.currency,
                            counterAccount = rawTransaction.counterAccount,
                            counterBankCode = rawTransaction.counterBankCode,
                            counterBankName = rawTransaction.counterBankName,
                            variableSymbol = rawTransaction.variableSymbol,
                            constantSymbol = rawTransaction.constantSymbol,
                            specificSymbol = rawTransaction.specificSymbol,
                            userIdentification = rawTransaction.userIdentification,
                            message = rawTransaction.message,
                            transactionType = rawTransaction.transactionType,
                            comment = rawTransaction.comment,
                            importedAt = Instant.now()
                        )
                        
                        transactions = transactions + (transactionId -> transaction)
                        val state = TransactionProcessingState.initial(transaction)
                        processingStates = processingStates + (transactionId -> state)
                        
                        events = TransactionImported(
                            transactionId = transactionId,
                            sourceAccountId = sourceAccountId,
                            date = transaction.date,
                            amount = transaction.amount,
                            currency = transaction.currency,
                            occurredAt = Instant.now()
                        ) :: events
                        
                        Some(transaction)
                    }
            yield result

        def createImportCompletedEvent(sourceAccountId: Long, count: Int): UIO[ImportCompleted] =
            ZIO.succeed(ImportCompleted(
                sourceAccountId = sourceAccountId,
                count = count,
                occurredAt = Instant.now()
            ))

        // Test helpers
        def getTransactions: UIO[Map[TransactionId, Transaction]] = ZIO.succeed(transactions)
        def getProcessingStates: UIO[Map[TransactionId, TransactionProcessingState]] = ZIO.succeed(processingStates)
        def getEvents: UIO[List[Any]] = ZIO.succeed(events)
        def reset: UIO[Unit] = ZIO.succeed {
            transactions = Map.empty
            processingStates = Map.empty
            events = List.empty
        }
    end TestImportService

    // Test layer
    private val testLayer = ZLayer.succeed(new TestImportService)

    // Accessor for test operations
    private def testService = ZIO.service[TestImportService]

    // Sample raw transactions for testing
    private val sampleRawTransactions = Seq(
        RawTransaction(
            externalId = "tx-1",
            date = LocalDate.of(2023, 1, 15),
            amount = BigDecimal("100.50"),
            currency = "CZK",
            counterAccount = Some("123456789"),
            counterBankCode = Some("0800"),
            counterBankName = Some("Česká spořitelna"),
            variableSymbol = Some("12345"),
            constantSymbol = None,
            specificSymbol = None,
            userIdentification = Some("John Doe"),
            message = Some("Payment for services"),
            transactionType = "PAYMENT",
            comment = None
        ),
        RawTransaction(
            externalId = "tx-2",
            date = LocalDate.of(2023, 1, 16),
            amount = BigDecimal("250.75"),
            currency = "CZK",
            counterAccount = Some("987654321"),
            counterBankCode = Some("0300"),
            counterBankName = Some("ČSOB"),
            variableSymbol = Some("54321"),
            constantSymbol = None,
            specificSymbol = None,
            userIdentification = Some("Jane Smith"),
            message = Some("Invoice payment"),
            transactionType = "PAYMENT",
            comment = None
        )
    )

    // Tests
    override def spec =
        suite("ImportService")(
            test("importTransactions should create transaction entities with Imported status") {
                for
                    _ <- testService.flatMap(_.reset)
                    sourceAccountId = 1L
                    result <- ImportService.importTransactions(sourceAccountId, sampleRawTransactions)
                    (importedCount, duplicateIds) = result
                    transactions <- testService.flatMap(_.getTransactions)
                    processingStates <- testService.flatMap(_.getProcessingStates)
                yield
                    assertTrue(
                        importedCount == 2,
                        duplicateIds.isEmpty,
                        transactions.size == 2,
                        processingStates.size == 2,
                        processingStates.values.forall(_.status == TransactionStatus.Imported)
                    )
            },
            
            test("importTransactions should detect duplicate transactions") {
                for
                    _ <- testService.flatMap(_.reset)
                    sourceAccountId = 1L
                    _ <- ImportService.importTransactions(sourceAccountId, sampleRawTransactions)
                    // Import the same transactions again
                    result <- ImportService.importTransactions(sourceAccountId, sampleRawTransactions)
                    (importedCount, duplicateIds) = result
                    transactions <- testService.flatMap(_.getTransactions)
                    events <- testService.flatMap(_.getEvents)
                    duplicateEvents = events.collect { case e: DuplicateTransactionDetected => e }
                yield
                    assertTrue(
                        importedCount == 0,
                        duplicateIds.length == 2,
                        transactions.size == 2,  // Still only 2 transactions (no duplicates created)
                        duplicateEvents.nonEmpty  // Duplicate detection events were emitted
                    )
            },
            
            test("ImportCompleted event should be published with correct count") {
                for
                    _ <- testService.flatMap(_.reset)
                    sourceAccountId = 1L
                    _ <- ImportService.importTransactions(sourceAccountId, sampleRawTransactions)
                    events <- testService.flatMap(_.getEvents)
                    importCompletedEvents = events.collect { case e: ImportCompleted => e }
                yield
                    assertTrue(
                        importCompletedEvents.nonEmpty,
                        importCompletedEvents.head.count == 2,
                        importCompletedEvents.head.sourceAccountId == sourceAccountId
                    )
            },
            
            test("TransactionImported events should be published for each transaction") {
                for
                    _ <- testService.flatMap(_.reset)
                    sourceAccountId = 1L
                    _ <- ImportService.importTransactions(sourceAccountId, sampleRawTransactions)
                    events <- testService.flatMap(_.getEvents)
                    transactionImportedEvents = events.collect { case e: TransactionImported => e }
                yield
                    assertTrue(
                        transactionImportedEvents.length == 2
                    )
            }
        ).provideLayer(testLayer) @@ sequential
    end spec
end ImportServiceSpec