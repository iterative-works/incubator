package works.iterative.incubator.budget.domain.service

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.TransactionImportError
import works.iterative.incubator.budget.domain.service.TransactionImportError.InvalidDateRange
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
      /** Maximum allowed date range by bank type */
      val MaxDateRangeDays = Map(
        "fio" -> 90,
        "mock" -> 45,
        "default" -> 30
      )

      /** Bank-specific date validation based on account ID */
      override protected def validateBankSpecificDateRange(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, TransactionImportError, Unit] =
        // Get the max days based on bank ID
        val maxDays = MaxDateRangeDays.getOrElse(accountId.bankId, MaxDateRangeDays("default"))

        if startDate.plusDays(maxDays).isBefore(endDate) then
          ZIO.fail(InvalidDateRange(
            s"Date range cannot exceed $maxDays days (${accountId.bankId} bank limitation)"
          ))
        else
          ZIO.unit

      /** Legacy validation method for backward compatibility */
      @deprecated("Use validateDateRangeForAccount instead", "2025.05")
      override def validateDateRange(
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, TransactionImportError, Unit] =
        // Use default validation with fixed max days for backward compatibility
        super.validateDateRange(startDate, endDate).flatMap { _ =>
          val defaultMaxDays = 90 // Fio Bank's limit
          if startDate.plusDays(defaultMaxDays).isBefore(endDate) then
            ZIO.fail(InvalidDateRange(
              s"Date range cannot exceed $defaultMaxDays days (test bank limitation)"
            ))
          else
            ZIO.unit
        }

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
      /** Bank-specific validation is just using default implementation for empty test */
      override protected def validateBankSpecificDateRange(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, TransactionImportError, Unit] = ZIO.unit

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
      /** Bank-specific validation is just using default implementation for error test */
      override protected def validateBankSpecificDateRange(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, TransactionImportError, Unit] = ZIO.unit

      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.fail(new RuntimeException("Simulated bank API error"))
    }
  )