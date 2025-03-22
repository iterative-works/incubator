package works.iterative.incubator.transactions

import java.time.{Instant, LocalDate}

/** Query parameters for filtering transactions
  *
  * This class provides a flexible way to query transactions based on various criteria.
  */
case class TransactionQuery(
    id: Option[TransactionId] = None,
    sourceAccountId: Option[Long] = None,
    dateFrom: Option[LocalDate] = None,
    dateTo: Option[LocalDate] = None,
    amountMin: Option[BigDecimal] = None,
    amountMax: Option[BigDecimal] = None,
    currency: Option[String] = None,
    counterAccount: Option[String] = None,
    variableSymbol: Option[String] = None,
    importedAfter: Option[Instant] = None,
    importedBefore: Option[Instant] = None
)
