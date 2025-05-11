package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*

object ImportBatchIdSpec extends ZIOSpecDefault:
    def spec = suite("ImportBatchId")(
        test("create should create ImportBatchId with valid inputs"):
            val accountId = "bank1-account123"
            val sequenceNumber = 42L
            val id = ImportBatchId(accountId, sequenceNumber)
            assertTrue(
                id.accountId == accountId &&
                    id.sequenceNumber == sequenceNumber &&
                    id.value == s"$accountId-$sequenceNumber"
            )
        ,
        test("create should reject null accountId") {
            try
                val _ = ImportBatchId(null, 1L)
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Account ID must not be null")
                    )
        },
        test("create should reject empty accountId") {
            try
                val _ = ImportBatchId("", 1L)
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Account ID must not be empty")
                    )
        },
        test("create should reject non-positive sequenceNumber") {
            try
                val _ = ImportBatchId("bank1-account123", 0L)
                assertNever("Should have thrown exception but didn't")
            catch
                case e: IllegalArgumentException =>
                    assertTrue(
                        e.getMessage.contains("Sequence number must be positive")
                    )
        },
        test("constructor should create valid ImportBatchId"):
            val accountId = "bank1-account123"
            val sequenceNumber = 1L
            val id = ImportBatchId(accountId, sequenceNumber)
            assertTrue(
                id.accountId == accountId &&
                    id.sequenceNumber == sequenceNumber
            )
        ,
        test("manually created IDs with sequential numbers should look like incrementing sequence"):
            val accountId = "bank1-account123"
            val id1 = ImportBatchId(accountId, 1L)
            val id2 = ImportBatchId(accountId, 2L)
            val id3 = ImportBatchId(accountId, 3L)
            assertTrue(
                id1.sequenceNumber < id2.sequenceNumber &&
                    id2.sequenceNumber < id3.sequenceNumber
            )
        ,
        test("fromString should parse valid composite ID"):
            val accountId = "bank1account123" // Use a simple ID without dashes
            val sequenceNumber = 42L
            val idString = s"$accountId-$sequenceNumber"
            val result = ImportBatchId.fromString(idString)
            assertTrue(
                result.isRight &&
                    result.toOption.get.accountId == accountId &&
                    result.toOption.get.sequenceNumber == sequenceNumber
            )
        ,
        test("fromString should reject null input"):
            val result = ImportBatchId.fromString(null)
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains(
                        "Import batch ID string must not be null or empty"
                    )
            )
        ,
        test("fromString should reject empty input"):
            val result = ImportBatchId.fromString("")
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains(
                        "Import batch ID string must not be null or empty"
                    )
            )
        ,
        test("fromString should reject invalid format"):
            val result = ImportBatchId.fromString("invalid") // Missing required dash separator
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains("Invalid composite ID format")
            )
        ,
        test("fromString should reject non-numeric sequence number"):
            val result = ImportBatchId.fromString("account123-abc")
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains("Invalid sequence number format")
            )
        ,
        test("fromString should reject non-positive sequence number"):
            val result = ImportBatchId.fromString("account123-0")
            assertTrue(
                result.isLeft &&
                    result.swap.toOption.get.contains("Sequence number must be positive")
            )
        ,
        test("value method should combine accountId and sequenceNumber"):
            val accountId = "bank1-account123"
            val sequenceNumber = 42L
            val id = ImportBatchId(accountId, sequenceNumber)
            assertTrue(
                id.value == s"$accountId-$sequenceNumber"
            )
        ,
        test("toString should return the same as value"):
            val accountId = "bank1-account123"
            val sequenceNumber = 42L
            val id = ImportBatchId(accountId, sequenceNumber)
            assertTrue(
                id.toString == id.value
            )
        ,
        test("Different ImportBatchIds should not be equal"):
            val id1 = ImportBatchId("account1", 1L)
            val id2 = ImportBatchId("account1", 2L)
            val id3 = ImportBatchId("account2", 1L)
            assertTrue(
                id1 != id2 && id1 != id3 && id2 != id3
            )
        ,
        test("Same ImportBatchIds should be equal"):
            val id1 = ImportBatchId("account1", 1L)
            val id2 = ImportBatchId("account1", 1L)
            assertTrue(
                id1 == id2
            )
    )
end ImportBatchIdSpec
