package works.iterative.incubator.view.dev

import works.iterative.incubator.transactions.domain.model.SourceAccount
import works.iterative.incubator.transactions.domain.model.Transaction
import works.iterative.incubator.transactions.domain.model.TransactionId
import works.iterative.incubator.transactions.domain.model.TransactionProcessingState
import works.iterative.incubator.transactions.domain.model.TransactionStatus
import works.iterative.incubator.view.dev.examples.*
import works.iterative.incubator.transactions.web.view.TransactionWithState
import java.time.{LocalDate, Instant}

/** Provides test data for UI component previews. Contains example data for different UI scenarios.
  */
class TestDataProvider:

    // Available preview scenarios by component
    val getAvailableScenarios: Map[String, List[String]] = Map(
        "source-accounts" -> List("default", "empty", "with-errors", "form"),
        "transactions" -> List("default", "empty", "with-pending", "with-warnings")
    )

    // Get data for source account scenarios
    def getSourceAccountData(scenario: String): ExampleData = scenario match
        case "default" => ExampleData(
                sourceAccounts = List(
                    SourceAccount(1L, "CZ123456789", "FIO", "Main Checking", "CZK", active = true),
                    SourceAccount(2L, "CZ987654321", "CSOB", "Savings", "CZK", active = true),
                    SourceAccount(3L, "CZ555555555", "KB", "Old Account", "CZK", active = false)
                )
            )
        case "empty" => ExampleData(sourceAccounts = List())
        case "with-errors" => ExampleData(
                sourceAccounts = List(
                    SourceAccount(1L, "CZ123456789", "FIO", "Main Checking", "CZK", active = true),
                    SourceAccount(4L, "CZ11111111", "ERROR", "Error Account", "CZK", active = true)
                ),
                errors = List("Unable to connect to account CZ11111111")
            )
        case "form" => ExampleData(
                sourceAccounts = List(SourceAccount(
                    1L,
                    "CZ123456789",
                    "FIO",
                    "Main Checking",
                    "CZK",
                    active = true
                )),
                formValues = Map(
                    "name" -> "Main Checking",
                    "accountId" -> "CZ123456789",
                    "bankId" -> "FIO"
                )
            )
        case _ => ExampleData.empty

    // Get data for transaction scenarios
    def getTransactionData(scenario: String): ExampleData = scenario match
        case "default" =>
            val now = LocalDate.now()
            val txs = List(
                Transaction(
                    id = TransactionId(1L, "tx1"),
                    date = now.minusDays(1),
                    amount = BigDecimal("150.75"),
                    currency = "CZK",
                    counterAccount = Some("2345678901"),
                    counterBankCode = Some("0800"),
                    counterBankName = Some("Česká Spořitelna"),
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Grocery Store Purchase"),
                    message = None,
                    transactionType = "Payment",
                    comment = None,
                    importedAt = Instant.now()
                ),
                Transaction(
                    id = TransactionId(1L, "tx2"),
                    date = now.minusDays(2),
                    amount = BigDecimal("-45.50"),
                    currency = "CZK",
                    counterAccount = Some("9876543210"),
                    counterBankCode = Some("0300"),
                    counterBankName = Some("ČSOB"),
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Coffee Shop"),
                    message = None,
                    transactionType = "Payment",
                    comment = None,
                    importedAt = Instant.now()
                ),
                Transaction(
                    id = TransactionId(2L, "tx3"),
                    date = now.minusDays(5),
                    amount = BigDecimal("1200.00"),
                    currency = "CZK",
                    counterAccount = Some("1122334455"),
                    counterBankCode = Some("0100"),
                    counterBankName = Some("KB"),
                    variableSymbol = Some("123456"),
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Salary Payment"),
                    message = None,
                    transactionType = "Income",
                    comment = None,
                    importedAt = Instant.now()
                )
            )

            val states = List(
                TransactionProcessingState(
                    transactionId = TransactionId(1L, "tx1"),
                    status = TransactionStatus.Imported,
                    suggestedPayeeName = None,
                    suggestedCategory = None,
                    suggestedMemo = None,
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = None,
                    submittedAt = None
                ),
                TransactionProcessingState(
                    transactionId = TransactionId(1L, "tx2"),
                    status = TransactionStatus.Imported,
                    suggestedPayeeName = None,
                    suggestedCategory = None,
                    suggestedMemo = None,
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = None,
                    submittedAt = None
                ),
                TransactionProcessingState(
                    transactionId = TransactionId(2L, "tx3"),
                    status = TransactionStatus.Imported,
                    suggestedPayeeName = None,
                    suggestedCategory = None,
                    suggestedMemo = None,
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = None,
                    submittedAt = None
                )
            )

            // Create TransactionWithState objects
            val transactionsWithState = txs.map { tx =>
                val state = states.find(_.transactionId == tx.id)
                TransactionWithState(tx, state)
            }

            ExampleData(
                transactions = txs,
                processingStates = states,
                transactionsWithState = transactionsWithState
            )

        case "empty" => ExampleData(transactions = List())
        case "with-pending" =>
            val now = LocalDate.now()
            val txs = List(
                Transaction(
                    id = TransactionId(1L, "tx1"),
                    date = now.minusDays(1),
                    amount = BigDecimal("150.75"),
                    currency = "CZK",
                    counterAccount = Some("2345678901"),
                    counterBankCode = Some("0800"),
                    counterBankName = Some("Česká Spořitelna"),
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Grocery Store Purchase"),
                    message = None,
                    transactionType = "Payment",
                    comment = None,
                    importedAt = Instant.now()
                ),
                Transaction(
                    id = TransactionId(1L, "tx4"),
                    date = now,
                    amount = BigDecimal("75.25"),
                    currency = "CZK",
                    counterAccount = Some("9876543210"),
                    counterBankCode = Some("0300"),
                    counterBankName = Some("ČSOB"),
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Pending Transaction"),
                    message = None,
                    transactionType = "Payment",
                    comment = None,
                    importedAt = Instant.now()
                )
            )

            val states = List(
                TransactionProcessingState(
                    transactionId = TransactionId(1L, "tx1"),
                    status = TransactionStatus.Categorized,
                    suggestedPayeeName = Some("Grocery Store"),
                    suggestedCategory = Some("Food:Groceries"),
                    suggestedMemo = Some("Weekly shopping"),
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = Some(Instant.now()),
                    submittedAt = None
                ),
                TransactionProcessingState(
                    transactionId = TransactionId(1L, "tx4"),
                    status = TransactionStatus.Imported,
                    suggestedPayeeName = None,
                    suggestedCategory = None,
                    suggestedMemo = None,
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = None,
                    submittedAt = None
                )
            )

            // Create TransactionWithState objects
            val transactionsWithState = txs.map { tx =>
                val state = states.find(_.transactionId == tx.id)
                TransactionWithState(tx, state)
            }

            ExampleData(
                transactions = txs,
                processingStates = states,
                transactionsWithState = transactionsWithState
            )

        case "with-warnings" =>
            val now = LocalDate.now()
            val txs = List(
                Transaction(
                    id = TransactionId(1L, "tx1"),
                    date = now.minusDays(1),
                    amount = BigDecimal("150.75"),
                    currency = "CZK",
                    counterAccount = Some("2345678901"),
                    counterBankCode = Some("0800"),
                    counterBankName = Some("Česká Spořitelna"),
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Grocery Store Purchase"),
                    message = None,
                    transactionType = "Payment",
                    comment = None,
                    importedAt = Instant.now()
                ),
                Transaction(
                    id = TransactionId(1L, "tx5"),
                    date = now,
                    amount = BigDecimal("999.99"),
                    currency = "CZK",
                    counterAccount = Some("5555555555"),
                    counterBankCode = Some("0100"),
                    counterBankName = Some("KB"),
                    variableSymbol = None,
                    constantSymbol = None,
                    specificSymbol = None,
                    userIdentification = Some("Warning Transaction"),
                    message = None,
                    transactionType = "Payment",
                    comment = None,
                    importedAt = Instant.now()
                )
            )

            val states = List(
                TransactionProcessingState(
                    transactionId = TransactionId(1L, "tx1"),
                    status = TransactionStatus.Categorized,
                    suggestedPayeeName = Some("Grocery Store"),
                    suggestedCategory = Some("Food:Groceries"),
                    suggestedMemo = Some("Weekly shopping"),
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = Some(Instant.now()),
                    submittedAt = None
                ),
                TransactionProcessingState(
                    transactionId = TransactionId(1L, "tx5"),
                    status = TransactionStatus.Imported,
                    suggestedPayeeName = None,
                    suggestedCategory = None,
                    suggestedMemo = None,
                    overridePayeeName = None,
                    overrideCategory = None,
                    overrideMemo = None,
                    ynabTransactionId = None,
                    ynabAccountId = None,
                    processedAt = None,
                    submittedAt = None
                )
            )

            // Create TransactionWithState objects
            val transactionsWithState = txs.map { tx =>
                val state = states.find(_.transactionId == tx.id)
                TransactionWithState(tx, state)
            }

            ExampleData(
                transactions = txs,
                processingStates = states,
                transactionsWithState = transactionsWithState,
                warnings = List("Unusual amount detected for transaction tx5")
            )
        case _ => ExampleData.empty
end TestDataProvider
