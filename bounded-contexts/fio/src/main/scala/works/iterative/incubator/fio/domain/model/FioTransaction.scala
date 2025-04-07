package works.iterative.incubator.fio.domain.model

/** Represents a transaction field with its value, name and id
  *
  * Classification: Domain Value Object
  */
case class FioTransactionValue[T](value: T, name: String, id: Int)

/** The main transaction object representing a financial transaction from Fio Bank API
  *
  * This is a direct representation of the transaction as it comes from the Fio Bank API.
  * It contains all the fields that Fio Bank provides for a transaction.
  *
  * Classification: Domain Entity (External API Model)
  */
case class FioTransaction(
    column22: Option[FioTransactionValue[Long]], // Transaction ID
    column0: Option[FioTransactionValue[String]], // Date
    column1: Option[FioTransactionValue[Double]], // Amount
    column14: Option[FioTransactionValue[String]], // Currency
    column2: Option[FioTransactionValue[String]], // Counter account
    column10: Option[FioTransactionValue[String]], // Counter account name
    column3: Option[FioTransactionValue[String]], // Bank code
    column12: Option[FioTransactionValue[String]], // Bank name
    column4: Option[FioTransactionValue[String]], // KS (constant symbol)
    column5: Option[FioTransactionValue[String]], // VS (variable symbol)
    column6: Option[FioTransactionValue[String]], // SS (specific symbol)
    column7: Option[FioTransactionValue[String]], // User identification
    column8: Option[FioTransactionValue[String]], // Transaction type
    column9: Option[FioTransactionValue[String]], // Additional info
    column16: Option[FioTransactionValue[String]], // Additional info
    column17: Option[FioTransactionValue[Long]], // Order ID
    column18: Option[FioTransactionValue[String]], // Additional info
    column25: Option[FioTransactionValue[String]], // Comment
    column26: Option[FioTransactionValue[String]], // Additional info
    column27: Option[FioTransactionValue[String]] // Additional info
)

/** List of transactions from Fio Bank API
  *
  * Classification: Domain Value Object
  */
case class FioTransactionList(transaction: List[FioTransaction])

/** Information about the bank account and statement
  *
  * Classification: Domain Value Object
  */
case class FioStatementInfo(
    accountId: String,
    bankId: String,
    currency: String,
    iban: String,
    bic: String,
    openingBalance: Double,
    closingBalance: Double,
    dateStart: String,
    dateEnd: String,
    yearList: Option[String],
    idList: Option[String],
    idFrom: Long,
    idTo: Long,
    idLastDownload: Option[String]
)

/** Account statement from Fio Bank API
  *
  * Classification: Domain Value Object
  */
case class FioAccountStatement(
    info: FioStatementInfo,
    transactionList: FioTransactionList
)

/** Root response object from Fio Bank API
  *
  * Classification: Domain Value Object
  */
case class FioResponse(
    accountStatement: FioAccountStatement
)