package works.iterative.incubator.budget.domain.service

import works.iterative.incubator.budget.domain.model.{AccountId, Transaction}
import works.iterative.incubator.budget.domain.service.TransactionImportError.*
import java.time.LocalDate
import zio.*
import works.iterative.incubator.budget.domain.model.ImportBatchId

/** Service interface for importing transactions from bank APIs.
  *
  * This service defines the contract for fetching transactions from any bank's API. Specific bank
  * implementations will provide concrete implementations of this interface.
  *
  * Category: Service Interface Layer: Domain
  */
trait BankTransactionService:
    /** Validates a date range for the specified account.
      *
      * This method is account-aware and can apply different validation rules based on the bank
      * associated with the account.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if the date range is valid for this account, or
      *   fails with an InvalidDateRange error if invalid
      */
    def validateDateRangeForAccount(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        // Perform basic validations first
        validateBasicDateRange(startDate, endDate).flatMap { _ =>
            // Then apply bank-specific validations
            validateBankSpecificDateRange(accountId, startDate, endDate)
        }

    /** Basic date range validation that applies to all banks.
      *
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if the basic validation passes, or fails with an
      *   InvalidDateRange error if invalid
      */
    protected def validateBasicDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        if startDate == null || endDate == null then
            ZIO.fail(InvalidDateRange("Both start and end dates are required"))
        else if startDate.isAfter(endDate) then
            ZIO.fail(InvalidDateRange("Start date cannot be after end date"))
        else if startDate.isAfter(LocalDate.now) || endDate.isAfter(LocalDate.now) then
            ZIO.fail(InvalidDateRange("Dates cannot be in the future"))
        else
            ZIO.unit

    /** Bank-specific date range validation.
      *
      * This method should be overridden by implementations to provide bank-specific validation
      * rules.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if the bank-specific validation passes, or fails
      *   with an InvalidDateRange error if invalid
      */
    protected def validateBankSpecificDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] = ZIO.unit

    /** Validates a date range without account context (backward compatibility).
      *
      * @deprecated
      *   Use validateDateRangeForAccount instead
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that completes successfully if basic validation passes, or fails with an
      *   InvalidDateRange error if invalid
      */
    @deprecated("Use validateDateRangeForAccount instead", "2025.05")
    def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        validateBasicDateRange(startDate, endDate)

    /** Fetches transactions from a bank API for a specified date range.
      *
      * @param accountId
      *   The ID of the source account to fetch transactions for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that returns a list of transactions or a service-specific error
      */
    def fetchTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate,
        importBatchId: ImportBatchId
    ): ZIO[Any, Throwable, List[Transaction]]
end BankTransactionService

/** Companion object providing ZIO accessor methods.
  */
object BankTransactionService:
    /** Validates a date range for a specific account.
      *
      * @param accountId
      *   The account ID to validate for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires BankTransactionService and completes successfully if the date
      *   range is valid for the specified account, or fails with an InvalidDateRange error if
      *   invalid
      */
    def validateDateRangeForAccount(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[BankTransactionService, TransactionImportError, Unit] =
        ZIO.serviceWithZIO(_.validateDateRangeForAccount(accountId, startDate, endDate))

    /** Validates a date range for a specific bank (backward compatibility).
      *
      * @deprecated
      *   Use validateDateRangeForAccount instead
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires BankTransactionService and completes successfully if the date
      *   range is valid, or fails with an InvalidDateRange error if invalid
      */
    @deprecated("Use validateDateRangeForAccount instead", "2025.05")
    def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[BankTransactionService, TransactionImportError, Unit] =
        ZIO.serviceWithZIO(_.validateDateRange(startDate, endDate))

    /** Accesses the service to fetch transactions.
      *
      * @param accountId
      *   The ID of the source account to fetch transactions for
      * @param startDate
      *   The start date of the range
      * @param endDate
      *   The end date of the range
      * @return
      *   A ZIO effect that requires BankTransactionService and returns a list of transactions or a
      *   service-specific error
      */
    def fetchTransactions(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate,
        importBatchId: ImportBatchId
    ): ZIO[BankTransactionService, Throwable, List[Transaction]] =
        ZIO.serviceWithZIO(_.fetchTransactions(accountId, startDate, endDate, importBatchId))
end BankTransactionService
