package works.iterative.incubator.budget.domain.mock

import zio.*

import works.iterative.incubator.budget.domain.model.*
import works.iterative.incubator.budget.domain.port.*
import works.iterative.incubator.budget.domain.port.{
    CategorizationProvider,
    TransactionCategorization,
    CategorizationError
}

/** Mock implementation of the CategorizationProvider port for testing purposes.
  *
  * This mock provides configurable behavior for testing categorization scenarios including:
  *   - Rule-based categorization logic
  *   - Configurable confidence scores
  *   - Error simulation
  *   - Learning behavior tracking
  */
case class MockCategorizationProvider(
    config: Ref[CategorizationConfig],
    invocations: Ref[List[CategorizationInvocation]]
) extends CategorizationProvider:
    /** Categorizes a single transaction based on configured rules.
      *
      * @param transaction
      *   The transaction to categorize
      * @return
      *   ZIO effect with categorization result or configured error
      */
    def categorizeTransaction(
        transaction: Transaction
    ): ZIO[Any, CategorizationError, TransactionCategorization] =
        for
            cfg <- config.get
            _ <-
                invocations.update(_ :+ CategorizationInvocation.CategorizeTransaction(transaction))
            result <- cfg.categorizeError match
                case Some(error) => ZIO.fail(error)
                case None =>
                    getCategoryForTransaction(transaction, cfg)
        yield result

    /** Categorizes multiple transactions in bulk.
      *
      * @param transactions
      *   The list of transactions to categorize
      * @return
      *   ZIO effect with categorization results or configured error
      */
    def categorizeTransactions(
        transactions: List[Transaction]
    ): ZIO[Any, CategorizationError, List[TransactionCategorization]] =
        for
            cfg <- config.get
            _ <- invocations.update(
                _ :+ CategorizationInvocation.CategorizeTransactions(transactions)
            )
            result <- cfg.bulkCategorizeError match
                case Some(error) => ZIO.fail(error)
                case None =>
                    ZIO.foreach(transactions)(tx => getCategoryForTransaction(tx, cfg))
        yield result

    /** Updates the categorization model with feedback about a categorization.
      *
      * @param transaction
      *   The transaction that was categorized
      * @param assignedCategory
      *   The category that was assigned
      * @param correctCategory
      *   The correct category provided by user feedback
      * @return
      *   ZIO effect indicating success or a provider error
      */
    def learnFromFeedback(
        transaction: Transaction,
        assignedCategory: Category,
        correctCategory: Category
    ): ZIO[Any, CategorizationError, Unit] =
        for
            cfg <- config.get
            _ <- invocations.update(
                _ :+ CategorizationInvocation.LearnFromFeedback(
                    transaction,
                    assignedCategory,
                    correctCategory
                )
            )
            result <- cfg.learnError match
                case Some(error) => ZIO.fail(error)
                case None        =>
                    // In a real implementation, this would update the model
                    // For the mock, we just record the invocation
                    ZIO.unit
        yield result

    // Internal helper methods

    private def getCategoryForTransaction(
        transaction: Transaction,
        cfg: CategorizationConfig
    ): UIO[TransactionCategorization] =
        for
            // Find the first matching rule based on transaction message
            matchingRule <- ZIO.succeed(
                cfg.rules.find(rule =>
                    transaction.message.exists(_.toLowerCase.contains(rule.keyword.toLowerCase)) ||
                        transaction.comment.exists(
                            _.toLowerCase.contains(rule.keyword.toLowerCase)
                        ) ||
                        transaction.transactionType.toLowerCase.contains(rule.keyword.toLowerCase)
                )
            )

            // Determine category and confidence
            (category, confidence) <- matchingRule match
                case Some(rule) => ZIO.succeed((rule.category, rule.confidence))
                case None       => ZIO.succeed((cfg.defaultCategory, cfg.defaultConfidence))

            // Generate alternatives if configured
            alternatives <- ZIO.succeed(
                if cfg.generateAlternatives then
                    cfg.alternativeCategories
                        .filterNot(_ == category)
                        .take(cfg.numAlternatives)
                        .map(c => (c, ConfidenceScore(scala.util.Random.between(0.1, 0.6))))
                else
                    List.empty
            )
        yield TransactionCategorization(
            transactionId = transaction.id,
            category = category,
            confidenceScore = confidence,
            alternativeCategories = alternatives
        )

    // Configuration methods for testing

    /** Adds a categorization rule to the mock.
      *
      * @param keyword
      *   Keyword to match in transaction description
      * @param category
      *   Category to assign on match
      * @param confidence
      *   Confidence score for the categorization
      * @return
      *   ZIO effect updating the mock configuration
      */
    def addRule(
        keyword: String,
        category: Category,
        confidence: ConfidenceScore = ConfidenceScore(0.9)
    ): UIO[Unit] =
        config.update { cfg =>
            val newRule = CategorizationRule(keyword, category, confidence)
            cfg.copy(rules = cfg.rules :+ newRule)
        }

    /** Sets the default category for transactions that match no rules.
      *
      * @param category
      *   The default category to assign
      * @param confidence
      *   The confidence score for the default category
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setDefaultCategory(
        category: Category,
        confidence: ConfidenceScore = ConfidenceScore(0.5)
    ): UIO[Unit] =
        config.update(cfg =>
            cfg.copy(defaultCategory = category, defaultConfidence = confidence)
        )

    /** Configures alternative categories to be returned with categorization results.
      *
      * @param categories
      *   List of alternative categories to include
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setAlternativeCategories(categories: List[Category]): UIO[Unit] =
        config.update(cfg =>
            cfg.copy(
                alternativeCategories = categories,
                generateAlternatives = true
            )
        )

    /** Configures an error for single transaction categorization.
      *
      * @param error
      *   The error to throw on categorizeTransaction calls
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setCategorizeError(error: CategorizationError): UIO[Unit] =
        config.update(cfg => cfg.copy(categorizeError = Some(error)))

    /** Configures an error for bulk transaction categorization.
      *
      * @param error
      *   The error to throw on categorizeTransactions calls
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setBulkCategorizeError(error: CategorizationError): UIO[Unit] =
        config.update(cfg => cfg.copy(bulkCategorizeError = Some(error)))

    /** Configures an error for learning from feedback.
      *
      * @param error
      *   The error to throw on learnFromFeedback calls
      * @return
      *   ZIO effect updating the mock configuration
      */
    def setLearnError(error: CategorizationError): UIO[Unit] =
        config.update(cfg => cfg.copy(learnError = Some(error)))

    /** Clears all configured errors.
      *
      * @return
      *   ZIO effect clearing error configurations
      */
    def clearErrors: UIO[Unit] =
        config.update(cfg =>
            cfg.copy(
                categorizeError = None,
                bulkCategorizeError = None,
                learnError = None
            )
        )

    /** Gets all recorded invocations for verification.
      *
      * @return
      *   ZIO effect with list of recorded invocations
      */
    def getInvocations: UIO[List[CategorizationInvocation]] =
        invocations.get

    /** Resets the mock to its initial state.
      *
      * @return
      *   ZIO effect resetting the mock
      */
    def reset: UIO[Unit] =
        config.set(CategorizationConfig.default) *>
            invocations.set(List.empty)
end MockCategorizationProvider

object MockCategorizationProvider:
    /** Creates a new MockCategorizationProvider with default configuration.
      *
      * @return
      *   ZIO effect creating the mock
      */
    def make: UIO[MockCategorizationProvider] =
        for
            configRef <- Ref.make(CategorizationConfig.default)
            invocationsRef <- Ref.make(List.empty[CategorizationInvocation])
        yield MockCategorizationProvider(configRef, invocationsRef)

    /** Creates a preconfigured mock for specific scenarios.
      *
      * @param scenarioName
      *   Name of the predefined scenario to use
      * @return
      *   ZIO effect creating a configured mock
      */
    def forScenario(scenarioName: String): UIO[MockCategorizationProvider] =
        for
            mock <- make
            _ <- scenarioName match
                case "successful-categorization" =>
                    mock.addRule("GROCERY", CategoryHelper.create("Groceries")) *>
                        mock.addRule("RESTAURANT", CategoryHelper.create("Dining Out")) *>
                        mock.addRule("UBER", CategoryHelper.create("Transportation"))
                case "ambiguous-transaction" =>
                    mock.addRule(
                        "AMAZON",
                        CategoryHelper.create("Shopping"),
                        ConfidenceScore(0.6)
                    ) *>
                        mock.setAlternativeCategories(
                            List(
                                CategoryHelper.create("Entertainment"),
                                CategoryHelper.create("Home"),
                                CategoryHelper.create("Gifts")
                            )
                        )
                case "categorization-fails" =>
                    mock.setCategorizeError(
                        CategorizationError.ServiceUnavailable("Service is down for maintenance")
                    )
                case _ => ZIO.unit
        yield mock

    /** Creates a ZIO layer for the MockCategorizationProvider.
      *
      * @return
      *   ZLayer containing the MockCategorizationProvider as a CategorizationProvider
      */
    val layer: ULayer[CategorizationProvider] =
        ZLayer.scoped {
            ZIO.acquireRelease(
                make.map(provider => provider.asInstanceOf[CategorizationProvider])
            )(_ => ZIO.unit)
        }

    /** Creates a ZIO layer for the MockCategorizationProvider, exposing the mock interfaces.
      *
      * @return
      *   ZLayer containing the MockCategorizationProvider
      */
    val mockLayer: ULayer[MockCategorizationProvider] =
        ZLayer.scoped {
            ZIO.acquireRelease(
                make
            )(_.reset)
        }
end MockCategorizationProvider

/** Configuration for the MockCategorizationProvider. */
case class CategorizationConfig(
    rules: List[CategorizationRule] = List.empty,
    defaultCategory: Category =
        works.iterative.incubator.budget.domain.model.Category.Uncategorized,
    defaultConfidence: ConfidenceScore = ConfidenceScore(0.5),
    alternativeCategories: List[Category] = List.empty,
    generateAlternatives: Boolean = false,
    numAlternatives: Int = 3,
    categorizeError: Option[CategorizationError] = None,
    bulkCategorizeError: Option[CategorizationError] = None,
    learnError: Option[CategorizationError] = None
)

object CategorizationConfig:
    /** Default configuration with no rules and an Uncategorized default category. */
    val default: CategorizationConfig = CategorizationConfig()
end CategorizationConfig

/** Rule for matching transactions to categories. */
case class CategorizationRule(
    keyword: String,
    category: Category,
    confidence: ConfidenceScore
)

/** Helper object to create a category */
object CategoryHelper:
    def create(name: String): Category = Category(
        id = s"mock-${name.toLowerCase.replace(' ', '-')}",
        name = name,
        parentId = None
    )
end CategoryHelper

/** Records method invocations for verification in tests. */
sealed trait CategorizationInvocation

object CategorizationInvocation:
    /** Records a categorizeTransaction call */
    case class CategorizeTransaction(
        transaction: Transaction
    ) extends CategorizationInvocation

    /** Records a categorizeTransactions call */
    case class CategorizeTransactions(
        transactions: List[Transaction]
    ) extends CategorizationInvocation

    /** Records a learnFromFeedback call */
    case class LearnFromFeedback(
        transaction: Transaction,
        assignedCategory: Category,
        correctCategory: Category
    ) extends CategorizationInvocation
end CategorizationInvocation
