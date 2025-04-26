package works.iterative.incubator.budget.domain.query

import java.time.{Instant, LocalDate}
import works.iterative.incubator.budget.domain.model.TransactionId

/** Query parameters for filtering transactions
  *
  * This class provides a flexible way to query transactions based on various criteria.
  *
  * Classification: Domain Query Object
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
