package works.iterative.incubator.budget.domain.service.impl

import zio.*
import java.time.Instant
import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.domain.event.*
import works.iterative.incubator.budget.domain.service.*
import works.iterative.incubator.budget.domain.query.TransactionProcessingStateQuery

/** Implementation of the CategorizationService that contains all business logic for transaction categorization.
  *
  * This implementation follows the functional core pattern, containing all business logic for
  * transaction categorization while delegating infrastructure concerns to repository interfaces.
  *
  * @param transactionRepository Repository for retrieving Transaction entities
  * @param processingStateRepository Repository for storing and retrieving transaction processing states
  * @param categoryRepository Repository for retrieving categories
  * @param categorizationStrategy Strategy for category assignment (can be replaced for testing)
  * @param eventPublisher Function to publish domain events
  */
case class CategorizationServiceImpl(
    transactionRepository: TransactionRepository,
    processingStateRepository: TransactionProcessingStateRepository,
    categoryRepository: CategoryRepository,
    categorizationStrategy: CategorizationStrategy,
    eventPublisher: DomainEvent => UIO[Unit]
) extends CategorizationService:
    
    /** Apply automated categorization to a batch of transactions
      *
      * This workflow:
      * 1. Processes uncategorized transactions using categorization logic
      * 2. Updates transaction processing states with suggested categories and confidence scores
      * 3. Emits events for categorized transactions
      *
      * @param transactionIds The IDs of transactions to categorize
      * @return A CategorizedResult with the number of categorized transactions
      */
    override def categorizeTransactions(
        transactionIds: Seq[TransactionId]
    ): UIO[CategorizationResult] =
        for
            // Process each transaction and collect results
            categorizations <- ZIO.foreach(transactionIds) { id => 
                categorizeTransaction(id)
            }
            validCats = categorizations.flatten
            
            // Calculate average confidence
            avgConfidence <- calculateAverageConfidence(validCats)
            
            // Publish batch event if we have any successful categorizations
            _ <- ZIO.when(validCats.nonEmpty) {
                val sourceAccountId = validCats.head.transactionId.sourceAccountId
                val event = TransactionsCategorized(
                    transactionCount = validCats.size,
                    sourceAccountId = sourceAccountId,
                    averageConfidence = avgConfidence.getOrElse(ConfidenceScore(0.0)),
                    occurredAt = Instant.now()
                )
                eventPublisher(event)
            }
        yield CategorizationResult(
            categorizedCount = validCats.size,
            failedCount = transactionIds.size - validCats.size,
            averageConfidence = avgConfidence
        )

    /** Apply categorization to a single transaction
      *
      * @param transactionId The ID of the transaction to categorize
      * @return The categorization results for the transaction
      */
    override def categorizeTransaction(
        transactionId: TransactionId
    ): UIO[Option[TransactionCategorization]] =
        for
            // Get the transaction and its processing state
            txOpt <- transactionRepository.findById(transactionId)
            stateOpt <- processingStateRepository.load(transactionId)
            
            // If both exist, proceed with categorization
            result <- (txOpt, stateOpt) match
                case (Some(tx), Some(state)) if !state.isDuplicate && state.status == TransactionStatus.Imported =>
                    for
                        // Apply categorization strategy to get suggested category and confidence
                        categorization <- categorizationStrategy.categorize(tx)
                        
                        // If we have a category suggestion, update the processing state
                        _ <- ZIO.when(categorization.categoryId.isDefined) {
                            for
                                // Update processing state with AI suggestions
                                updatedState <- ZIO.succeed(state.withAICategorization(
                                    payeeName = categorization.payeeName,
                                    category = categorization.categoryId,
                                    memo = categorization.memo,
                                    categoryConfidence = categorization.confidence,
                                    payeeConfidence = Some(ConfidenceScore(0.8))
                                ))
                                _ <- processingStateRepository.save(transactionId, updatedState)
                                
                                // Publish event for the categorized transaction
                                _ <- ZIO.when(categorization.categoryId.isDefined) {
                                    val event = TransactionCategorized(
                                        transactionId = transactionId,
                                        category = categorization.categoryId.get,
                                        payeeName = categorization.payeeName.getOrElse("Unknown"),
                                        byAI = true,
                                        occurredAt = Instant.now()
                                    )
                                    eventPublisher(event)
                                }
                            yield ()
                        }
                    yield categorization
                    
                case _ => ZIO.succeed(None)
        yield result match
            case value: TransactionCategorization => Some(value)
            case _ => None

    /** Manually update category for a single transaction
      *
      * @param transactionId The ID of the transaction to update
      * @param categoryId The new category ID
      * @param memo Optional memo to update
      * @param payeeName Optional payee name to update
      * @return An updated TransactionProcessingState
      */
    override def updateCategory(
        transactionId: TransactionId,
        categoryId: String,
        memo: Option[String] = None,
        payeeName: Option[String] = None
    ): UIO[Option[TransactionProcessingState]] =
        for
            // Get the current processing state
            stateOpt <- processingStateRepository.load(transactionId)
            
            // If exists, update with user overrides
            resultOpt <- stateOpt match
                case Some(state) =>
                    for
                        // Verify the category exists
                        categoryOpt <- categoryRepository.findById(categoryId)
                        _ <- ZIO.when(categoryOpt.isEmpty) {
                            ZIO.logWarning(s"Attempting to set non-existent category: $categoryId")
                        }
                        
                        // Apply user overrides
                        updatedState = state.withUserOverrides(
                            payeeName = payeeName,
                            category = Some(categoryId),
                            memo = memo
                        )
                        _ <- processingStateRepository.save(transactionId, updatedState)
                        
                        // Publish event for manual update
                        event = CategoryUpdated(
                            transactionId = transactionId,
                            oldCategory = state.effectiveCategory,
                            newCategory = categoryId,
                            occurredAt = Instant.now()
                        )
                        _ <- eventPublisher(event)
                    yield Some(updatedState)
                    
                case None => ZIO.succeed(None)
        yield resultOpt

    /** Bulk update categories for transactions matching criteria
      *
      * @param filter Criteria for matching transactions to update
      * @param categoryId The new category ID
      * @param memo Optional memo to update
      * @param payeeName Optional payee name to update
      * @return The number of transactions updated
      */
    override def bulkUpdateCategory(
        filter: TransactionFilter,
        categoryId: String,
        memo: Option[String] = None,
        payeeName: Option[String] = None
    ): UIO[Int] =
        for
            // Verify category exists
            categoryOpt <- categoryRepository.findById(categoryId)
            _ <- ZIO.when(categoryOpt.isEmpty) {
                ZIO.logWarning(s"Attempting to bulk update with non-existent category: $categoryId")
            }
            
            // Construct query to find matching transactions - just filter by source account and we'll do further filtering below
            query <- ZIO.succeed(TransactionProcessingStateQuery(
                sourceAccountId = filter.sourceAccountId
            ))
            
            // Find matching processing states
            states <- processingStateRepository.find(query)
            
            // Further filter based on transaction content (needs to load actual transactions)
            filteredStates <- if filter.descriptionContains.isDefined || filter.counterPartyContains.isDefined || 
                              filter.transactionType.isDefined then
                ZIO.foreach(states) { state =>
                    for
                        txOpt <- transactionRepository.findById(state.transactionId)
                    yield txOpt.map(tx => (tx, state))
                }.map(_.flatten).map { txAndStates =>
                    // Apply additional filters
                    txAndStates.filter { case (tx, _) =>
                        val descMatches = filter.descriptionContains.forall(desc => 
                            tx.message.exists(_.contains(desc)) || tx.userIdentification.exists(_.contains(desc))
                        )
                        val counterPartyMatches = filter.counterPartyContains.forall(cp => 
                            tx.counterAccount.exists(_.contains(cp)) || tx.counterBankName.exists(_.contains(cp))
                        )
                        val typeMatches = filter.transactionType.forall(_ == tx.transactionType)
                        
                        descMatches && counterPartyMatches && typeMatches
                    }.map(_._2)
                }
            else 
                ZIO.succeed(states)
                
            // Apply updates to each filtered state
            updates <- ZIO.foreach(filteredStates) { state =>
                updateCategory(state.transactionId, categoryId, memo, payeeName)
            }
            updatedCount = updates.flatten.size
            
            // Publish bulk update event
            _ <- ZIO.when(updatedCount > 0) {
                val event = BulkCategoryUpdated(
                    count = updatedCount,
                    category = categoryId,
                    filterCriteria = s"Filter: ${filter.toString}",
                    occurredAt = Instant.now()
                )
                eventPublisher(event)
            }
        yield updatedCount

    /** Calculate average confidence score for a batch of categorizations
      *
      * @param categorizations List of transaction categorizations 
      * @return An average confidence score
      */
    override def calculateAverageConfidence(
        categorizations: Seq[TransactionCategorization]
    ): UIO[Option[ConfidenceScore]] =
        ZIO.succeed {
            val confidences = categorizations.flatMap(_.confidence.map(_.value))
            if confidences.isEmpty then None
            else Some(ConfidenceScore(confidences.sum / confidences.size))
        }
end CategorizationServiceImpl

/** Companion object for CategorizationServiceImpl */
object CategorizationServiceImpl:
    /** Create a ZLayer for the CategorizationServiceImpl */
    def layer(
        categorizationStrategy: CategorizationStrategy,
        eventPublisher: DomainEvent => UIO[Unit]
    ): URLayer[TransactionRepository & TransactionProcessingStateRepository & CategoryRepository, CategorizationService] =
        ZLayer {
            for
                transactionRepository <- ZIO.service[TransactionRepository]
                processingStateRepository <- ZIO.service[TransactionProcessingStateRepository]
                categoryRepository <- ZIO.service[CategoryRepository]
            yield CategorizationServiceImpl(
                transactionRepository,
                processingStateRepository,
                categoryRepository,
                categorizationStrategy,
                eventPublisher
            )
        }
end CategorizationServiceImpl

/** Strategy interface for transaction categorization
  * 
  * This allows different categorization algorithms to be plugged in,
  * such as rule-based, machine learning, etc.
  */
trait CategorizationStrategy:
    def categorize(transaction: Transaction): UIO[TransactionCategorization]
end CategorizationStrategy

/** Simple rule-based categorization strategy implementation */
case class SimpleCategorizationStrategy(
    categories: Map[String, String],
    defaultConfidence: Double = 0.7
) extends CategorizationStrategy:
    def categorize(transaction: Transaction): UIO[TransactionCategorization] =
        ZIO.succeed {
            // Very simple matching based on transaction description or user identification
            val searchText = (transaction.message ++ transaction.userIdentification).mkString(" ").toLowerCase
            
            // Find the first matching category pattern
            val categoryMatch = categories.find { case (pattern, _) => 
                searchText.contains(pattern.toLowerCase)
            }
            
            // Create result
            TransactionCategorization(
                transactionId = transaction.id,
                categoryId = categoryMatch.map(_._2),
                payeeName = transaction.userIdentification,
                memo = transaction.message,
                confidence = categoryMatch.map(_ => ConfidenceScore(defaultConfidence))
            )
        }
end SimpleCategorizationStrategy