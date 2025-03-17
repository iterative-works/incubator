package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

// Represents a transaction field with its value, name and id
case class FioTransactionValue[T](value: T, name: String, id: Int)

// The main transaction object
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

case class FioTransactionList(transaction: List[FioTransaction])

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

case class FioAccountStatement(
    info: FioStatementInfo,
    transactionList: FioTransactionList
)

case class FioResponse(
    accountStatement: FioAccountStatement
)
