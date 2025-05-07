package works.iterative.incubator.budget.application.service

import works.iterative.incubator.budget.domain.model.AccountId
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.infrastructure.adapter.*
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportPresenter
import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.LocalDate
import zio.*
import zio.test.*
import zio.test.Assertion.*

/** Test suite for TransactionImportPresenterLive.
  */
object TransactionImportPresenterLiveSpec extends ZIOSpecDefault:
  // Test account ID to use throughout tests
  private val testAccountId = AccountId("fio", "test-account")

  override def spec = suite("TransactionImportPresenterLive")(
    test("should get initial view model") {
      for
        viewModel <- TransactionImportPresenter.getImportViewModel()
      yield assert(viewModel.importStatus)(equalTo(ImportStatus.NotStarted)) &&
        assert(viewModel.startDate)(equalTo(LocalDate.now().withDayOfMonth(1))) &&
        assert(viewModel.endDate)(equalTo(LocalDate.now()))
    },

    test("should validate date range") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        validResult <- TransactionImportPresenter.validateDateRange(startDate, endDate)
        
        invalidStartDate = today.plusDays(1)
        invalidResult <- TransactionImportPresenter.validateDateRange(invalidStartDate, endDate)
      yield assert(validResult)(isRight(isUnit)) &&
        assert(invalidResult)(isLeft(anything))
    },

    test("should import transactions") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        result <- TransactionImportPresenter.importTransactions(testAccountId, startDate, endDate)
      yield assert(result.transactionCount)(equalTo(TestFioBankService.TestTransactionCount)) &&
        assert(result.errorMessage)(isNone) &&
        assert(result.endTime)(isSome(anything))
    },

    test("should get import status") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        _ <- TransactionImportPresenter.importTransactions(testAccountId, startDate, endDate)
        status <- TransactionImportPresenter.getImportStatus()
      yield assert(status)(equalTo(ImportStatus.Completed))
    }
  ).provide(
    ZLayer.succeed(testAccountId),
    InMemoryTransactionRepository.layer,
    InMemoryImportBatchRepository.layer,
    TestFioBankService.layer,
    TransactionImportService.live,
    TransactionImportPresenterLive.layer
  )