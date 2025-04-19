package works.iterative.incubator.categorization.infrastructure.service

import works.iterative.incubator.categorization.domain.model.*
import works.iterative.incubator.categorization.domain.service.*
import zio.*
import zio.test.*
import zio.test.TestAspect.*

object PayeeCleanupServiceLiveSpec extends ZIOSpecDefault:
    // Test data
    val amazonPayee = "AMAZON MKTPLC AMZN.CO.UK/PMTS 15OCT A1B2CD3E4"
    val uberPayee = "UBER TRIP HELP.UBER.COM 16OCT 12345"
    val unknownPayee = "ACME STORE LONDON 19OCT REF859GBP22.50"

    // Mock OpenAI client that returns predefined responses
    val mockOpenAIClient = new OpenAIClient:
        override def cleanupPayee(
            original: String,
            context: Map[String, String]
        ): IO[OpenAIError, (String, Option[PayeeCleanupRule])] =
            // Return predefined responses based on input
            original match
                case s if s.contains("AMAZON") =>
                    ZIO.succeed(
                        (
                            "Amazon",
                            Some(PayeeCleanupRule.newFromLLM(
                                "AMAZON",
                                PatternType.Contains,
                                "Amazon",
                                0.95
                            ))
                        )
                    )
                case s if s.contains("UBER") =>
                    ZIO.succeed(
                        (
                            "Uber",
                            Some(PayeeCleanupRule.newFromLLM(
                                "UBER",
                                PatternType.Contains,
                                "Uber",
                                0.9
                            ))
                        )
                    )
                case s if s.contains("ACME") =>
                    ZIO.succeed(
                        (
                            "Acme Store",
                            Some(PayeeCleanupRule.newFromLLM(
                                "ACME STORE",
                                PatternType.Contains,
                                "Acme Store",
                                0.8
                            ))
                        )
                    )
                case _ =>
                    ZIO.succeed((original, None))

        override def healthCheck(): IO[OpenAIError, Unit] =
            ZIO.unit
    end mockOpenAIClient

    // Test layer
    val testLayer = ZLayer.succeed(mockOpenAIClient) >>> PayeeCleanupServiceLive.inMemoryLayer

    def spec = suite("PayeeCleanupServiceLive")(
        test("should clean up payee using existing rules") {
            for
                service <- ZIO.service[PayeeCleanupService]
                result <- service.cleanupPayee(amazonPayee, Map.empty)
            yield assertTrue(
                result.original == amazonPayee,
                result.cleaned == "Amazon",
                result.appliedRule.isDefined,
                result.generatedRule.isEmpty
            )
        },
        test("should use LLM for unknown payees") {
            for
                service <- ZIO.service[PayeeCleanupService]
                result <- service.cleanupPayee(unknownPayee, Map.empty)
            yield assertTrue(
                result.original == unknownPayee,
                result.cleaned == "Acme Store",
                result.appliedRule.isEmpty,
                result.generatedRule.isDefined
            )
        },
        test("should correctly manage rule lifecycle") {
            for
                service <- ZIO.service[PayeeCleanupService]
                // Clean up an unknown payee to generate a rule
                result1 <- service.cleanupPayee(unknownPayee, Map.empty)
                // Get the generated rule
                ruleId = result1.generatedRule.get.id
                // Get pending rules
                pendingRules <- service.getPendingRules()
                // Approve the rule
                approvedRule <- service.approveRule(ruleId)
                // Get approved rules
                approvedRules <- service.getApprovedRules()
                // Clean up using the newly approved rule
                result2 <- service.cleanupPayee(unknownPayee, Map.empty)
                // Add feedback
                _ <- service.provideFeedback(ruleId, true)
            yield assertTrue(
                pendingRules.exists(_.id == ruleId),
                approvedRules.exists(_.id == ruleId),
                approvedRule.status == RuleStatus.Approved,
                result2.appliedRule.exists(_.id == ruleId)
            )
        },
        test("should handle rule rejection") {
            for
                service <- ZIO.service[PayeeCleanupService]
                // Clean up an unknown payee to generate a rule
                result <- service.cleanupPayee("SOME NEW PAYEE", Map.empty)
                // Get the generated rule
                ruleId = result.generatedRule.get.id
                // Get pending rules before rejection
                pendingRulesBefore <- service.getPendingRules()
                // Reject the rule
                _ <- service.rejectRule(ruleId, Some("Not useful"))
                // Get pending rules after rejection
                pendingRulesAfter <- service.getPendingRules()
            yield assertTrue(
                pendingRulesBefore.exists(_.id == ruleId),
                !pendingRulesAfter.exists(_.id == ruleId)
            )
        },
        test("should allow manual rule creation") {
            for
                service <- ZIO.service[PayeeCleanupService]
                // Create a manual rule
                rule <- service.createRule(
                    pattern = "STARBUCKS",
                    patternType = PatternType.Contains,
                    replacement = "Starbucks Coffee"
                )
                // Get approved rules
                approvedRules <- service.getApprovedRules()
                // Test the rule
                result <- service.cleanupPayee("STARBUCKS LONDON HIGH ST", Map.empty)
            yield assertTrue(
                rule.status == RuleStatus.Approved,
                rule.generatedBy == GeneratorType.Human,
                approvedRules.exists(_.id == rule.id),
                result.cleaned == "Starbucks Coffee",
                result.appliedRule.exists(_.id == rule.id)
            )
        }
    ).provideLayer(testLayer) @@ timeout(10.seconds)
end PayeeCleanupServiceLiveSpec
