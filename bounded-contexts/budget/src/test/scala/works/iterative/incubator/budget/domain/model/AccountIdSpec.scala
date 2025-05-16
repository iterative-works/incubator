package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*

object AccountIdSpec extends ZIOSpecDefault:
    def spec = suite("AccountId")(
        test("create should create AccountId with valid inputs"):
            val bankId = "bank123"
            val bankAccountId = "account456"
            val id = AccountId(bankId, bankAccountId)
            assertTrue(
                id.bankId == bankId &&
                    id.bankAccountId == bankAccountId &&
                    id.value == s"$bankAccountId/$bankId"
            )
        ,
        test("create should reject null bankId") {
            try
                val _ = AccountId(null, "account123")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank ID must not be null")
                    )
        },
        test("create should reject null bankAccountId") {
            try
                val _ = AccountId("bank123", null)
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank account ID must not be null")
                    )
        },
        test("create should reject empty bankId") {
            try
                val _ = AccountId("", "account123")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank ID must not be empty")
                    )
        },
        test("create should reject empty bankAccountId") {
            try
                val _ = AccountId("bank123", "")
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Bank account ID must not be empty")
                    )
        },
        test("constructor should create valid AccountId"):
            val bankId = "testbank"
            val accountId = "test123"
            val id = AccountId(bankId, accountId)
            assertTrue(
                id.bankId.nonEmpty &&
                    id.bankAccountId.nonEmpty &&
                    id.value.contains("/") &&
                    id.bankId == bankId &&
                    id.bankAccountId == accountId
            )
        ,
        test("fromString should parse valid composite ID"):
            val bankId = "bank123"
            val bankAccountId = "account456"
            val idString = s"$bankAccountId/$bankId"
            val result = AccountId.fromString(idString)
            assertTrue(
                result.isRight &&
                    result.toOption.get.bankId == bankId &&
                    result.toOption.get.bankAccountId == bankAccountId
            )
        ,
        test("fromString should reject null input"):
            val result = AccountId.fromString(null)
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains("Account ID string must not be null or empty")
            )
        ,
        test("fromString should reject empty input"):
            val result = AccountId.fromString("")
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains("Account ID string must not be null or empty")
            )
        ,
        test("fromString should reject invalid format"):
            val result = AccountId.fromString("invalid") // Missing required dash separator
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains("Invalid composite ID format")
            )
        ,
        test("value method should combine bankId and bankAccountId"):
            val bankId = "bank123"
            val bankAccountId = "account456"
            val id = AccountId(bankId, bankAccountId)
            assertTrue(
                id.value == s"$bankAccountId/$bankId"
            )
        ,
        test("toString should return the same as value"):
            val bankId = "bank123"
            val bankAccountId = "account456"
            val id = AccountId(bankId, bankAccountId)
            assertTrue(
                id.toString == id.value
            )
        ,
        test("Different AccountIds should not be equal"):
            val id1 = AccountId("bank1", "account1")
            val id2 = AccountId("bank1", "account2")
            val id3 = AccountId("bank2", "account1")
            assertTrue(
                id1 != id2 && id1 != id3 && id2 != id3
            )
        ,
        test("Same AccountIds should be equal"):
            val id1 = AccountId("bank1", "account1")
            val id2 = AccountId("bank1", "account1")
            assertTrue(
                id1 == id2
            )
    )
end AccountIdSpec
