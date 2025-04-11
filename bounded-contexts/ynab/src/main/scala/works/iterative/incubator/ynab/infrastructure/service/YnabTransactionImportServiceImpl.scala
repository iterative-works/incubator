package works.iterative.incubator.ynab.infrastructure.service

import zio.*
import works.iterative.incubator.ynab.application.service.{YnabService, YnabTransactionImportService}
import works.iterative.incubator.ynab.domain.model.*
import works.iterative.incubator.ynab.domain.repository.YnabAccountMappingRepository
import java.util.UUID

/**
 * Implementation of YnabTransactionImportService
 *
 * @param ynabService Service for interacting with YNAB API
 * @param accountMappingRepository Repository for account mappings
 */
class YnabTransactionImportServiceImpl(
    ynabService: YnabService,
    accountMappingRepository: YnabAccountMappingRepository
) extends YnabTransactionImportService:
    /**
     * Import transactions to YNAB
     *
     * @param transactions The transactions to import
     * @param sourceAccountId The source account ID
     * @return A map of transaction IDs to their import status
     */
    override def importTransactions(
        transactions: Seq[YnabTransaction],
        sourceAccountId: String
    ): Task[Map[String, YnabTransactionImportResult]] = {
        for {
            // First get the mapping for this account
            mappingOpt <- accountMappingRepository.findBySourceAccountId(sourceAccountId.toLong)
            
            // Ensure the mapping exists before proceeding
            mapping <- ZIO.fromOption(mappingOpt)
              .orElseFail(new RuntimeException(s"No YNAB account mapping found for source account $sourceAccountId"))
            
            // Only proceed if the mapping is active
            _ <- ZIO.when(!mapping.active) {
              ZIO.fail(new RuntimeException(s"YNAB account mapping for source account $sourceAccountId is not active"))
            }
            
            // Get all available budgets to find the correct one
            budgets <- ynabService.getBudgets()
            budgetId = budgets.headOption.map(_.id).getOrElse("default-budget")
            
            // Get the budget service for the selected budget
            budgetService = ynabService.getBudgetService(budgetId)
            
            // Import the transactions using the budget service
            results <- budgetService.createTransactions(transactions.map(tx => 
                // Make sure the transaction uses the correct YNAB account ID from the mapping
                tx.copy(accountId = mapping.ynabAccountId)
            ))
            
            // Convert the results to the expected format
            importResults = results.map { case (transaction, id) => 
                val transactionId = transaction.id.getOrElse(transaction.importId.getOrElse(UUID.randomUUID().toString))
                transactionId -> YnabTransactionImportSuccess(ynabTransactionId = id)
            }
        } yield importResults
    }
end YnabTransactionImportServiceImpl

object YnabTransactionImportServiceImpl:
    val layer: URLayer[YnabService & YnabAccountMappingRepository, YnabTransactionImportService] =
        ZLayer.fromFunction(YnabTransactionImportServiceImpl(_, _))
end YnabTransactionImportServiceImpl