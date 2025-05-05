package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.time.{Instant, LocalDate}
import java.util.Currency

object TransactionSpec extends ZIOSpecDefault:
    // Sample data for testing
    private val accountId = AccountId("bank123", "account456")
    private val importBatchId = ImportBatchId("bank123-account456", 1L)
    private val transactionDate = LocalDate.of(2025, 3, 15)
    private val czk = Currency.getInstance("CZK")
    private val amount = Money(BigDecimal(-1250), czk)

    def spec = suite("Transaction")(
        // Factory method tests
        test("create should succeed with valid inputs") {
            val result = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                counterparty = Some("Albert"),
                importBatchId = importBatchId
            )

            assertTrue(
                result.isRight,
                result.map(_.accountId).getOrElse(AccountId("test", "test")) == accountId,
                result.map(_.date).getOrElse(LocalDate.now) == transactionDate,
                result.map(_.amount).getOrElse(Money.zero(czk)) == amount,
                result.map(_.description).getOrElse("") == "Groceries",
                result.map(_.counterparty).getOrElse(None) == Some("Albert"),
                result.map(_.status).getOrElse(
                    TransactionStatus.Failed
                ) == TransactionStatus.Imported
            )
        },
        test("create should fail with null accountId") {
            val result = Transaction.create(
                accountId = null,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Account ID")
            )
        },
        test("create should fail with null date") {
            val result = Transaction.create(
                accountId = accountId,
                date = null,
                amount = amount,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Date")
            )
        },
        test("create should fail with future date") {
            val futureDate = LocalDate.now().plusDays(1)
            val result = Transaction.create(
                accountId = accountId,
                date = futureDate,
                amount = amount,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("future")
            )
        },
        test("create should fail with null amount") {
            val result = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = null,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Amount")
            )
        },
        test("create should fail with empty description") {
            val result = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "",
                importBatchId = importBatchId
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Description")
            )
        },
        test("create should fail with null importBatchId") {
            val result = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                importBatchId = null
            )

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Import batch ID")
            )
        },
        test("create should trim description and optional strings") {
            val result = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "  Groceries  ",
                counterparty = Some("  Albert  "),
                counterAccount = Some("  123456  "),
                reference = Some("  VS123  "),
                importBatchId = importBatchId
            )

            assertTrue(
                result.isRight,
                result.map(_.description).getOrElse("") == "Groceries",
                result.map(_.counterparty).getOrElse(None) == Some("Albert"),
                result.map(_.counterAccount).getOrElse(None) == Some("123456"),
                result.map(_.reference).getOrElse(None) == Some("VS123")
            )
        },
        test("create should filter out empty optional strings") {
            val result = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                counterparty = Some(""),
                counterAccount = Some(" "),
                reference = Some(""),
                importBatchId = importBatchId
            )

            assertTrue(
                result.isRight,
                result.map(_.counterparty).getOrElse(Some("x")) == None,
                result.map(_.counterAccount).getOrElse(Some("x")) == None,
                result.map(_.reference).getOrElse(Some("x")) == None
            )
        },

        // Entity method tests
        test("updateStatus should change status and update timestamp") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    val originalTimestamp = transaction.updatedAt
                    // Small delay to ensure timestamp differs
                    Thread.sleep(10)

                    val updated = transaction.updateStatus(TransactionStatus.Categorized)

                    assertTrue(
                        updated.status == TransactionStatus.Categorized,
                        updated.updatedAt.isAfter(originalTimestamp)
                    )
                }
            }
        },
        test("updateDescription should update description and timestamp with valid input") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    val originalTimestamp = transaction.updatedAt
                    Thread.sleep(10)

                    val result = transaction.updateDescription("Supermarket shopping")

                    assertTrue(
                        result.isRight,
                        result.map(_.description).getOrElse("") == "Supermarket shopping",
                        result.map(_.updatedAt.isAfter(originalTimestamp)).getOrElse(false)
                    )
                }
            }
        },
        test("updateDescription should fail with empty description") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    val result = transaction.updateDescription("")

                    assertTrue(
                        result.isLeft,
                        result.left.getOrElse("").contains("empty")
                    )
                }
            }
        },
        test("updateCounterparty should update counterparty and timestamp") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                counterparty = None,
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    val originalTimestamp = transaction.updatedAt
                    Thread.sleep(10)

                    val updated = transaction.updateCounterparty(Some("Albert"))

                    assertTrue(
                        updated.counterparty == Some("Albert"),
                        updated.updatedAt.isAfter(originalTimestamp)
                    )
                }
            }
        },
        test("updateCounterparty should set to None for empty string") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = amount,
                description = "Groceries",
                counterparty = Some("Albert"),
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    val updated = transaction.updateCounterparty(Some(""))

                    assertTrue(updated.counterparty.isEmpty)
                }
            }
        },
        test("isExpense should return true for negative amounts") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = Money(BigDecimal(-100), czk),
                description = "Groceries",
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    assertTrue(transaction.isExpense)
                }
            }
        },
        test("isIncome should return true for positive amounts") {
            val transactionResult = Transaction.create(
                accountId = accountId,
                date = transactionDate,
                amount = Money(BigDecimal(100), czk),
                description = "Salary",
                importBatchId = importBatchId
            )

            assertTrue(transactionResult.isRight) && {
                val transaction = transactionResult.getOrElse(null)
                assertTrue(transaction != null) && {
                    assertTrue(transaction.isIncome)
                }
            }
        }
    )
end TransactionSpec
