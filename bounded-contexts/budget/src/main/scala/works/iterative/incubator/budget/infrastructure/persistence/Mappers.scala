package works.iterative.incubator.budget.infrastructure.persistence

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.FioAccount
import java.util.Currency

/** Mappers for converting between domain entities and database DTOs.
  */
object TransactionMapper:
    /** Converts a domain Transaction entity to a TransactionDTO for database storage.
      */
    def toDTO(entity: Transaction): TransactionDTO =
        TransactionDTO(
            id = entity.id.toString,
            sourceAccountId = entity.accountId.toString,
            transactionDate = entity.date,
            amountValue = entity.amount.amount,
            amountCurrency = entity.amount.currency.getCurrencyCode,
            description = entity.description,
            counterparty = entity.counterparty,
            counterAccount = entity.counterAccount,
            reference = entity.reference,
            importBatchId = entity.importBatchId.toString,
            status = entity.status.toString,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    /** Converts a TransactionDTO from the database to a domain Transaction entity.
      */
    def toDomain(dto: TransactionDTO): Either[String, Transaction] =
        for
            transactionId <- TransactionId.fromString(dto.id)
            importBatchId <- ImportBatchId.fromString(dto.importBatchId)
            currency <-
                try Right(Currency.getInstance(dto.amountCurrency))
                catch
                    case _: Exception => Left(s"Invalid currency code: ${dto.amountCurrency}")
            money = Money(dto.amountValue, currency)
            status <-
                try Right(TransactionStatus.valueOf(dto.status))
                catch
                    case _: Exception => Left(s"Invalid transaction status: ${dto.status}")
        yield Transaction(
            id = transactionId,
            date = dto.transactionDate,
            amount = money,
            description = dto.description,
            counterparty = dto.counterparty,
            counterAccount = dto.counterAccount,
            reference = dto.reference,
            importBatchId = importBatchId,
            status = status,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
end TransactionMapper

object ImportBatchMapper:
    /** Converts a domain ImportBatch entity to an ImportBatchDTO for database storage.
      */
    def toDTO(entity: ImportBatch): ImportBatchDTO =
        ImportBatchDTO(
            id = entity.id.toString,
            accountId = entity.accountId.toString,
            startDate = entity.startDate,
            endDate = entity.endDate,
            status = entity.status.toString,
            transactionCount = entity.transactionCount,
            errorMessage = entity.errorMessage,
            startTime = entity.startTime,
            endTime = entity.endTime,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    /** Converts an ImportBatchDTO from the database to a domain ImportBatch entity.
      */
    def toDomain(dto: ImportBatchDTO): Either[String, ImportBatch] =
        for
            importBatchId <- ImportBatchId.fromString(dto.id)
            accountId <- AccountId.fromString(dto.accountId)
            status <-
                try Right(ImportStatus.valueOf(dto.status))
                catch
                    case _: Exception => Left(s"Invalid import status: ${dto.status}")
        yield ImportBatch(
            id = importBatchId,
            accountId = accountId,
            startDate = dto.startDate,
            endDate = dto.endDate,
            status = status,
            transactionCount = dto.transactionCount,
            errorMessage = dto.errorMessage,
            startTime = dto.startTime,
            endTime = dto.endTime,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
end ImportBatchMapper

object FioAccountMapper:
    /** Converts a domain FioAccount entity to a FioAccountDTO for database storage.
      */
    def toDTO(entity: FioAccount): FioAccountDTO =
        FioAccountDTO(
            id = entity.id,
            sourceAccountId = entity.sourceAccountId.toString,
            encryptedToken = entity.encryptedToken,
            lastSyncTime = entity.lastSyncTime,
            lastFetchedId = entity.lastFetchedId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    /** Converts a FioAccountDTO from the database to a domain FioAccount entity.
      */
    def toDomain(dto: FioAccountDTO): Either[String, FioAccount] =
        for
            accountId <- AccountId.fromString(dto.sourceAccountId)
        yield FioAccount(
            id = dto.id,
            sourceAccountId = accountId,
            encryptedToken = dto.encryptedToken,
            lastSyncTime = dto.lastSyncTime,
            lastFetchedId = dto.lastFetchedId,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
end FioAccountMapper
