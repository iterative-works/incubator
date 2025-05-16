package works.iterative.incubator.budget.ui.transaction_import

import works.iterative.incubator.budget.domain.model.AccountId
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.infrastructure.adapter.*
import works.iterative.incubator.budget.ui.transaction_import.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import zio.*
import zio.test.*
import zio.test.Assertion.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.InMemoryFioAccountRepository

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
            yield assertTrue(
                viewModel.importStatus == ImportStatus.NotStarted,
                viewModel.startDate == LocalDate.now().withDayOfMonth(1),
                viewModel.endDate == LocalDate.now()
            )
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
            yield assertTrue(
                result.isLeft,
                result.left.toOption.flatMap(_.errors.get("dateRange")).isDefined
            )
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
            yield assertTrue(
                result.isLeft,
                result.left.toOption.flatMap(_.errors.get("accountId")).isDefined
            )
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
            yield assertTrue(
                result.isRight,
                result.toOption.map(_.transactionCount).getOrElse(
                    0
                ) == TestBankTransactionService.TestTransactionCount
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
        InMemoryTransactionRepository.layer,
        InMemoryImportBatchRepository.layer,
        FioBankServiceNormal.layer,
        TransactionImportService.live,
        TransactionImportPresenterLive.layer,
        InMemoryFioAccountRepository.layer
    )
end TransactionImportPresenterLiveSpec
