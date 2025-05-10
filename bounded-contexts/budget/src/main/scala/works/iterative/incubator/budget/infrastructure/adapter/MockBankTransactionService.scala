package works.iterative.incubator.budget.infrastructure.adapter

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.service.BankTransactionService
import works.iterative.incubator.budget.domain.service.TransactionImportError
import works.iterative.incubator.budget.domain.service.TransactionImportError.InvalidDateRange
import java.time.{Instant, LocalDate}
import java.util.Currency
import zio.*
import scala.util.Random
import java.util.UUID

/** Mock implementation of BankTransactionService for testing and development purposes.
  *
  * This implementation generates random transactions for a specified date range.
  *
  * Category: Mock Adapter Layer: Infrastructure
  */
final case class MockBankTransactionService() extends BankTransactionService:
    private val random = new Random()

    /** Maximum number of days allowed in a date range, by bank type */
    val MaxDateRangeDays = Map(
        "fio" -> 90, // Fio Bank has 90 days limit
        "mock" -> 45, // Our mock bank has 45 days limit
        "default" -> 30 // Default for unknown banks
    )

    /** Bank-specific date range validation based on account ID.
      *
      * This implementation uses the bank type from the account ID to determine which validation
      * rules to apply.
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
    override protected def validateBankSpecificDateRange(
        accountId: AccountId,
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        // Get the max days based on bank ID from the account ID
        val maxDays = MaxDateRangeDays.getOrElse(accountId.bankId, MaxDateRangeDays("default"))

        if startDate.plusDays(maxDays).isBefore(endDate) then
            ZIO.fail(InvalidDateRange(
                s"Date range cannot exceed $maxDays days (${accountId.bankId} bank limitation)"
            ))
        else
            ZIO.unit
        end if
    end validateBankSpecificDateRange

    /** Legacy validation method for backward compatibility.
      *
      * @deprecated
      *   Use validateDateRangeForAccount instead
      */
    @deprecated("Use validateDateRangeForAccount instead", "2025.05")
    override def validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): ZIO[Any, TransactionImportError, Unit] =
        // Run the basic validation first
        super.validateDateRange(startDate, endDate).flatMap { _ =>
            // Then add Mock-specific validation with default max days
            val defaultMaxDays = MaxDateRangeDays("default")
            if startDate.plusDays(defaultMaxDays).isBefore(endDate) then
                ZIO.fail(InvalidDateRange(
                    s"Date range cannot exceed $defaultMaxDays days (default limitation)"
                ))
            else
                ZIO.unit
            end if
        }

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
                    BigDecimal(
                        random.nextInt(10000) - 5000
                    ) / 100, // Random amount between -50 and 50
                    Currency.getInstance("CZK") // Czech koruna
                )

                // Create descriptions based on whether it's income or expense
                val (description, counterparty) =
                    if amount.isPositive then
                        ("Incoming payment", Some(s"Customer ${random.nextInt(100)}"))
                    else
                        ("Payment", Some(s"Merchant ${random.nextInt(100)}"))

                Transaction(
                    id = MockBankTransactionService.generateRandomTransactionId(),
                    date = date,
                    amount = amount,
                    description = description,
                    counterparty = counterparty,
                    counterAccount = Some(s"2345678901/${random.nextInt(10000)}"),
                    reference = Some(s"REF${random.nextInt(1000000)}"),
                    importBatchId = MockBankTransactionService.generateRandomBatchId(),
                    status = TransactionStatus.Imported,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            }
        }
end MockBankTransactionService

/** Companion object for MockBankTransactionService.
  */
object MockBankTransactionService:
    /** Generates a random TransactionId for testing purposes.
      *
      * @return
      *   A new random TransactionId
      */
    def generateRandomTransactionId(): TransactionId =
        val randomBankId = UUID.randomUUID().toString.take(8)
        val randomAccountId = UUID.randomUUID().toString.take(12)
        val randomTxId = UUID.randomUUID().toString
        TransactionId(AccountId(randomBankId, randomAccountId), randomTxId)
    end generateRandomTransactionId

    /** WARNING: This method should only be used for tests. In production, import batch IDs should
      * be generated by the ImportBatchRepository.
      *
      * @deprecated
      *   Use ImportBatchRepository.createBatch or ImportBatchRepository.nextSequenceNumber instead.
      * @return
      *   A new import batch ID with a random account ID and sequence number for testing
      */
    def generateRandomBatchId(): ImportBatchId =
        ImportBatchId(java.util.UUID.randomUUID().toString.take(8), 1L)

    /** Creates a mock implementation of BankTransactionService.
      *
      * @return
      *   A ZLayer that provides a BankTransactionService
      */
    val layer: ULayer[BankTransactionService] =
        ZLayer.succeed(MockBankTransactionService())

end MockBankTransactionService
