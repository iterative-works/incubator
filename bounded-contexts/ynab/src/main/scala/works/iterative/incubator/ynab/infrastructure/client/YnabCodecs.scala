package works.iterative.incubator.ynab.infrastructure.client

import zio.json.*
import java.time.LocalDate
import works.iterative.incubator.ynab.domain.model.*

/** DTOs and JSON codecs for YNAB API
 *
 * This file contains the data transfer objects and JSON codecs for interacting with the YNAB API.
 * DTOs are used to map between our domain model and the API's JSON structure.
 *
 * Classification: Infrastructure DTO
 */

// Generic API response wrapper
case class ApiResponse[T](data: T)

// Specific response types
case class BudgetsResponse(budgets: Seq[YnabBudget])
case class AccountsResponse(accounts: Seq[YnabAccount])
case class CategoriesResponse(categoryGroups: Seq[CategoryGroupDTO])
case class TransactionResponse(transaction: TransactionDTO)
case class TransactionsResponse(transactions: Seq[TransactionDTO])

// Request types
case class TransactionCreateRequest(transaction: TransactionDTO)
case class TransactionsCreateRequest(transactions: Seq[TransactionDTO])

// DTOs for mapping between API and domain models
case class CategoryGroupDTO(
    id: String,
    name: String,
    hidden: Boolean,
    deleted: Boolean,
    categories: Seq[CategoryDTO]
)

case class CategoryDTO(
    id: String,
    name: String,
    hidden: Boolean,
    deleted: Boolean,
    budgeted: Option[BigDecimal],
    activity: Option[BigDecimal],
    balance: Option[BigDecimal]
)

case class TransactionDTO(
    id: String,
    date: String, // ISO format: "YYYY-MM-DD"
    amount: Long, // In milliunits (1/1000)
    memo: Option[String],
    cleared: String,
    approved: Boolean,
    account_id: String,
    payee_name: Option[String],
    payee_id: Option[String],
    category_id: Option[String],
    flag_color: Option[String],
    import_id: Option[String]
):
    // Convert DTO to domain model
    def toDomain: YnabTransaction =
        YnabTransaction(
            id = Some(id),
            date = LocalDate.parse(date),
            amount = BigDecimal(amount) / 1000, // Convert from milliunits
            memo = memo,
            cleared = cleared,
            approved = approved,
            accountId = account_id,
            payeeName = payee_name,
            payeeId = payee_id,
            categoryId = category_id,
            flagColor = flag_color,
            importId = import_id
        )

object TransactionDTO:
    // Convert domain model to DTO
    def fromDomain(transaction: YnabTransaction): TransactionDTO =
        TransactionDTO(
            id = transaction.id.getOrElse(""),
            date = transaction.date.toString,
            amount = (transaction.amount * 1000).toLongExact, // Convert to milliunits
            memo = transaction.memo,
            cleared = transaction.cleared,
            approved = transaction.approved,
            account_id = transaction.accountId,
            payee_name = transaction.payeeName,
            payee_id = transaction.payeeId,
            category_id = transaction.categoryId,
            flag_color = transaction.flagColor,
            import_id = transaction.importId.orElse(Some(transaction.generateImportId))
        )

// JSON codecs
object YnabCodecs:
    // Date format conversion helpers
    private val dateFormat = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
    
    given JsonEncoder[LocalDate] = JsonEncoder.string.contramap[LocalDate](_.format(dateFormat))
    given JsonDecoder[LocalDate] = JsonDecoder.string.map(LocalDate.parse(_, dateFormat))
    
    // Domain model codecs
    given JsonEncoder[YnabBudget] = DeriveJsonEncoder.gen[YnabBudget]
    given JsonDecoder[YnabBudget] = DeriveJsonDecoder.gen[YnabBudget]
    
    given JsonEncoder[YnabAccount] = DeriveJsonEncoder.gen[YnabAccount]
    given JsonDecoder[YnabAccount] = DeriveJsonDecoder.gen[YnabAccount]
    
    given JsonEncoder[YnabCategoryGroup] = DeriveJsonEncoder.gen[YnabCategoryGroup]
    given JsonDecoder[YnabCategoryGroup] = DeriveJsonDecoder.gen[YnabCategoryGroup]
    
    given JsonEncoder[YnabCategory] = DeriveJsonEncoder.gen[YnabCategory]
    given JsonDecoder[YnabCategory] = DeriveJsonDecoder.gen[YnabCategory]
    
    // DTO codecs
    given JsonEncoder[TransactionDTO] = DeriveJsonEncoder.gen[TransactionDTO]
    given JsonDecoder[TransactionDTO] = DeriveJsonDecoder.gen[TransactionDTO]
    
    given JsonEncoder[CategoryDTO] = DeriveJsonEncoder.gen[CategoryDTO]
    given JsonDecoder[CategoryDTO] = DeriveJsonDecoder.gen[CategoryDTO]
    
    given JsonEncoder[CategoryGroupDTO] = DeriveJsonEncoder.gen[CategoryGroupDTO]
    given JsonDecoder[CategoryGroupDTO] = DeriveJsonDecoder.gen[CategoryGroupDTO]
    
    // Request/Response codecs
    given JsonEncoder[TransactionCreateRequest] = DeriveJsonEncoder.gen[TransactionCreateRequest]
    given JsonDecoder[TransactionCreateRequest] = DeriveJsonDecoder.gen[TransactionCreateRequest]
    
    given JsonEncoder[TransactionsCreateRequest] = DeriveJsonEncoder.gen[TransactionsCreateRequest]
    given JsonDecoder[TransactionsCreateRequest] = DeriveJsonDecoder.gen[TransactionsCreateRequest]
    
    given [T: JsonDecoder]: JsonDecoder[ApiResponse[T]] = DeriveJsonDecoder.gen[ApiResponse[T]]
    given [T: JsonEncoder]: JsonEncoder[ApiResponse[T]] = DeriveJsonEncoder.gen[ApiResponse[T]]
    
    given JsonDecoder[BudgetsResponse] = DeriveJsonDecoder.gen[BudgetsResponse]
    given JsonDecoder[AccountsResponse] = DeriveJsonDecoder.gen[AccountsResponse]
    given JsonDecoder[CategoriesResponse] = DeriveJsonDecoder.gen[CategoriesResponse]
    given JsonDecoder[TransactionResponse] = DeriveJsonDecoder.gen[TransactionResponse]
    given JsonDecoder[TransactionsResponse] = DeriveJsonDecoder.gen[TransactionsResponse]