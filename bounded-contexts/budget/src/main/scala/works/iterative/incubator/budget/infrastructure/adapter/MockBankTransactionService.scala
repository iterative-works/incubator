package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.BankTransactionService
import java.time.{Instant, LocalDate}
import java.util.Currency
import zio.*
import scala.util.Random

/** Mock implementation of BankTransactionService for testing and development purposes.
  *
  * This implementation generates random transactions for a specified date range.
  *
  * Category: Mock Adapter
  * Layer: Infrastructure
  */
final case class MockBankTransactionService() extends BankTransactionService:
  private val random = new Random()

  override def fetchTransactions(
      accountId: AccountId,
      startDate: LocalDate,
      endDate: LocalDate
  ): ZIO[Any, Throwable, List[Transaction]] =
    ZIO.succeed {
      // Generate 1-5 transactions per day in the range
      val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt + 1
      val transactionCount = days * (1 + random.nextInt(5))
      
      // Generate random transactions
      List.tabulate(transactionCount) { i =>
        val dayOffset = random.nextInt(days)
        val date = startDate.plusDays(dayOffset)
        val amount = Money(
          BigDecimal(random.nextInt(10000) - 5000) / 100, // Random amount between -50 and 50
          Currency.getInstance("CZK") // Czech koruna
        )
        
        // Create descriptions based on whether it's income or expense
        val (description, counterparty) = 
          if (amount.isPositive)
            ("Incoming payment", Some(s"Customer ${random.nextInt(100)}"))
          else
            ("Payment", Some(s"Merchant ${random.nextInt(100)}"))
            
        Transaction(
          id = TransactionId.generate(),
          accountId = accountId,
          date = date,
          amount = amount,
          description = description,
          counterparty = counterparty,
          counterAccount = Some(s"2345678901/${random.nextInt(10000)}"),
          reference = Some(s"REF${random.nextInt(1000000)}"),
          importBatchId = ImportBatchId.generate(),
          status = TransactionStatus.Imported,
          createdAt = Instant.now(),
          updatedAt = Instant.now()
        )
      }
    }

/** Companion object for MockBankTransactionService.
  */
object MockBankTransactionService:
  /** Creates a mock implementation of BankTransactionService.
    *
    * @return
    *   A ZLayer that provides a BankTransactionService
    */
  val layer: ULayer[BankTransactionService] =
    ZLayer.succeed(MockBankTransactionService())