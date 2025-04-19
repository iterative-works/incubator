package works.iterative.incubator.categorization.infrastructure.service

import works.iterative.incubator.categorization.domain.model.*
import works.iterative.incubator.categorization.domain.service.*

import zio.*
import java.time.Instant
import scala.collection.concurrent.TrieMap

/** Live implementation of PayeeCleanupService using OpenAI for LLM capabilities
  *
  * This service orchestrates payee name cleanup by:
  *   1. First checking for matching rules in the repository 2. Falling back to OpenAI for cleaning
  *      when no rules match 3. Tracking rule performance and managing the rule lifecycle
  */
case class PayeeCleanupServiceLive(
    openAIClient: OpenAIClient,
    ruleRepository: Ref[TrieMap[String, PayeeCleanupRule]] // Simplified in-memory repository
) extends PayeeCleanupService:

    /** Clean up a payee name using existing rules or LLM generation */
    override def cleanupPayee(
        original: String,
        context: Map[String, String]
    ): Task[PayeeCleanupResult] =
        for
            // First try to find matching rules
            matchingRules <- findMatchingRules(original)
            result <- matchingRules.headOption match
                case Some(rule) =>
                    // Apply the best matching rule
                    val cleaned = applyRule(original, rule)
                    // Update usage count
                    updateRuleUsage(rule.id).as(
                        PayeeCleanupResult(
                            original = original,
                            cleaned = cleaned,
                            confidence = rule.confidence,
                            appliedRule = Some(rule),
                            generatedRule = None
                        )
                    )

                case None =>
                    // No matching rule, use OpenAI
                    openAIClient.cleanupPayee(original, context)
                        .map { case (cleanedName, suggestedRule) =>
                            PayeeCleanupResult(
                                original = original,
                                cleaned = cleanedName,
                                confidence = suggestedRule.map(_.confidence).getOrElse(0.7),
                                appliedRule = None,
                                generatedRule = suggestedRule
                            )
                        }
                        .catchAll { error =>
                            // Fallback to original name if OpenAI fails
                            ZIO.logError(s"OpenAI error during cleanup: ${error.message}").as(
                                PayeeCleanupResult(
                                    original = original,
                                    cleaned = original, // Fallback to original
                                    confidence = 0.0,
                                    appliedRule = None,
                                    generatedRule = None
                                )
                            )
                        }
        yield result

    /** Get all rules with pending status that need approval */
    override def getPendingRules(): Task[Seq[PayeeCleanupRule]] =
        ruleRepository.get.map { rules =>
            rules.values.filter(_.status == RuleStatus.Pending).toSeq
        }

    /** Get all approved rules */
    override def getApprovedRules(): Task[Seq[PayeeCleanupRule]] =
        ruleRepository.get.map { rules =>
            rules.values.filter(_.status == RuleStatus.Approved).toSeq
        }

    /** Approve a pending rule, optionally with modifications */
    override def approveRule(
        ruleId: String,
        modifications: Option[Map[String, String]]
    ): Task[PayeeCleanupRule] =
        for
            // Get current rules
            rules <- ruleRepository.get

            // Find the rule
            rule <- ZIO.fromOption(rules.get(ruleId))
                .orElseFail(new IllegalArgumentException(s"Rule not found: $ruleId"))

            // Apply modifications if any
            modifiedRule = modifications.map { mods =>
                rule.copy(
                    pattern = mods.getOrElse("pattern", rule.pattern),
                    patternType = mods.get("patternType").flatMap { pt =>
                        pt match
                            case "exact"      => Some(PatternType.Exact)
                            case "contains"   => Some(PatternType.Contains)
                            case "startsWith" => Some(PatternType.StartsWith)
                            case "regex"      => Some(PatternType.Regex)
                            case _            => None
                    }.getOrElse(rule.patternType),
                    replacement = mods.getOrElse("replacement", rule.replacement),
                    updatedAt = Some(Instant.now())
                )
            }.getOrElse(rule)

            // Set status to approved
            approvedRule = modifiedRule.copy(
                status = RuleStatus.Approved,
                updatedAt = Some(Instant.now())
            )

            // Update repository
            _ <- ruleRepository.update { rules =>
                rules.addOne((ruleId, approvedRule))
            }
        yield approvedRule

    /** Reject a pending rule */
    override def rejectRule(
        ruleId: String,
        reason: Option[String]
    ): Task[Unit] =
        for
            // Get current rules
            rules <- ruleRepository.get

            // Find the rule
            rule <- ZIO.fromOption(rules.get(ruleId))
                .orElseFail(new IllegalArgumentException(s"Rule not found: $ruleId"))

            // Set status to rejected
            rejectedRule = rule.copy(
                status = RuleStatus.Rejected,
                updatedAt = Some(Instant.now())
            )

            // Update repository
            _ <- ruleRepository.update { rules =>
                rules.addOne((ruleId, rejectedRule))
            }
        yield ()

    /** Provide feedback on rule application */
    override def provideFeedback(
        ruleId: String,
        wasSuccessful: Boolean
    ): Task[Unit] =
        for
            // Get current rules
            rules <- ruleRepository.get

            // Find the rule
            rule <- ZIO.fromOption(rules.get(ruleId))
                .orElseFail(new IllegalArgumentException(s"Rule not found: $ruleId"))

            // Calculate new success rate
            newSuccessCount = rule.usageCount * rule.successRate + (if wasSuccessful then 1 else 0)
            newUsageCount = rule.usageCount + 1
            newSuccessRate = newSuccessCount / newUsageCount

            // Update rule with new metrics
            updatedRule = rule.copy(
                usageCount = newUsageCount,
                successRate = newSuccessRate,
                updatedAt = Some(Instant.now())
            )

            // Update repository
            _ <- ruleRepository.update { rules =>
                rules.addOne((ruleId, updatedRule))
            }
        yield ()

    /** Find rules that match a given payee name */
    override def findMatchingRules(
        payeeName: String
    ): Task[Seq[PayeeCleanupRule]] =
        ruleRepository.get.map { rules =>
            // Only consider approved rules
            val approvedRules = rules.values.filter(_.status == RuleStatus.Approved)

            // Find matches based on pattern type
            val matches = approvedRules.filter { rule =>
                val matches = rule.patternType match
                    case PatternType.Exact =>
                        payeeName.equalsIgnoreCase(rule.pattern)
                    case PatternType.Contains =>
                        payeeName.toLowerCase.contains(rule.pattern.toLowerCase)
                    case PatternType.StartsWith =>
                        payeeName.toLowerCase.startsWith(rule.pattern.toLowerCase)
                    case PatternType.Regex =>
                        try
                            payeeName.matches(rule.pattern)
                        catch
                            case _: Exception => false

                matches
            }

            // Sort by specificity and success rate
            matches.toSeq.sortBy(r =>
                (-r.patternType.ordinal, -r.successRate, -r.confidence)
            )
        }

    /** Create a new rule manually */
    override def createRule(
        pattern: String,
        patternType: PatternType,
        replacement: String
    ): Task[PayeeCleanupRule] =
        // Create new human rule
        val rule = PayeeCleanupRule.newFromHuman(pattern, patternType, replacement)
        // Add to repository
        ruleRepository.update { rules =>
            rules.addOne((rule.id, rule))
        }.as(rule)
    end createRule

    /** Apply a rule to clean up a payee name */
    private def applyRule(original: String, rule: PayeeCleanupRule): String =
        rule.patternType match
            case PatternType.Exact =>
                rule.replacement
            case PatternType.Contains =>
                rule.replacement
            case PatternType.StartsWith
                if original.toLowerCase.startsWith(rule.pattern.toLowerCase) =>
                rule.replacement
            case PatternType.Regex =>
                try
                    original.replaceAll(rule.pattern, rule.replacement)
                catch
                    case _: Exception => rule.replacement
            case _ =>
                rule.replacement

    /** Update usage count for a rule */
    private def updateRuleUsage(ruleId: String): UIO[Unit] =
        ruleRepository.update { rules =>
            rules.get(ruleId) match
                case Some(rule) =>
                    rules.addOne((ruleId, rule.copy(usageCount = rule.usageCount + 1)))
                case None =>
                    rules
        }
end PayeeCleanupServiceLive

object PayeeCleanupServiceLive:
    /** Create an in-memory test instance with some predefined rules */
    def inMemory: URIO[OpenAIClient, PayeeCleanupService] =
        for
            client <- ZIO.service[OpenAIClient]
            ruleMap <- Ref.make(TrieMap[String, PayeeCleanupRule](
                // Add some predefined rules
                "1" -> PayeeCleanupRule.newFromHuman(
                    pattern = "AMAZON",
                    patternType = PatternType.Contains,
                    replacement = "Amazon"
                ),
                "2" -> PayeeCleanupRule.newFromHuman(
                    pattern = "UBER",
                    patternType = PatternType.Contains,
                    replacement = "Uber"
                )
            ))
        yield PayeeCleanupServiceLive(client, ruleMap)

    /** Layer for dependency injection */
    val inMemoryLayer: ZLayer[OpenAIClient, Nothing, PayeeCleanupService] =
        ZLayer.fromZIO(inMemory)
end PayeeCleanupServiceLive
