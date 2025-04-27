package works.iterative.incubator.budget.domain.mock

import zio.*
import java.time.{LocalDate, Instant}

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.port.*
import works.iterative.incubator.budget.infrastructure.repository.inmemory.*

/** Factory for creating and configuring mocks for domain testing.
  *
  * This factory provides utilities for creating complete test environments with all necessary mocks
  * configured for specific scenarios.
  */
object MockFactory:
    /** Contains all mock components needed for a test environment. */
    case class MockEnvironment(
        transactionRepo: InMemoryTransactionRepository,
        processingStateRepo: InMemoryTransactionProcessingStateRepository,
        categoryRepo: InMemoryCategoryRepository,
        sourceAccountRepo: InMemorySourceAccountRepository,
        transactionProvider: MockTransactionProvider,
        categorizationProvider: MockCategorizationProvider,
        submissionPort: MockTransactionSubmissionPort
    ):
        /** Resets all mocks to their default state.
          *
          * @return
          *   ZIO effect resetting all mocks
          */
        def reset: UIO[Unit] =
            transactionRepo.reset() *>
                processingStateRepo.reset() *>
                categoryRepo.reset() *>
                sourceAccountRepo.reset() *>
                transactionProvider.reset *>
                categorizationProvider.reset *>
                submissionPort.reset
    end MockEnvironment

    /** Creates a full mock environment for testing.
      *
      * @return
      *   ZIO effect with configured MockEnvironment
      */
    def createMockEnvironment: UIO[MockEnvironment] =
        for
            transactionRepo <- InMemoryTransactionRepository.make()
            processingStateRepo <- InMemoryTransactionProcessingStateRepository.make()
            categoryRepo <- InMemoryCategoryRepository.make()
            sourceAccountRepo <- InMemorySourceAccountRepository.make()
            transactionProvider <- MockTransactionProvider.make
            categorizationProvider <- MockCategorizationProvider.make
            submissionPort <- MockTransactionSubmissionPort.make
        yield MockEnvironment(
            transactionRepo,
            processingStateRepo,
            categoryRepo,
            sourceAccountRepo,
            transactionProvider,
            categorizationProvider,
            submissionPort
        )

    /** Creates a mock environment configured for a specific scenario.
      *
      * @param scenario
      *   Name of the scenario to configure
      * @return
      *   ZIO effect with a scenario-specific MockEnvironment
      */
    def createForScenario(scenario: String): UIO[MockEnvironment] =
        for
            env <- createMockEnvironment
            _ <- configureForScenario(env, scenario)
        yield env

    /** Configures a mock environment for a specific scenario.
      *
      * @param env
      *   The environment to configure
      * @param scenario
      *   Name of the scenario
      * @return
      *   ZIO effect configuring the environment
      */
    private def configureForScenario(env: MockEnvironment, scenario: String): UIO[Unit] =
        scenario match
            case "transaction-import" =>
                // Configure for transaction import scenario
                val account = SourceAccount(
                    id = 1L,
                    accountId = "test-account-1",
                    bankId = "TEST",
                    name = "Test Bank Account",
                    currency = "CZK"
                )

                // Add test transactions
                val transactions = List(
                    createTransaction(
                        id = "tx-1",
                        sourceAccountId = account.id,
                        amount = 100.0,
                        message = Some("Grocery Store")
                    ),
                    createTransaction(
                        id = "tx-2",
                        sourceAccountId = account.id,
                        amount = -25.0,
                        message = Some("Gas Station")
                    ),
                    createTransaction(
                        id = "tx-3",
                        sourceAccountId = account.id,
                        amount = 50.0,
                        message = Some("Paycheck")
                    ),
                    createTransaction(
                        id = "tx-4",
                        sourceAccountId = account.id,
                        amount = -30.0,
                        message = Some("Restaurant")
                    ),
                    createTransaction(
                        id = "tx-5",
                        sourceAccountId = account.id,
                        amount = -15.5,
                        message = Some("Coffee Shop")
                    )
                )

                // Configure mocks
                env.sourceAccountRepo.save(account.id, account) *>
                    env.transactionProvider.setTransactionsForAccount(
                        account.accountId,
                        transactions
                    )

            case "transaction-categorization" =>
                // Configure for categorization scenario
                val categories = List(
                    CategoryHelper.create("Groceries"),
                    CategoryHelper.create("Dining Out"),
                    CategoryHelper.create("Transportation"),
                    CategoryHelper.create("Income"),
                    CategoryHelper.create("Coffee Shops"),
                    works.iterative.incubator.budget.domain.model.Category.Uncategorized
                )

                // Add categories to repository and configure provider rules
                env.categoryRepo.addBatch(categories) *>
                    env.categorizationProvider.addRule(
                        "Grocery",
                        CategoryHelper.create("Groceries")
                    ) *>
                    env.categorizationProvider.addRule(
                        "Restaurant",
                        CategoryHelper.create("Dining Out")
                    ) *>
                    env.categorizationProvider.addRule(
                        "Gas",
                        CategoryHelper.create("Transportation")
                    ) *>
                    env.categorizationProvider.addRule(
                        "Paycheck",
                        CategoryHelper.create("Income")
                    ) *>
                    env.categorizationProvider.addRule(
                        "Coffee",
                        CategoryHelper.create("Coffee Shops")
                    ) *>
                    env.categorizationProvider.setDefaultCategory(
                        works.iterative.incubator.budget.domain.model.Category.Uncategorized
                    )

            case "transaction-submission" =>
                // Configure for submission scenario
                env.submissionPort.requireCategory(true) *>
                    env.submissionPort.setValidateBeforeSubmission(true) *>
                    env.submissionPort.setMinimumDescriptionLength(3)

            case "duplicate-detection" =>
                // Configure for duplicate detection
                val account = SourceAccount(
                    id = 2L,
                    accountId = "test-account-2",
                    bankId = "TEST",
                    name = "Test Bank Account",
                    currency = "CZK"
                )

                // Create transactions for duplicate testing
                val transaction = createTransaction(
                    id = "tx-dup-1", // Using same ID for both transactions to simulate external duplication
                    sourceAccountId = account.id,
                    amount = 100.0,
                    message = Some("Duplicate Transaction")
                )

                val duplicateTransaction = createTransaction(
                    id = "tx-dup-1", // Same ID as first transaction to test duplicate detection
                    sourceAccountId = account.id,
                    amount = 100.0,
                    message = Some("Duplicate Transaction")
                )

                // Configure mocks
                env.sourceAccountRepo.save(account.id, account) *>
                    env.transactionRepo.save(transaction.id, transaction) *>
                    env.transactionProvider.setTransactionsForAccount(
                        account.accountId,
                        List(duplicateTransaction)
                    )

            case _ => ZIO.unit

    /** Helper to create a test transaction */
    private def createTransaction(
        id: String,
        sourceAccountId: Long,
        amount: BigDecimal,
        date: LocalDate = LocalDate.now(),
        message: Option[String] = None,
        comment: Option[String] = None
    ): Transaction =
        Transaction(
            id = TransactionId(sourceAccountId, id),
            date = date,
            amount = amount,
            currency = "CZK",
            counterAccount = None,
            counterBankCode = None,
            counterBankName = None,
            variableSymbol = None,
            constantSymbol = None,
            specificSymbol = None,
            userIdentification = None,
            message = message,
            transactionType = "PAYMENT",
            comment = comment,
            importedAt = Instant.now()
        )

    val allLayers
        : ULayer[TransactionProvider & CategorizationProvider & TransactionSubmissionPort] =
        MockTransactionProvider.layer ++ MockCategorizationProvider.layer ++ MockTransactionSubmissionPort.layer

end MockFactory
