package works.iterative.incubator.budget.domain.model

import java.util.Currency

/** Represents a monetary amount with a specific currency.
  *
  * Money is immutable and provides operations for arithmetic with monetary amounts, ensuring
  * currency compatibility is maintained.
  *
  * Category: Value Object Layer: Domain
  */
case class Money(amount: BigDecimal, currency: Currency):
    require(amount != null, "Amount must not be null")
    require(currency != null, "Currency must not be null")

    /** Adds another Money value to this one.
      *
      * @param other
      *   The Money value to add
      * @return
      *   A new Money value representing the sum
      * @throws IllegalArgumentException
      *   if currencies don't match
      */
    def add(other: Money): Either[String, Money] =
        if currency != other.currency then
            Left(s"Cannot add money with different currencies: $currency and ${other.currency}")
        else
            Right(Money(amount + other.amount, currency))

    /** Subtracts another Money value from this one.
      *
      * @param other
      *   The Money value to subtract
      * @return
      *   A new Money value representing the difference
      * @throws IllegalArgumentException
      *   if currencies don't match
      */
    def subtract(other: Money): Either[String, Money] =
        if currency != other.currency then
            Left(
                s"Cannot subtract money with different currencies: $currency and ${other.currency}"
            )
        else
            Right(Money(amount - other.amount, currency))

    /** Multiplies this Money value by a factor.
      *
      * @param factor
      *   The factor to multiply by
      * @return
      *   A new Money value representing the product
      */
    def multiply(factor: BigDecimal): Money =
        Money(amount * factor, currency)

    /** Returns the negative of this Money value.
      *
      * @return
      *   A new Money value with the negative amount
      */
    def negate: Money = Money(-amount, currency)

    /** Checks if this Money value is zero.
      *
      * @return
      *   true if the amount is zero, false otherwise
      */
    def isZero: Boolean = amount == 0

    /** Checks if this Money value is positive.
      *
      * @return
      *   true if the amount is positive, false otherwise
      */
    def isPositive: Boolean = amount > 0

    /** Checks if this Money value is negative.
      *
      * @return
      *   true if the amount is negative, false otherwise
      */
    def isNegative: Boolean = amount < 0
end Money

object Money:
    /** Creates a Money value with zero amount in the specified currency.
      *
      * @param currency
      *   The currency of the money
      * @return
      *   A new Money value with zero amount
      */
    def zero(currency: Currency): Money = Money(BigDecimal(0), currency)

    /** Attempts to create a Money value from a string representation of the amount.
      *
      * @param s
      *   The string representation of the amount
      * @param currency
      *   The currency of the money
      * @return
      *   Either a Money value or an error message
      */
    def fromString(s: String, currency: Currency): Either[String, Money] =
        try
            val amount = BigDecimal(s)
            Right(Money(amount, currency))
        catch
            case _: NumberFormatException => Left(s"Invalid amount format: $s")
end Money
