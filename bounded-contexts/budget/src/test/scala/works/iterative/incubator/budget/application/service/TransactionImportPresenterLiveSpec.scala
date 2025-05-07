package works.iterative.incubator.budget.application.service

import works.iterative.incubator.budget.domain.model.AccountId
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.infrastructure.adapter.*
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportPresenter
import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import zio.*
import zio.test.*
import zio.test.Assertion.*

/** Test suite for TransactionImportPresenterLive.
  */
object TransactionImportPresenterLiveSpec extends ZIOSpecDefault:
  // Test account ID to use throughout tests
  private val testAccountId = AccountId("fio", "test-account")
  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

  override def spec = suite("TransactionImportPresenterLive")(
    test("should get initial view model") {
      for
        viewModel <- TransactionImportPresenter.getImportViewModel()
      yield assert(viewModel.importStatus)(equalTo(ImportStatus.NotStarted)) &&
        assert(viewModel.startDate)(equalTo(LocalDate.now().withDayOfMonth(1))) &&
        assert(viewModel.endDate)(equalTo(LocalDate.now()))
    },

    test("should validate form with valid data") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        command = TransactionImportCommand(
          accountId = testAccountId.toString,
          startDate = startDate.format(formatter),
          endDate = endDate.format(formatter)
        )
        result <- TransactionImportPresenter.validateAndProcess(command)
      yield assert(result.isRight)(isTrue)
    },

    test("should validate form with invalid date range") {
      for
        today <- ZIO.succeed(LocalDate.now)
        invalidStartDate = today.plusDays(1)
        endDate = today
        command = TransactionImportCommand(
          accountId = testAccountId.toString,
          startDate = invalidStartDate.format(formatter),
          endDate = endDate.format(formatter)
        )
        result <- TransactionImportPresenter.validateAndProcess(command)
      yield {
        assert(result.isLeft)(isTrue) &&
        assert(result.left.toOption.flatMap(_.errors.get("dateRange")).isDefined)(isTrue)
      }
    },
    
    test("should validate form with invalid account id") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        command = TransactionImportCommand(
          accountId = "This is definitely not a valid account ID",
          startDate = startDate.format(formatter),
          endDate = endDate.format(formatter)
        )
        result <- TransactionImportPresenter.validateAndProcess(command)
      yield {
        assert(result.isLeft)(isTrue) &&
        assert(result.left.toOption.flatMap(_.errors.get("accountId")).isDefined)(isTrue)
      }
    },

    test("should process import with valid data") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        command = TransactionImportCommand(
          accountId = testAccountId.toString,
          startDate = startDate.format(formatter),
          endDate = endDate.format(formatter)
        )
        result <- TransactionImportPresenter.validateAndProcess(command)
      yield assert(result.isRight)(isTrue) &&
        assert(result.toOption.map(_.transactionCount).getOrElse(0))(
          equalTo(TestFioBankService.TestTransactionCount)
        )
    },

    test("should get import status") {
      for
        today <- ZIO.succeed(LocalDate.now)
        startDate = today.minusDays(7)
        endDate = today
        command = TransactionImportCommand(
          accountId = testAccountId.toString,
          startDate = startDate.format(formatter),
          endDate = endDate.format(formatter)
        )
        _ <- TransactionImportPresenter.validateAndProcess(command)
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