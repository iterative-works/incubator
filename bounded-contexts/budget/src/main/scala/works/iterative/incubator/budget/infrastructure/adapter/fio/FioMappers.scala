package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.FioModels.*
import java.time.{LocalDate, Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Currency
import zio.*

/** Utility for mapping between Fio API models and domain models.
  *
  * Provides functions for transforming Fio Bank transaction data into domain Transaction entities.
  *
  * Category: Mapper Layer: Infrastructure
  */
object FioMappers:
    // Date formatter for Fio date format (e.g. "2025-03-14+0100")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Parses a date string from Fio API format.
      *
      * @param dateString
      *   The date string in format "yyyy-MM-dd+XXXX" (timezone may be included)
      * @return
      *   A LocalDate if successful, or an error if parsing fails
      */
    def parseDate(dateString: String): Either[String, LocalDate] =
        try
            // Extract just the date part (ignore timezone for LocalDate)
            val datePart = dateString.split("\\+").head
            val parsed = LocalDate.parse(datePart, dateFormatter)
            Right(parsed)
        catch
            case e: Exception => Left(s"Failed to parse date '$dateString': ${e.getMessage}")

    /** Combines multiple reference fields (KS, VS, SS) into a single reference string.
      *
      * @param ks
      *   Constant symbol
      * @param vs
      *   Variable symbol
      * @param ss
      *   Specific symbol
      * @return
      *   A formatted reference string, or None if all fields are empty
      */
    def combineReferences(
        ks: Option[String],
        vs: Option[String],
        ss: Option[String]
    ): Option[String] =
        val references = Seq(
            ks.map(k => s"KS:$k"),
            vs.map(v => s"VS:$v"),
            ss.map(s => s"SS:$s")
        ).flatten

        if references.isEmpty then None else Some(references.mkString(", "))
    end combineReferences

    /** Creates a description by combining transaction type and comment.
      *
      * @param transactionType
      *   The type of the transaction
      * @param comment
      *   Optional comment for the transaction
      * @return
      *   A combined description string
      */
    def createDescription(transactionType: Option[String], comment: Option[String]): String =
        (transactionType, comment) match
            case (Some(tType), Some(comm)) => s"$tType - $comm"
            case (Some(tType), None)       => tType
            case (None, Some(comm))        => comm
            case _                         => "Unknown transaction"

    /** Extracts the counter account by combining the account number and bank code.
      *
      * @param accountNumber
      *   The counter account number
      * @param bankCode
      *   The bank code
      * @return
      *   A formatted counter account string, or None if account number is empty
      */
    def extractCounterAccount(
        accountNumber: Option[String],
        bankCode: Option[String]
    ): Option[String] =
        accountNumber.map { accNum =>
            bankCode match
                case Some(code) => s"$accNum/$code"
                case None       => accNum
        }

    /** Maps a Fio transaction to a domain Transaction entity.
      *
      * @param fioTransaction
      *   The Fio transaction to convert
      * @param accountId
      *   The source account ID for the transaction
      * @param importBatchId
      *   The import batch ID
      * @return
      *   Either a valid Transaction, or an error message
      */
    def mapToDomainTransaction(
        fioTransaction: FioTransaction,
        accountId: AccountId,
        importBatchId: ImportBatchId
    ): Either[String, Transaction] =
        // Extract required fields, failing if any required field is missing
        val dateResult = fioTransaction.column0
            .map(col => parseDate(col.value))
            .getOrElse(Left("Transaction date is missing"))

        val amountResult = fioTransaction.column1
            .map(col => Right(col.value))
            .getOrElse(Left("Transaction amount is missing"))

        val currencyResult = fioTransaction.column14
            .map(col =>
                try Right(Currency.getInstance(col.value))
                catch case e: Exception => Left(s"Invalid currency code: ${col.value}")
            )
            .getOrElse(Left("Transaction currency is missing"))

        val transactionIdResult = fioTransaction.column22
            .map(col => Right(col.value.toLong.toString))
            .getOrElse(Left("Transaction ID is missing"))

        // Combine date, amount, currency, and ID validation
        (dateResult, amountResult, currencyResult, transactionIdResult) match
            case (Right(date), Right(amount), Right(currency), Right(txId)) =>
                // Extract optional fields
                val counterAccountNumber = fioTransaction.column2.map(_.value)
                val bankCode = fioTransaction.column3.map(_.value)
                val ks = fioTransaction.column4.map(_.value)
                val vs = fioTransaction.column5.map(_.value)
                val ss = fioTransaction.column6.map(_.value)
                val transactionType = fioTransaction.column8.map(_.value)
                val counterparty = fioTransaction.column10.map(_.value)
                val comment = fioTransaction.column25.map(_.value)

                // Create derivative fields
                val counterAccount = extractCounterAccount(counterAccountNumber, bankCode)
                val reference = combineReferences(ks, vs, ss)
                val description = createDescription(transactionType, comment)

                // Create domain Transaction ID
                val domainTransactionId = TransactionId.create(accountId, txId) match
                    case Right(id) => id
                    case Left(err) =>
                        // In case of ID creation failure, generate a fallback ID
                        // This should not normally happen with valid data
                        TransactionId.generate()

                // Create the Money value
                val money = Money(amount, currency)

                // Create the Transaction entity
                Transaction.create(
                    id = domainTransactionId,
                    date = date,
                    amount = money,
                    description = description,
                    counterparty = counterparty,
                    counterAccount = counterAccount,
                    reference = reference,
                    importBatchId = importBatchId
                )

            case (Left(dateErr), _, _, _)     => Left(dateErr)
            case (_, Left(amountErr), _, _)   => Left(amountErr)
            case (_, _, Left(currencyErr), _) => Left(currencyErr)
            case (_, _, _, Left(idErr))       => Left(idErr)
        end match
    end mapToDomainTransaction
end FioMappers
