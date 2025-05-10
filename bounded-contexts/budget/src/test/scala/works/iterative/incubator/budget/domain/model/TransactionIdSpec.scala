package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*

object TransactionIdSpec extends ZIOSpecDefault:
    def spec = suite("TransactionId")(
        test("create should create TransactionId with valid inputs"):
            val accountId = AccountId("bank1", "account123")
            val bankTransactionId = "tx987654"
            val id = TransactionId(accountId, bankTransactionId)
            assertTrue(
                id.sourceAccount == accountId &&
                id.bankTransactionId == bankTransactionId &&
                id.value == s"${accountId.toString}-$bankTransactionId"
            ),

        test("create should reject null sourceAccount"):
            try
                // Using _ to indicate we don't care about the result
                val _ = TransactionId(null, "tx123")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Source account must not be null")
                    ),

        test("create should reject null bankTransactionId"):
            try
                val accountId = AccountId("bank1", "account123")
                val _ = TransactionId(accountId, null)
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank transaction ID must not be null")
                    ),

        test("create should reject empty bankTransactionId"):
            try
                val accountId = AccountId("bank1", "account123")
                val _ = TransactionId(accountId, "")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank transaction ID must not be empty")
                    ),

        test("random transaction ID should be valid"):
            import works.iterative.incubator.budget.infrastructure.adapter.MockBankTransactionService
            val id = MockBankTransactionService.generateRandomTransactionId()
            assertTrue(
                id.sourceAccount != null &&
                id.bankTransactionId.nonEmpty &&
                id.value.contains("-")
            ),

        test("fromString should parse valid composite ID in new format"):
            val bankId = "bank1"
            val accountId = "account123"
            val bankTransactionId = "tx987654"
            val idString = s"$bankId-$accountId-$bankTransactionId"
            val result = TransactionId.fromString(idString)
            assertTrue(
                result.isRight &&
                result.toOption.get.sourceAccount.bankId == bankId &&
                result.toOption.get.sourceAccount.bankAccountId == accountId &&
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

        test("value method should combine sourceAccount and bankTransactionId"):
            val accountId = AccountId("bank1", "account123")
            val bankTransactionId = "tx987654"
            val id = TransactionId(accountId, bankTransactionId)
            assertTrue(
                id.value == s"${accountId.toString}-$bankTransactionId"
            ),

        test("toString should return the same as value"):
            val accountId = AccountId("bank1", "account123")
            val bankTransactionId = "tx987654"
            val id = TransactionId(accountId, bankTransactionId)
            assertTrue(
                id.toString == id.value
            ),

        test("Different TransactionIds should not be equal"):
            val accountId1 = AccountId("bank1", "account123")
            val accountId2 = AccountId("bank2", "account123")
            val id1 = TransactionId(accountId1, "tx1")
            val id2 = TransactionId(accountId1, "tx2")
            val id3 = TransactionId(accountId2, "tx1")
            assertTrue(
                id1 != id2 && id1 != id3 && id2 != id3
            ),

        test("Same TransactionIds should be equal"):
            val accountId = AccountId("bank1", "account123")
            val id1 = TransactionId(accountId, "tx1")
            val id2 = TransactionId(accountId, "tx1")
            assertTrue(
                id1 == id2
            )
    )
