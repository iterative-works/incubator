package works.iterative.incubator.view.dev

import works.iterative.incubator.transactions._
import works.iterative.incubator.view.dev.examples._
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * Provides test data for UI component previews.
 * Contains example data for different UI scenarios.
 */
class TestDataProvider {
  
  // Available preview scenarios by component
  val getAvailableScenarios: Map[String, List[String]] = Map(
    "source-accounts" -> List("default", "empty", "with-errors", "form"),
    "transactions" -> List("default", "empty", "with-pending", "with-warnings")
  )
  
  // Get data for source account scenarios
  def getSourceAccountData(scenario: String): ExampleData = scenario match {
    case "default" => ExampleData(
      sourceAccounts = List(
        SourceAccount(SourceAccountId("1"), "Main Checking", "CZ123456789", "FIO", active = true),
        SourceAccount(SourceAccountId("2"), "Savings", "CZ987654321", "CSOB", active = true),
        SourceAccount(SourceAccountId("3"), "Old Account", "CZ555555555", "KB", active = false)
      )
    )
    case "empty" => ExampleData(sourceAccounts = List())
    case "with-errors" => ExampleData(
      sourceAccounts = List(
        SourceAccount(SourceAccountId("1"), "Main Checking", "CZ123456789", "FIO", active = true),
        SourceAccount(SourceAccountId("4"), "Error Account", "CZ11111111", "ERROR", active = true)
      ),
      errors = List("Unable to connect to account CZ11111111")
    )
    case "form" => ExampleData(
      // Empty data for form example
      formValues = Map(
        "name" -> "",
        "accountNumber" -> "",
        "bankCode" -> ""
      )
    )
    case _ => ExampleData.empty
  }
  
  // Get data for transaction scenarios
  def getTransactionData(scenario: String): ExampleData = scenario match {
    case "default" => 
      val now = ZonedDateTime.now()
      ExampleData(
        transactions = List(
          Transaction(
            id = TransactionId("tx1"),
            amount = BigDecimal("150.75"),
            date = now.minusDays(1),
            description = "Grocery Store Purchase",
            accountId = "CZ123456789/FIO",
            sourceAccountId = Some(SourceAccountId("1"))
          ),
          Transaction(
            id = TransactionId("tx2"),
            amount = BigDecimal("-45.50"),
            date = now.minusDays(2),
            description = "Coffee Shop",
            accountId = "CZ123456789/FIO",
            sourceAccountId = Some(SourceAccountId("1"))
          ),
          Transaction(
            id = TransactionId("tx3"),
            amount = BigDecimal("1200.00"),
            date = now.minusDays(5),
            description = "Salary Payment",
            accountId = "CZ987654321/CSOB",
            sourceAccountId = Some(SourceAccountId("2"))
          )
        ),
        processingStates = List(
          TransactionProcessingState(
            transactionId = TransactionId("tx1"),
            status = TransactionStatus.Processed,
            created = LocalDateTime.now().minusDays(1),
            updated = Some(LocalDateTime.now())
          ),
          TransactionProcessingState(
            transactionId = TransactionId("tx2"),
            status = TransactionStatus.Processed,
            created = LocalDateTime.now().minusDays(2),
            updated = Some(LocalDateTime.now())
          ),
          TransactionProcessingState(
            transactionId = TransactionId("tx3"),
            status = TransactionStatus.Processed,
            created = LocalDateTime.now().minusDays(5),
            updated = Some(LocalDateTime.now())
          )
        )
      )
    case "empty" => ExampleData(transactions = List())
    case "with-pending" =>
      val now = ZonedDateTime.now()
      ExampleData(
        transactions = List(
          Transaction(
            id = TransactionId("tx1"),
            amount = BigDecimal("150.75"),
            date = now.minusDays(1),
            description = "Grocery Store Purchase",
            accountId = "CZ123456789/FIO",
            sourceAccountId = Some(SourceAccountId("1"))
          ),
          Transaction(
            id = TransactionId("tx4"),
            amount = BigDecimal("75.25"),
            date = now,
            description = "Pending Transaction",
            accountId = "CZ123456789/FIO",
            sourceAccountId = Some(SourceAccountId("1"))
          )
        ),
        processingStates = List(
          TransactionProcessingState(
            transactionId = TransactionId("tx1"),
            status = TransactionStatus.Processed,
            created = LocalDateTime.now().minusDays(1),
            updated = Some(LocalDateTime.now())
          ),
          TransactionProcessingState(
            transactionId = TransactionId("tx4"),
            status = TransactionStatus.Pending,
            created = LocalDateTime.now(),
            updated = None
          )
        )
      )
    case "with-warnings" =>
      val now = ZonedDateTime.now()
      ExampleData(
        transactions = List(
          Transaction(
            id = TransactionId("tx1"),
            amount = BigDecimal("150.75"),
            date = now.minusDays(1),
            description = "Grocery Store Purchase",
            accountId = "CZ123456789/FIO",
            sourceAccountId = Some(SourceAccountId("1"))
          ),
          Transaction(
            id = TransactionId("tx5"),
            amount = BigDecimal("999.99"),
            date = now,
            description = "Warning Transaction",
            accountId = "CZ123456789/FIO",
            sourceAccountId = Some(SourceAccountId("1"))
          )
        ),
        processingStates = List(
          TransactionProcessingState(
            transactionId = TransactionId("tx1"),
            status = TransactionStatus.Processed,
            created = LocalDateTime.now().minusDays(1),
            updated = Some(LocalDateTime.now())
          ),
          TransactionProcessingState(
            transactionId = TransactionId("tx5"),
            status = TransactionStatus.Warning,
            created = LocalDateTime.now(),
            updated = Some(LocalDateTime.now())
          )
        ),
        warnings = List("Unusual amount detected for transaction tx5")
      )
    case _ => ExampleData.empty
  }
}