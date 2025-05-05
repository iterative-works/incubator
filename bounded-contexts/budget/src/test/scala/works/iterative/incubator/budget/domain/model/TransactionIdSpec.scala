package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*

object TransactionIdSpec extends ZIOSpecDefault:
    def spec = suite("TransactionId")(
        test("create should create TransactionId with valid inputs"):
            val sourceAccountId = "bank1-account123"
            val bankTransactionId = "tx987654"
            val id = TransactionId(sourceAccountId, bankTransactionId)
            assertTrue(
                id.sourceAccountId == sourceAccountId &&
                id.bankTransactionId == bankTransactionId &&
                id.value == s"$sourceAccountId-$bankTransactionId"
            ),

        test("create should reject null sourceAccountId"):
            try
                // Using _ to indicate we don't care about the result
                val _ = TransactionId(null, "tx123")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Source account ID must not be null")
                    ),

        test("create should reject null bankTransactionId"):
            try
                val _ = TransactionId("bank1-123", null)
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank transaction ID must not be null")
                    ),

        test("create should reject empty sourceAccountId"):
            try
                val _ = TransactionId("", "tx123")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Source account ID must not be empty")
                    ),

        test("create should reject empty bankTransactionId"):
            try
                val _ = TransactionId("bank1-123", "")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank transaction ID must not be empty")
                    ),

        test("generate should create valid TransactionId"):
            val id = TransactionId.generate()
            assertTrue(
                id.sourceAccountId.nonEmpty &&
                id.bankTransactionId.nonEmpty &&
                id.value.contains("-")
            ),

        test("fromString should parse valid composite ID"):
            val sourceAccountId = "bank1account123" // Use a simple ID without dashes
            val bankTransactionId = "tx987654"
            val idString = s"$sourceAccountId-$bankTransactionId"
            val result = TransactionId.fromString(idString)
            assertTrue(
                result.isRight &&
                result.toOption.get.sourceAccountId == sourceAccountId &&
                result.toOption.get.bankTransactionId == bankTransactionId
            ),

        test("fromString should reject null input"):
            val result = TransactionId.fromString(null)
            assertTrue(
                result.isLeft &&
                result.swap.toOption.get.contains("Transaction ID string must not be null or empty")
            ),

        test("fromString should reject empty input"):
            val result = TransactionId.fromString("")
            assertTrue(
                result.isLeft &&
                result.swap.toOption.get.contains("Transaction ID string must not be null or empty")
            ),

        test("fromString should reject invalid format"):
            val result = TransactionId.fromString("invalid")  // Missing required dash separator
            assertTrue(
                result.isLeft &&
                result.swap.toOption.get.contains("Invalid composite ID format")
            ),

        test("value method should combine sourceAccountId and bankTransactionId"):
            val sourceAccountId = "bank1-account123"
            val bankTransactionId = "tx987654"
            val id = TransactionId(sourceAccountId, bankTransactionId)
            assertTrue(
                id.value == s"$sourceAccountId-$bankTransactionId"
            ),

        test("toString should return the same as value"):
            val sourceAccountId = "bank1-account123"
            val bankTransactionId = "tx987654"
            val id = TransactionId(sourceAccountId, bankTransactionId)
            assertTrue(
                id.toString == id.value
            ),

        test("Different TransactionIds should not be equal"):
            val id1 = TransactionId("bank1-123", "tx1")
            val id2 = TransactionId("bank1-123", "tx2")
            val id3 = TransactionId("bank2-123", "tx1")
            assertTrue(
                id1 != id2 && id1 != id3 && id2 != id3
            ),

        test("Same TransactionIds should be equal"):
            val id1 = TransactionId("bank1-123", "tx1")
            val id2 = TransactionId("bank1-123", "tx1")
            assertTrue(
                id1 == id2
            )
    )