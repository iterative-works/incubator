package works.iterative.incubator.transactions
package infrastructure

import zio.*
import zio.test.*
import java.time.{LocalDate, Instant}
import works.iterative.incubator.transactions.domain.repository.{
    TransactionRepository,
    TransactionProcessingStateRepository,
    SourceAccountRepository
}
import works.iterative.incubator.transactions.domain.model.{
    Transaction,
    TransactionId,
    TransactionStatus,
    SourceAccount,
    TransactionProcessingState
}
import works.iterative.incubator.transactions.domain.query.{
    TransactionQuery,
    TransactionProcessingStateQuery
}
import zio.test.TestAspect.sequential

object PostgreSQLTransactionRepositorySpec extends ZIOSpecDefault:
    import PostgreSQLLayers.*
    import MigrateAspects.*

    val repositoryLayer =
        transactionRepositoryLayer ++
            processingStateRepositoryLayer ++
            sourceAccountRepositoryLayer

    // Create sample transaction for testing
    def createSampleTransaction: Transaction =
        Transaction(
            id = TransactionId(sourceAccountId = 1L, transactionId = "TX123"),
            date = LocalDate.now(),
            amount = BigDecimal("500.00"),
            currency = "CZK",
            counterAccount = Some("987654"),
            counterBankCode = Some("0100"),
            counterBankName = Some("Test Bank"),
            variableSymbol = Some("VS123"),
            constantSymbol = Some("CS123"),
            specificSymbol = Some("SS123"),
            userIdentification = Some("User Test"),
            message = Some("Test payment"),
            transactionType = "PAYMENT",
            comment = Some("Test comment"),
            importedAt = Instant.now()
        )

    // Create sample transaction processing state for testing
    def createSampleProcessingState(transaction: Transaction): TransactionProcessingState =
        TransactionProcessingState.initial(transaction)

    def spec = {
        suite("PostgreSQLTransactionRepository")(
            test("should save and retrieve a transaction") {
                for
                    transactionRepo <- ZIO.service[TransactionRepository]
                    stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                    // Create a source account first (to satisfy foreign key)
                    sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                    sourceAccount = SourceAccount(
                        id = 1L,
                        accountId = "123456",
                        bankId = "0800",
                        name = "Test Account",
                        currency = "CZK"
                    )
                    _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                    // Create a transaction
                    transaction = createSampleTransaction
                    processingState = createSampleProcessingState(transaction)

                    // Execute
                    _ <- transactionRepo.save(transaction.id, transaction)
                    _ <- stateRepo.save(processingState.transactionId, processingState)
                    retrievedTx <- transactionRepo.load(transaction.id)
                    retrievedState <- stateRepo.load(transaction.id)
                yield
                // Assert
                assertTrue(
                    retrievedTx.isDefined,
                    retrievedTx.get.id == transaction.id,
                    retrievedTx.get.amount == transaction.amount,
                    retrievedTx.get.date == transaction.date,
                    retrievedState.isDefined,
                    retrievedState.get.status == TransactionStatus.Imported
                )
            },
            test("should find transactions by query") {
                for
                    transactionRepo <- ZIO.service[TransactionRepository]
                    stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                    // Create a source account first (to satisfy foreign key)
                    sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                    sourceAccount = SourceAccount(
                        id = 1L,
                        accountId = "123456",
                        bankId = "0800",
                        name = "Test Account",
                        currency = "CZK"
                    )
                    _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                    transaction1 = createSampleTransaction
                    transaction2 = createSampleTransaction.copy(
                        id = TransactionId(sourceAccountId = 1L, transactionId = "TX124"),
                        amount = BigDecimal("1000.00")
                    )
                    processingState1 = createSampleProcessingState(transaction1)
                    processingState2 = createSampleProcessingState(transaction2)

                    // Execute
                    _ <- transactionRepo.save(transaction1.id, transaction1)
                    _ <- transactionRepo.save(transaction2.id, transaction2)
                    _ <- stateRepo.save(processingState1.transactionId, processingState1)
                    _ <- stateRepo.save(processingState2.transactionId, processingState2)
                    results <- transactionRepo.find(TransactionQuery())
                yield
                // Assert
                assertTrue(
                    results.size >= 2,
                    results.exists(t => t.id == transaction1.id),
                    results.exists(t => t.id == transaction2.id)
                )
            },
            test("should find transactions by filter") {
                for
                    transactionRepo <- ZIO.service[TransactionRepository]
                    stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                    // Create a source account first (to satisfy foreign key)
                    sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                    sourceAccount = SourceAccount(
                        id = 1L,
                        accountId = "123456",
                        bankId = "0800",
                        name = "Test Account",
                        currency = "CZK"
                    )
                    _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                    transaction1 = createSampleTransaction
                    transaction2 = createSampleTransaction.copy(
                        id = TransactionId(sourceAccountId = 1L, transactionId = "TX124"),
                        amount = BigDecimal("1000.00")
                    )
                    processingState1 = createSampleProcessingState(transaction1)
                    processingState2 = createSampleProcessingState(transaction2)

                    // Execute
                    _ <- transactionRepo.save(transaction1.id, transaction1)
                    _ <- transactionRepo.save(transaction2.id, transaction2)
                    _ <- stateRepo.save(processingState1.transactionId, processingState1)
                    _ <- stateRepo.save(processingState2.transactionId, processingState2)
                    results <- transactionRepo.find(TransactionQuery(
                        amountMin = Some(BigDecimal("1000.00")),
                        amountMax = Some(BigDecimal("1000.00"))
                    ))
                yield
                // Assert
                assertTrue(
                    results.size == 1,
                    results.head.id == transaction2.id,
                    results.head.amount == transaction2.amount
                )
            },
            test("should update transaction processing state") {
                for
                    transactionRepo <- ZIO.service[TransactionRepository]
                    stateRepo <- ZIO.service[TransactionProcessingStateRepository]

                    // Create a source account first (to satisfy foreign key)
                    sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                    sourceAccount = SourceAccount(
                        id = 1L,
                        accountId = "123456",
                        bankId = "0800",
                        name = "Test Account",
                        currency = "CZK"
                    )
                    _ <- sourceAccountRepo.save(sourceAccount.id, sourceAccount)

                    transaction = createSampleTransaction
                    processingState = createSampleProcessingState(transaction)

                    // Execute - first save
                    _ <- transactionRepo.save(transaction.id, transaction)
                    _ <- stateRepo.save(processingState.transactionId, processingState)

                    // Update the processing state
                    updatedState = processingState.withAICategorization(
                        payeeName = Some("Test Payee"),
                        category = Some("Test Category"),
                        memo = Some("Test Memo")
                    )

                    // Save the updated state and retrieve
                    _ <- stateRepo.save(updatedState.transactionId, updatedState)
                    retrievedState <- stateRepo.load(transaction.id)
                yield
                // Assert
                assertTrue(
                    retrievedState.isDefined,
                    retrievedState.get.status == TransactionStatus.Categorized,
                    retrievedState.get.suggestedPayeeName.contains("Test Payee"),
                    retrievedState.get.suggestedCategory.contains("Test Category")
                )
            }
        ) @@ sequential @@ migrate
    }.provideSomeShared[Scope](
        flywayMigrationServiceLayer,
        repositoryLayer
    )
end PostgreSQLTransactionRepositorySpec
