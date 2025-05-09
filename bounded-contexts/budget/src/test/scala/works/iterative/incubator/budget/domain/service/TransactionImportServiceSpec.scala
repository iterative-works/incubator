package works.iterative.incubator.budget.domain.service

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.service.TransactionImportError.*
import works.iterative.incubator.budget.infrastructure.adapter.*
import java.time.{Instant, LocalDate}
import java.util.Currency
import zio.*
import zio.test.*
import zio.test.Assertion.*

/** Test suite for TransactionImportService.
  */
object TransactionImportServiceSpec extends ZIOSpecDefault:
  // Test account ID to use throughout tests
  private val testAccountId = AccountId("fio", "test-account")

  // Environment for all tests
  override def spec = suite("TransactionImportService")(
    validateDateRangeSuite,
    importTransactionsSuite,
    getImportStatusSuite,
    getMostRecentImportSuite
  )

  // Suite for date range validation tests
  private val validateDateRangeSuite = suite("validateDateRange")(
    test("should succeed with valid date range") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        result <- TransactionImportService.validateDateRange(startDate, endDate).exit
      yield assert(result)(succeeds(isUnit))
    },

    test("should fail when start date is after end date") {
      for
        today <- ZIO.succeed(LocalDate.now)
        endDate = today.minusDays(7)
        startDate = today
        result <- TransactionImportService.validateDateRange(startDate, endDate).exit
      yield assert(result)(fails(isSubtype[InvalidDateRange](anything)))
    },

    test("should fail when dates are in the future") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today
        endDate = today.plusDays(1)
        result <- TransactionImportService.validateDateRange(startDate, endDate).exit
      yield assert(result)(fails(isSubtype[InvalidDateRange](anything)))
    },

    test("should fail when range exceeds max days") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(100)
        endDate = today
        result <- TransactionImportService.validateDateRange(startDate, endDate).exit
      yield assert(result)(fails(isSubtype[InvalidDateRange](anything)))
    }
  ).provide(
    InMemoryTransactionRepository.layer,
    InMemoryImportBatchRepository.layer,
    FioBankServiceNormal.layer,
    TransactionImportService.live
  )

  // Suite for importing transactions tests
  private val importTransactionsSuite = suite("importTransactions")(
    test("should successfully import transactions") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        result <- TransactionImportService.importTransactions(testAccountId, startDate, endDate)
      yield assert(result.status)(equalTo(ImportStatus.Completed)) &&
        assert(result.transactionCount)(equalTo(TestFioBankService.TestTransactionCount)) &&
        assert(result.errorMessage)(isNone)
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceNormal.layer,
      TransactionImportService.live
    ),

    test("should handle no transactions") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        result <- TransactionImportService.importTransactions(testAccountId, startDate, endDate).exit
      yield assert(result)(fails(isSubtype[NoTransactionsFound](anything)))
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceEmpty.layer,
      TransactionImportService.live
    ),

    test("should handle bank API errors") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        result <- TransactionImportService.importTransactions(testAccountId, startDate, endDate).exit
      yield assert(result)(fails(isSubtype[BankApiError](anything)))
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceError.layer,
      TransactionImportService.live
    )
  )

  // Suite for getting import status tests
  private val getImportStatusSuite = suite("getImportStatus")(
    test("should get import batch by ID") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        batch <- TransactionImportService.importTransactions(testAccountId, startDate, endDate)
        result <- TransactionImportService.getImportStatus(batch.id)
      yield assert(result.id)(equalTo(batch.id)) &&
        assert(result.status)(equalTo(ImportStatus.Completed))
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceNormal.layer,
      TransactionImportService.live
    ),

    test("should fail when batch ID doesn't exist") {
      for
        nonExistentId <- ZIO.succeed(ImportBatchId.generate())
        result <- TransactionImportService.getImportStatus(nonExistentId).exit
      yield assert(result)(fails(isSubtype[ImportBatchError](anything)))
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceNormal.layer,
      TransactionImportService.live
    )
  )

  // Suite for getting most recent import tests
  private val getMostRecentImportSuite = suite("getMostRecentImport")(
    test("should return None when no imports exist") {
      for
        // Using a new account ID that has no imports
        result <- TransactionImportService.getMostRecentImport(AccountId("fio", "new-account"))
      yield assert(result)(isNone)
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceNormal.layer,
      TransactionImportService.live
    ),

    test("should return the most recent import") {
      for
        today <- ZIO.succeed(LocalDate.now)
        
        // Create an older import with a custom service that always succeeds
        olderStartDate = today.minusDays(30)
        olderEndDate = today.minusDays(20)
        
        // Get an import service with the older transactions
        importService <- ZIO.service[TransactionImportService]
        olderBatch <- importService.importTransactions(testAccountId, olderStartDate, olderEndDate)
        
        // Create a newer import
        newerStartDate = today.minusDays(10)
        newerEndDate = today
        newerBatch <- importService.importTransactions(testAccountId, newerStartDate, newerEndDate)
        
        // Get most recent import
        result <- importService.getMostRecentImport(testAccountId)
      yield assert(result)(isSome(equalTo(newerBatch)))
    }.provide(
      InMemoryTransactionRepository.layer,
      InMemoryImportBatchRepository.layer,
      FioBankServiceNormal.layer,
      TransactionImportService.live
    )
  )

/** Shared constants for test FioBankService implementations.
  */
object TestFioBankService:
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

/** Normal implementation of FioBankService for successful test cases.
  */
object FioBankServiceNormal:
  val layer: ULayer[FioBankService] = ZLayer.succeed(
    new FioBankService {
      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.succeed(TestFioBankService.generateTransactions(accountId, startDate, TestFioBankService.TestTransactionCount))
    }
  )

/** FioBankService implementation that simulates empty transaction lists.
  */
object FioBankServiceEmpty:
  val layer: ULayer[FioBankService] = ZLayer.succeed(
    new FioBankService {
      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.succeed(List.empty)
    }
  )

/** FioBankService implementation that simulates errors.
  */
object FioBankServiceError:
  val layer: ULayer[FioBankService] = ZLayer.succeed(
    new FioBankService {
      override def fetchTransactions(
          accountId: AccountId,
          startDate: LocalDate,
          endDate: LocalDate
      ): ZIO[Any, Throwable, List[Transaction]] =
        ZIO.fail(new RuntimeException("Simulated bank API error"))
    }
  )