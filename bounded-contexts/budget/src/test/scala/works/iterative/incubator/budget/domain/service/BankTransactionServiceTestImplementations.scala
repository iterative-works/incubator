package works.iterative.incubator.budget.domain.service

import works.iterative.incubator.budget.domain.model.*
import java.time.{Instant, LocalDate}
import java.util.Currency
import zio.*

/** Shared constants for test BankTransactionService implementations.
  */
object TestBankTransactionService:
  val TestTransactionCount = 10

  /** Generates test transactions with the given account ID and date range.
    */
  def generateTransactions(
      accountId: AccountId,
      startDate: LocalDate,
      count: Int
  ): List[Transaction] =
    List.tabulate(count) { i =>
      Transaction(
        id = TransactionId.generate(),
        accountId = accountId,
        date = startDate.plusDays(i % 7),
        amount = Money(BigDecimal(-100 * (i + 1)), Currency.getInstance("CZK")),
        description = s"Test transaction $i",
        counterparty = Some(s"Test Merchant $i"),
        counterAccount = Some(s"123456789/$i"),
        reference = Some(s"REF$i"),
        importBatchId = ImportBatchId.generate(),
        status = TransactionStatus.Imported,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
      )
    }

/** Normal implementation of BankTransactionService for successful test cases.
  */
object FioBankServiceNormal:
  val layer: ULayer[BankTransactionService] = ZLayer.succeed(
    new BankTransactionService {
      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.succeed(TestBankTransactionService.generateTransactions(accountId, startDate, TestBankTransactionService.TestTransactionCount))
    }
  )

/** BankTransactionService implementation that simulates empty transaction lists.
  */
object FioBankServiceEmpty:
  val layer: ULayer[BankTransactionService] = ZLayer.succeed(
    new BankTransactionService {
      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.succeed(List.empty)
    }
  )

/** BankTransactionService implementation that simulates errors.
  */
object FioBankServiceError:
  val layer: ULayer[BankTransactionService] = ZLayer.succeed(
    new BankTransactionService {
      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.fail(new RuntimeException("Simulated bank API error"))
    }
  )