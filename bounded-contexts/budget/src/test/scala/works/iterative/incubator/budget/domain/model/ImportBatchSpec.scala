package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.{Instant, LocalDate}
// No UUID import needed

object ImportBatchSpec extends ZIOSpecDefault:
    // Sample data for testing
    private val accountId = AccountId("bank123", "account456")
    private val validStartDate = LocalDate.now().minusDays(30)
    private val validEndDate = LocalDate.now().minusDays(15)

    def spec = suite("ImportBatch")(
        // Factory method tests
        test("create should succeed with valid inputs") {
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(
                result.isRight,
                result.map(_.accountId).getOrElse(AccountId("test", "test")) == accountId,
                result.map(_.startDate).getOrElse(LocalDate.now) == validStartDate,
                result.map(_.endDate).getOrElse(LocalDate.now) == validEndDate,
                result.map(_.status).getOrElse(ImportStatus.Error) == ImportStatus.NotStarted,
                result.map(_.transactionCount).getOrElse(-1) == 0,
                result.map(_.errorMessage).getOrElse(Some("error")) == None,
                result.map(_.endTime).getOrElse(Some(Instant.now)) == None
            )
        },
        test("create should fail with null accountId") {
            val result = ImportBatch.create(
                accountId = null,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Account ID")
            )
        },
        test("create should fail with null startDate") {
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = null,
                endDate = validEndDate
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Start date")
            )
        },
        test("create should fail with null endDate") {
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = null
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("End date")
            )
        },
        test("create should fail when startDate is after endDate") {
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = validEndDate,
                endDate = validStartDate
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Start date cannot be after end date")
            )
        },
        test("create should fail with future startDate") {
            val futureDate = LocalDate.now().plusDays(1)
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = futureDate,
                endDate = futureDate.plusDays(5)
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("future")
            )
        },
        test("create should fail with future endDate") {
            val futureDate = LocalDate.now().plusDays(1)
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = futureDate
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("future")
            )
        },
        test("create should fail when date range exceeds max days") {
            val startDate = LocalDate.now().minusDays(100)
            val endDate = LocalDate.now().minusDays(1)
            val result = ImportBatch.create(
                accountId = accountId,
                startDate = startDate,
                endDate = endDate
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Date range cannot exceed")
            )
        },

        // Entity method tests
        test("markInProgress should update status for NotStarted batch") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    val originalTimestamp = importBatch.updatedAt
                    Thread.sleep(10)

                    val result = importBatch.markInProgress()

                    assertTrue(
                        result.isRight,
                        result.map(_.status).getOrElse(
                            ImportStatus.NotStarted
                        ) == ImportStatus.InProgress,
                        result.map(_.updatedAt.isAfter(originalTimestamp)).getOrElse(false)
                    )
                }
            }
        },
        test("markInProgress should fail for batches not in NotStarted status") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    val inProgressResult = importBatch.markInProgress()
                    assertTrue(inProgressResult.isRight) && {
                        val inProgress = inProgressResult.getOrElse(null)
                        assertTrue(inProgress != null) && {
                            val result = inProgress.markInProgress()

                            assertTrue(
                                result.isLeft,
                                result.left.getOrElse("").contains("Cannot start import")
                            )
                        }
                    }
                }
            }
        },
        test("markCompleted should update status, count, and timestamps for InProgress batch") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    val inProgressResult = importBatch.markInProgress()
                    assertTrue(inProgressResult.isRight) && {
                        val inProgress = inProgressResult.getOrElse(null)
                        assertTrue(inProgress != null) && {
                            val originalTimestamp = inProgress.updatedAt
                            Thread.sleep(10)

                            val result = inProgress.markCompleted(42)

                            assertTrue(
                                result.isRight,
                                result.map(_.status).getOrElse(
                                    ImportStatus.InProgress
                                ) == ImportStatus.Completed,
                                result.map(_.transactionCount).getOrElse(0) == 42,
                                result.map(_.endTime.isDefined).getOrElse(false),
                                result.map(_.updatedAt.isAfter(originalTimestamp)).getOrElse(false)
                            )
                        }
                    }
                }
            }
        },
        test("markCompleted should fail for batches not in InProgress status") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    val result = importBatch.markCompleted(42)

                    assertTrue(
                        result.isLeft,
                        result.left.getOrElse("").contains("Cannot complete import")
                    )
                }
            }
        },
        test("markFailed should update status, error message, and timestamps") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    val originalTimestamp = importBatch.updatedAt
                    Thread.sleep(10)

                    val result = importBatch.markFailed("Connection error")

                    assertTrue(
                        result.isRight,
                        result.map(_.status).getOrElse(
                            ImportStatus.NotStarted
                        ) == ImportStatus.Error,
                        result.map(_.errorMessage).getOrElse(None) == Some("Connection error"),
                        result.map(_.endTime.isDefined).getOrElse(false),
                        result.map(_.updatedAt.isAfter(originalTimestamp)).getOrElse(false)
                    )
                }
            }
        },
        test("completionTimeSeconds should calculate correct duration") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    // Batch with no end time
                    assert(importBatch.completionTimeSeconds)(isNone) && {
                        // Batch with end time
                        val startTime = Instant.now().minusSeconds(30)
                        val endTime = Instant.now()
                        val completedBatch = importBatch.copy(
                            startTime = startTime,
                            endTime = Some(endTime)
                        )

                        // Should be approximately 30 seconds, allow small difference due to code execution time
                        val seconds = completedBatch.completionTimeSeconds.getOrElse(0L)
                        assertTrue(seconds >= 29 && seconds <= 31)
                    }
                }
            }
        },
        test("isSuccess should return true when errorMessage is None") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    assertTrue(importBatch.isSuccess)
                }
            }
        },
        test("isSuccess should return false when errorMessage is defined") {
            val batchResult = ImportBatch.create(
                accountId = accountId,
                startDate = validStartDate,
                endDate = validEndDate
            )

            assertTrue(batchResult.isRight) && {
                val importBatch = batchResult.getOrElse(null)
                assertTrue(importBatch != null) && {
                    val failedResult = importBatch.markFailed("Connection error")
                    assertTrue(failedResult.isRight) && {
                        val failedBatch = failedResult.getOrElse(null)
                        assertTrue(failedBatch != null) && {
                            assertTrue(!failedBatch.isSuccess)
                        }
                    }
                }
            }
        }
    )
end ImportBatchSpec
