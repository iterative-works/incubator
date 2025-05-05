package works.iterative.incubator.budget.domain.model

import java.time.{Instant, LocalDate}

/** Represents a financial transaction in the system.
  *
  * @param id
  *   Unique identifier for the transaction
  * @param accountId
  *   ID of the source account for the transaction
  * @param date
  *   Date when the transaction occurred
  * @param amount
  *   Money amount of the transaction (positive for income, negative for expense)
  * @param description
  *   Primary description of the transaction
  * @param counterparty
  *   Optional name of the counterparty (payer or payee)
  * @param counterAccount
  *   Optional account number of the counterparty
  * @param reference
  *   Optional reference information like variable symbol
  * @param importBatchId
  *   ID of the batch this transaction was imported with
  * @param status
  *   Current status of the transaction in the processing workflow
  * @param createdAt
  *   When this transaction record was created
  * @param updatedAt
  *   When this transaction record was last updated
  */
case class Transaction(
    id: TransactionId,
    accountId: AccountId,
    date: LocalDate,
    amount: Money,
    description: String,
    counterparty: Option[String] = None,
    counterAccount: Option[String] = None,
    reference: Option[String] = None,
    importBatchId: ImportBatchId,
    status: TransactionStatus,
    createdAt: Instant,
    updatedAt: Instant
):
    /** Updates the transaction's status.
      *
      * @param newStatus
      *   The new status to set
      * @return
      *   A new Transaction instance with updated status and updatedAt timestamp
      */
    def updateStatus(newStatus: TransactionStatus): Transaction =
        this.copy(
            status = newStatus,
            updatedAt = Instant.now
        )

    /** Updates the transaction's description.
      *
      * @param newDescription
      *   The new description
      * @return
      *   A new Transaction instance with updated description and updatedAt timestamp
      */
    def updateDescription(newDescription: String): Either[String, Transaction] =
        if newDescription == null || newDescription.trim.isEmpty then
            Left("Description cannot be empty")
        else
            Right(
                this.copy(
                    description = newDescription.trim,
                    updatedAt = Instant.now
                )
            )

    /** Updates the transaction's counterparty.
      *
      * @param newCounterparty
      *   The new counterparty
      * @return
      *   A new Transaction instance with updated counterparty and updatedAt timestamp
      */
    def updateCounterparty(newCounterparty: Option[String]): Transaction =
        this.copy(
            counterparty = newCounterparty.map(_.trim).filter(_.nonEmpty),
            updatedAt = Instant.now
        )

    /** Determines if this transaction is an expense (negative amount).
      *
      * @return
      *   true if the amount is negative, false otherwise
      */
    def isExpense: Boolean = amount.isNegative

    /** Determines if this transaction is income (positive amount).
      *
      * @return
      *   true if the amount is positive, false otherwise
      */
    def isIncome: Boolean = amount.isPositive
end Transaction

object Transaction:
    /** Creates a new Transaction with validation.
      *
      * @param accountId
      *   ID of the source account
      * @param date
      *   Date when the transaction occurred
      * @param amount
      *   Money amount of the transaction
      * @param description
      *   Primary description of the transaction
      * @param counterparty
      *   Optional name of the counterparty
      * @param counterAccount
      *   Optional account number of the counterparty
      * @param reference
      *   Optional reference information
      * @param importBatchId
      *   ID of the import batch
      * @return
      *   Either a valid Transaction or an error message
      */
    def create(
        accountId: AccountId,
        date: LocalDate,
        amount: Money,
        description: String,
        counterparty: Option[String] = None,
        counterAccount: Option[String] = None,
        reference: Option[String] = None,
        importBatchId: ImportBatchId
    ): Either[String, Transaction] =
        // Validate input
        if accountId == null then
            Left("Account ID must not be null")
        else if date == null then
            Left("Date must not be null")
        else if date.isAfter(LocalDate.now) then
            Left("Transaction date cannot be in the future")
        else if amount == null then
            Left("Amount must not be null")
        else if description == null || description.trim.isEmpty then
            Left("Description must not be empty")
        else if importBatchId == null then
            Left("Import batch ID must not be null")
        else
            val now = Instant.now
            val id = TransactionId.generate()

            // Create new transaction with all data validated
            Right(
                Transaction(
                    id = id,
                    accountId = accountId,
                    date = date,
                    amount = amount,
                    description = description.trim,
                    counterparty = counterparty.map(_.trim).filter(_.nonEmpty),
                    counterAccount = counterAccount.map(_.trim).filter(_.nonEmpty),
                    reference = reference.map(_.trim).filter(_.nonEmpty),
                    importBatchId = importBatchId,
                    status = TransactionStatus.Imported,
                    createdAt = now,
                    updatedAt = now
                )
            )
end Transaction
