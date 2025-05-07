package works.iterative.incubator.budget.ui.transaction_import.models

import zio.test.*
import java.time.LocalDate
import scala.util.Success

/** Test suite for TransactionImportCommand.
  *
  * Tests the TransactionImportCommand model for proper serialization and parsing methods.
  */
class TransactionImportCommandSpec extends ZIOSpecDefault:
    def spec = suite("TransactionImportCommand")(
        test("should create command from form data") {
            // Create form data
            val formData = Map(
                "accountId" -> "0100-1234567890",
                "startDate" -> "2023-01-01",
                "endDate" -> "2023-01-31"
            )

            // Create command from form data
            val command = TransactionImportCommand.fromFormData(formData)

            assertTrue(
                command.accountId == "0100-1234567890",
                command.startDate == "2023-01-01",
                command.endDate == "2023-01-31"
            )
        },
        test("should handle missing form data") {
            // Create form data with missing fields
            val formData = Map[String, String]()

            // Create command from form data
            val command = TransactionImportCommand.fromFormData(formData)

            assertTrue(
                command.accountId.isEmpty,
                command.startDate.isEmpty,
                command.endDate.isEmpty
            )
        },
        test("should parse dates correctly") {
            // Create command with valid dates
            val command = TransactionImportCommand(
                accountId = "0100-1234567890",
                startDate = "2023-01-01",
                endDate = "2023-01-31"
            )

            // Parse dates
            val (startDateResult, endDateResult) = command.toLocalDates

            assertTrue(
                startDateResult.isSuccess,
                endDateResult.isSuccess,
                startDateResult.get == LocalDate.of(2023, 1, 1),
                endDateResult.get == LocalDate.of(2023, 1, 31)
            )
        },
        test("should handle invalid dates") {
            // Create command with invalid dates
            val command = TransactionImportCommand(
                accountId = "0100-1234567890",
                startDate = "invalid",
                endDate = "2023-01-31"
            )

            // Parse dates
            val (startDateResult, endDateResult) = command.toLocalDates

            assertTrue(
                startDateResult.isFailure,
                endDateResult.isSuccess,
                endDateResult.get == LocalDate.of(2023, 1, 31)
            )
        },
        test("should parse account ID correctly") {
            // Create command with valid account ID
            val command = TransactionImportCommand(
                accountId = "0100-1234567890",
                startDate = "2023-01-01",
                endDate = "2023-01-31"
            )

            // Parse account ID
            val accountIdResult = command.toAccountId

            assertTrue(
                accountIdResult.isRight,
                accountIdResult.toOption.exists(_.toString == "0100-1234567890")
            )
        },
        test("should handle invalid account ID") {
            // Create command with invalid account ID
            val command = TransactionImportCommand(
                accountId = "invalid",
                startDate = "2023-01-01",
                endDate = "2023-01-31"
            )

            // Parse account ID
            val accountIdResult = command.toAccountId

            assertTrue(
                accountIdResult.isLeft
            )
        },
        test("should replace null values with empty strings") {
            // Create command with null values
            val command = TransactionImportCommand(
                accountId = null,
                startDate = null,
                endDate = null
            )

            // Apply defaults
            val withDefaults = command.withDefaults

            assertTrue(
                withDefaults.accountId == "",
                withDefaults.startDate == "",
                withDefaults.endDate == ""
            )
        }
    )
end TransactionImportCommandSpec
