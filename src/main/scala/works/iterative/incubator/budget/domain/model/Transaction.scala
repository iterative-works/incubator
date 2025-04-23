package works.iterative.incubator.budget.domain.model

import java.time.LocalDate

/** Represents an immutable financial transaction event from a bank
  *
  * This is a value object representing the raw financial transaction data as received from the
  * bank. It contains only the immutable aspects of a transaction - what actually happened in the
  * financial world.
  *
  * The processing state, categorization, and other mutable aspects are stored separately in
  * TransactionProcessingState.
  *
  * Classification: Domain Entity (Value Object)
  */
case class Transaction(
    // Core identity - includes source account reference
    id: TransactionId, // Composite ID containing sourceAccountId and bank's transaction ID

    // Transaction details
    date: LocalDate, // Transaction date
    amount: BigDecimal, // Transaction amount
    currency: String, // Currency code (e.g., CZK)

    // Counterparty information
    counterAccount: Option[String], // Counter account number
    counterBankCode: Option[String], // Counter bank code
    counterBankName: Option[String], // Name of the counter bank

    // Additional transaction details
    variableSymbol: Option[String], // Variable symbol
    constantSymbol: Option[String], // Constant symbol
    specificSymbol: Option[String], // Specific symbol
    userIdentification: Option[String], // User identification
    message: Option[String], // Message for recipient
    transactionType: String, // Transaction type
    comment: Option[String], // Comment

    // When this transaction was imported (metadata)
    importedAt: java.time.Instant // When this transaction was imported
)