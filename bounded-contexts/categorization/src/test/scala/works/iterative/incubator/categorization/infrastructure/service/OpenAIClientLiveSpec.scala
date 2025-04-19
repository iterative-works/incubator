package works.iterative.incubator.categorization.infrastructure.service

import sttp.client4.Backend
import sttp.model.StatusCode
import works.iterative.incubator.categorization.domain.model.*
import works.iterative.incubator.categorization.domain.service.OpenAIClient
import works.iterative.incubator.categorization.infrastructure.config.OpenAIConfig

import zio.*
import zio.test.*
import zio.test.Assertion.*
import sttp.client4.httpclient.zio.HttpClientZioBackend

object OpenAIClientLiveSpec extends ZIOSpecDefault:
    // Sample test data
    val testPayee = "ACME CORP LTD UK REF123456 PAYMENT"
    val testContext = Map(
        "amount" -> "123.45",
        "date" -> "2023-10-15",
        "counterAccount" -> "GB12345678901234"
    )

    // Test configuration
    val testConfig = OpenAIConfig(
        apiKey = Config.Secret("test-api-key"),
        model = "gpt-4o-mini",
        maxRetries = 2,
        temperature = Some(0.7),
        maxTokens = Some(500)
    )

    // Config Layer for tests
    val mockConfigProvider: ZLayer[Any, Nothing, Unit] =
        Runtime.setConfigProvider(ConfigProvider.fromMap(Map(
            "openai.api.key" -> "test-api-key",
            "openai.model" -> "gpt-4o-mini",
            "openai.maxRetries" -> "2",
            "openai.temperature" -> "0.7",
            "openai.maxTokens" -> "500"
        )))

    // Valid JSON response from OpenAI
    val validJsonResponse = """
    {
      "id": "chatcmpl-123",
      "object": "chat.completion",
      "created": 1698335275,
      "model": "gpt-4o-mini",
      "choices": [
        {
          "index": 0,
          "message": {
            "role": "assistant",
            "content": "{\n  \"cleaned_payee\": \"Acme Corp\",\n  \"confidence\": 0.95,\n  \"rule_suggestion\": {\n    \"pattern\": \"ACME CORP\",\n    \"pattern_type\": \"CONTAINS\",\n    \"replacement\": \"Acme Corp\",\n    \"explanation\": \"Removes Ltd, UK, reference numbers and standardizes capitalization\"\n  }\n}"
          },
          "finish_reason": "stop"
        }
      ],
      "usage": {
        "prompt_tokens": 100,
        "completion_tokens": 50,
        "total_tokens": 150
      }
    }"""

    // Invalid JSON response from OpenAI
    val invalidJsonResponse = """
    {
      "id": "chatcmpl-123",
      "object": "chat.completion",
      "created": 1698335275,
      "model": "gpt-4o-mini",
      "choices": [
        {
          "index": 0,
          "message": {
            "role": "assistant",
            "content": "I'll help you clean up that payee name."
          },
          "finish_reason": "stop"
        }
      ],
      "usage": {
        "prompt_tokens": 100,
        "completion_tokens": 50,
        "total_tokens": 150
      }
    }"""

    // Health check response
    val healthCheckResponse = """
    {
      "id": "chatcmpl-123",
      "object": "chat.completion",
      "created": 1698335275,
      "model": "gpt-4o-mini",
      "choices": [
        {
          "index": 0,
          "message": {
            "role": "assistant",
            "content": "OK"
          },
          "finish_reason": "stop"
        }
      ],
      "usage": {
        "prompt_tokens": 10,
        "completion_tokens": 2,
        "total_tokens": 12
      }
    }"""

    // Create test backend that returns predetermined responses
    def createTestBackend: UIO[Backend[Task]] =
        ZIO.succeed {
            // Start with a stub backend
            HttpClientZioBackend.stub
                // Match cleanupPayee request
                .whenRequestMatches(r =>
                    r.uri.path.lastOption.contains("chat/completions") &&
                        r.body.toString.contains(testPayee)
                )
                .thenRespondAdjust(validJsonResponse)

                // Match health check request
                .whenRequestMatches(r =>
                    r.uri.path.lastOption.contains("chat/completions") &&
                        r.body.toString.contains("health check")
                )
                .thenRespondAdjust(healthCheckResponse)

                // Match authentication error
                .whenRequestMatches(r =>
                    r.uri.path.lastOption.contains("chat/completions") &&
                        r.body.toString.contains("trigger_auth_error")
                )
                .thenRespondAdjust(
                    """{"error":{"message":"Invalid Authentication","type":"invalid_request_error"}}""",
                    StatusCode.Unauthorized
                )

                // Match rate limit error
                .whenRequestMatches(r =>
                    r.uri.path.lastOption.contains("chat/completions") &&
                        r.body.toString.contains("trigger_rate_limit")
                )
                .thenRespondAdjust(
                    """{"error":{"message":"Rate limit exceeded","type":"rate_limit_error"}}""",
                    StatusCode.TooManyRequests
                )

                // Match invalid JSON response
                .whenRequestMatches(r =>
                    r.uri.path.lastOption.contains("chat/completions") &&
                        r.body.toString.contains("trigger_invalid_json")
                )
                .thenRespondAdjust(invalidJsonResponse)
        }

    // Tests
    def spec = suite("OpenAIClientLive")(
        suite("Unit tests with mock backend")(
            test("cleanupPayee should parse a valid response") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.cleanupPayee(testPayee, testContext)
                yield assertTrue(
                    result._1 == "Acme Corp",
                    result._2.isDefined,
                    result._2.exists(_.pattern == "ACME CORP"),
                    result._2.exists(_.replacement == "Acme Corp"),
                    result._2.exists(_.patternType == PatternType.Contains)
                )
            },
            test("healthCheck should succeed with OK response") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.healthCheck()
                yield assertCompletes
            },
            test("cleanupPayee should handle authentication errors") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.cleanupPayee("trigger_auth_error", testContext).exit
                yield assert(result)(fails(isSubtype[OpenAIError.AuthenticationError](anything)))
            },
            test("cleanupPayee should handle rate limit errors") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.cleanupPayee("trigger_rate_limit", testContext).exit
                yield assert(result)(fails(isSubtype[OpenAIError.RateLimitError](anything)))
            },
            test("cleanupPayee should handle invalid JSON responses") {
                for
                    client <- ZIO.service[OpenAIClient]
                    result <- client.cleanupPayee("trigger_invalid_json", testContext).exit
                yield assert(result)(fails(isSubtype[OpenAIError.ResponseParsingError](anything)))
            }
        ).provideLayer(OpenAIClientLive.live),

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
        ).provide(
            mockConfigProvider,
            OpenAIClientLive.live
        ) @@ TestAspect.ifEnvSet("OPENAI_API_KEY")
    )
end OpenAIClientLiveSpec
