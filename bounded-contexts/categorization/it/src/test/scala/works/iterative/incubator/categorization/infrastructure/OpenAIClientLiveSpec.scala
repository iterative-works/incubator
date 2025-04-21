package works.iterative.incubator.categorization.infrastructure.service

import works.iterative.incubator.categorization.domain.model.*
import works.iterative.incubator.categorization.domain.service.OpenAIClient

import zio.*
import zio.test.*

object OpenAIClientLiveSpec extends ZIOSpecDefault:
    // Tests
    def spec = suite("OpenAIClientLive")(
        // Integration tests - only run when enabled
        suite("Integration tests with real OpenAI API")(
            test("cleanupPayee should successfully clean a payee name") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.cleanupPayee(
                        "AMAZON UK MARKETPLACE 123-4567890-1234567 PAYMENT",
                        Map("amount" -> "29.99")
                    )
                yield assertTrue(
                    result._1.toLowerCase.contains("amazon"),
                    !result._1.contains("123-4567890-1234567")
                )
            },
            test("healthCheck should connect to the real API") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.healthCheck()
                yield assertCompletes
            }
        )
    ).provideLayer(
        OpenAIClientLive.live
    ) @@ TestAspect.withLiveClock @@ TestAspect.ifEnvSet(
        "OPENAI_API_KEY"
    ) @@ TestAspect.withLiveEnvironment
end OpenAIClientLiveSpec
