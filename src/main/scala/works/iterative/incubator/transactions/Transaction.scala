package works.iterative.incubator.transactions

case class Transaction(
    // Source data from FIO
    id: TransactionId, // Unique ID from FIO (column_22)
    date: java.time.LocalDate, // Transaction date
    amount: BigDecimal, // Transaction amount
    currency: String, // Currency code (e.g., CZK)
    counterAccount: Option[String], // Counter account number
    counterBankCode: Option[String], // Counter bank code
    counterBankName: Option[String], // Name of the counter bank
    variableSymbol: Option[String], // Variable symbol
    constantSymbol: Option[String], // Constant symbol
    specificSymbol: Option[String], // Specific symbol
    userIdentification: Option[String], // User identification
    message: Option[String], // Message for recipient
    transactionType: String, // Transaction type
    comment: Option[String], // Comment

    // Processing state
    status: TransactionStatus, // Imported, Categorized, Submitted

    // AI computed/processed fields for YNAB
    suggestedPayeeName: Option[String], // AI suggested payee name
    suggestedCategory: Option[String], // AI suggested category
    suggestedMemo: Option[String], // AI cleaned/processed memo

    // User overrides (if user wants to adjust AI suggestions)
    overridePayeeName: Option[String], // User override for payee
    overrideCategory: Option[String], // User override for category
    overrideMemo: Option[String], // User override for memo

    // YNAB integration fields
    ynabTransactionId: Option[String], // ID assigned by YNAB after submission
    ynabAccountId: Option[String], // YNAB account ID where transaction was submitted

    // Metadata
    importedAt: java.time.Instant, // When this transaction was imported
    processedAt: Option[java.time.Instant], // When AI processed this
    submittedAt: Option[java.time.Instant] // When submitted to YNAB
)
