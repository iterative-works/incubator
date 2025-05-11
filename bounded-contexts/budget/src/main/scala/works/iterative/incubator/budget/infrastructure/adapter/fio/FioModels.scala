package works.iterative.incubator.budget.infrastructure.adapter.fio

import zio.json.*

/** Data models for the Fio Bank API responses.
  *
  * These models match the structure of the JSON responses from the Fio Bank API.
  *
  * Category: Data Transfer Objects Layer: Infrastructure
  */
object FioModels:
    /** Root response structure from Fio Bank API.
      *
      * @param accountStatement
      *   The account statement containing transaction data
      */
    case class FioResponse(
        accountStatement: AccountStatement
    )

    object FioResponse:
        given JsonDecoder[FioResponse] = DeriveJsonDecoder.gen[FioResponse]

    /** Account statement containing account information and transactions.
      *
      * @param info
      *   General information about the account and statement
      * @param transactionList
      *   List of transactions in the statement
      */
    case class AccountStatement(
        info: AccountInfo,
        transactionList: TransactionList
    )

    object AccountStatement:
        given JsonDecoder[AccountStatement] = DeriveJsonDecoder.gen[AccountStatement]

    /** Account information section.
      *
      * @param accountId
      *   Bank account identifier
      * @param bankId
      *   Bank identifier
      * @param currency
      *   Account currency code
      * @param iban
      *   International Bank Account Number
      * @param bic
      *   Bank Identifier Code
      * @param openingBalance
      *   Opening balance for the statement period
      * @param closingBalance
      *   Closing balance for the statement period
      * @param dateStart
      *   Start date of the statement period
      * @param dateEnd
      *   End date of the statement period
      */
    case class AccountInfo(
        accountId: String,
        bankId: String,
        currency: String,
        iban: String,
        bic: Option[String],
        openingBalance: BigDecimal,
        closingBalance: BigDecimal,
        dateStart: String,
        dateEnd: String
    )

    object AccountInfo:
        given JsonDecoder[AccountInfo] = DeriveJsonDecoder.gen[AccountInfo]

    /** Container for the list of transactions.
      *
      * @param transaction
      *   Array of transaction objects
      */
    case class TransactionList(
        transaction: List[FioTransaction]
    )

    object TransactionList:
        given JsonDecoder[TransactionList] = DeriveJsonDecoder.gen[TransactionList]

    /** Represents a single transaction from Fio Bank.
      *
      * Each field is represented as a column with ID, name, and value.
      *
      * @param column0
      *   Date (Datum)
      * @param column1
      *   Amount (Objem)
      * @param column2
      *   Counter Account (Protiúčet)
      * @param column3
      *   Bank Code (Kód banky)
      * @param column4
      *   KS - Constant Symbol
      * @param column5
      *   VS - Variable Symbol
      * @param column6
      *   SS - Specific Symbol
      * @param column7
      *   User Identification
      * @param column8
      *   Type (Typ)
      * @param column10
      *   Counter Account Name (Název protiúčtu)
      * @param column12
      *   Bank Name (Název banky)
      * @param column14
      *   Currency (Měna)
      * @param column17
      *   Instruction ID (ID pokynu)
      * @param column22
      *   Transaction ID (ID pohybu)
      * @param column25
      *   Comment (Komentář)
      * @param column26
      *   BIC
      */
    case class FioTransaction(
        column0: Option[Column[String]], // Date
        column1: Option[Column[BigDecimal]], // Amount
        column2: Option[Column[String]], // Counter Account
        column3: Option[Column[String]], // Bank Code
        column4: Option[Column[String]], // KS
        column5: Option[Column[String]], // VS
        column6: Option[Column[String]], // SS
        column7: Option[Column[String]], // User Identification
        column8: Option[Column[String]], // Type
        column10: Option[Column[String]], // Counter Account Name
        column12: Option[Column[String]], // Bank Name
        column14: Option[Column[String]], // Currency
        column17: Option[Column[BigDecimal]], // Instruction ID
        column22: Option[Column[BigDecimal]], // Transaction ID
        column25: Option[Column[String]], // Comment
        column26: Option[Column[String]] // BIC
    )

    object FioTransaction:
        given JsonDecoder[FioTransaction] = DeriveJsonDecoder.gen[FioTransaction]

    /** Generic column structure used in Fio transactions.
      *
      * @param id
      *   Column identifier
      * @param name
      *   Human-readable column name
      * @param value
      *   The column value (can be String, BigDecimal, etc.)
      */
    case class Column[T](
        id: Int,
        name: String,
        value: T
    )

    object Column:
        given [T: JsonDecoder]: JsonDecoder[Column[T]] = DeriveJsonDecoder.gen[Column[T]]

    /** Error response from Fio API.
      *
      * @param error
      *   Error message
      * @param description
      *   Detailed error description
      * @param code
      *   Error code
      */
    case class FioError(
        error: String,
        description: Option[String],
        code: Option[Int]
    )

    object FioError:
        given JsonDecoder[FioError] = DeriveJsonDecoder.gen[FioError]
end FioModels
