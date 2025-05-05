package works.iterative.incubator.budget.domain.model

import zio.*
import zio.test.*
import zio.test.Assertion.*
import java.util.Currency

object MoneySpec extends ZIOSpecDefault:
    // Currencies for testing
    private val czk = Currency.getInstance("CZK")
    private val usd = Currency.getInstance("USD")

    def spec = suite("Money")(
        // Creation tests
        test("should create Money with valid amount and currency") {
            val money = Money(BigDecimal(100), czk)
            assertTrue(
                money.amount == BigDecimal(100),
                money.currency == czk
            )
        },
        test("should reject null amount") {
            val effect = ZIO.attempt {
                Money(null, czk)
            }
            assertZIO(effect.exit)(fails(isSubtype[IllegalArgumentException](anything)))
        },
        test("should reject null currency") {
            val effect = ZIO.attempt {
                Money(BigDecimal(100), null)
            }
            assertZIO(effect.exit)(fails(isSubtype[IllegalArgumentException](anything)))
        },
        test("Money.zero should create zero amount for given currency") {
            val money = Money.zero(czk)
            assertTrue(
                money.amount == BigDecimal(0),
                money.currency == czk,
                money.isZero
            )
        },
        test("Money.fromString should parse valid amount string") {
            val result = Money.fromString("123.45", czk)
            assertTrue(
                result.isRight,
                result.getOrElse(Money.zero(czk)).amount == BigDecimal("123.45")
            )
        },
        test("Money.fromString should fail on invalid amount string") {
            val result = Money.fromString("abc", czk)
            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("Invalid amount format")
            )
        },

        // Operations tests
        test("add should combine amounts with same currency") {
            val m1 = Money(BigDecimal(100), czk)
            val m2 = Money(BigDecimal(50), czk)
            val result = m1.add(m2)

            assertTrue(
                result.isRight,
                result.getOrElse(Money.zero(czk)).amount == BigDecimal(150)
            )
        },
        test("add should reject different currencies") {
            val m1 = Money(BigDecimal(100), czk)
            val m2 = Money(BigDecimal(50), usd)
            val result = m1.add(m2)

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("different currencies")
            )
        },
        test("subtract should reduce amounts with same currency") {
            val m1 = Money(BigDecimal(100), czk)
            val m2 = Money(BigDecimal(30), czk)
            val result = m1.subtract(m2)

            assertTrue(
                result.isRight,
                result.getOrElse(Money.zero(czk)).amount == BigDecimal(70)
            )
        },
        test("subtract should reject different currencies") {
            val m1 = Money(BigDecimal(100), czk)
            val m2 = Money(BigDecimal(30), usd)
            val result = m1.subtract(m2)

            assertTrue(
                result.isLeft,
                result.left.getOrElse("").contains("different currencies")
            )
        },
        test("multiply should scale the amount by a factor") {
            val money = Money(BigDecimal(100), czk)
            val result = money.multiply(BigDecimal("1.5"))

            assertTrue(
                result.amount == BigDecimal(150),
                result.currency == czk
            )
        },
        test("negate should invert the amount sign") {
            val money = Money(BigDecimal(100), czk)
            val result = money.negate

            assertTrue(
                result.amount == BigDecimal(-100),
                result.currency == czk
            )
        },

        // State tests
        test("isZero should return true for zero amount") {
            val money = Money(BigDecimal(0), czk)
            assertTrue(money.isZero)
        },
        test("isZero should return false for non-zero amount") {
            val money = Money(BigDecimal(1), czk)
            assertTrue(!money.isZero)
        },
        test("isPositive should return true for positive amount") {
            val money = Money(BigDecimal(5), czk)
            assertTrue(money.isPositive)
        },
        test("isPositive should return false for zero or negative amount") {
            val zero = Money(BigDecimal(0), czk)
            val negative = Money(BigDecimal(-5), czk)
            assertTrue(
                !zero.isPositive,
                !negative.isPositive
            )
        },
        test("isNegative should return true for negative amount") {
            val money = Money(BigDecimal(-5), czk)
            assertTrue(money.isNegative)
        },
        test("isNegative should return false for zero or positive amount") {
            val zero = Money(BigDecimal(0), czk)
            val positive = Money(BigDecimal(5), czk)
            assertTrue(
                !zero.isNegative,
                !positive.isNegative
            )
        }
    )
end MoneySpec
