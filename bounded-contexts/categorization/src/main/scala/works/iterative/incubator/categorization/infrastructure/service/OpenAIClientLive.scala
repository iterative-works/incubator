package works.iterative.incubator.categorization.infrastructure.service

import sttp.client4.*
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import sttp.openai.OpenAI
import sttp.openai.OpenAIExceptions.OpenAIException
import sttp.openai.requests.completions.chat.ChatRequestBody.{
    ChatBody,
    ChatCompletionModel,
    ResponseFormat
}
import sttp.openai.requests.completions.chat.ChatRequestResponseData.ChatResponse
import sttp.openai.requests.completions.chat.message.*
import works.iterative.incubator.categorization.domain.model.*
import works.iterative.incubator.categorization.domain.service.OpenAIClient
import works.iterative.incubator.categorization.infrastructure.config.OpenAIConfig

import zio.*
import sttp.openai.OpenAIUris

/** Live implementation of OpenAIClient that uses sttp-openai to communicate with OpenAI API
  *
  * This client handles:
  *   - Converting domain requests to OpenAI API format
  *   - Parsing OpenAI responses to domain models
  *   - Error handling and retries
  *   - Configuration of API connections
  */
case class OpenAIClientLive(
    config: OpenAIConfig,
    backend: Backend[Task]
) extends OpenAIClient:

    private val openAI =
        new OpenAI(
            config.apiKey.stringValue,
            config.baseUrl.map(Uri(_)).getOrElse(OpenAIUris.OpenAIBaseUri)
        )

    // System prompt template that instructs the model how to clean payee names
    private val systemPromptTemplate =
        """You are a financial transaction payee name cleanup assistant. Your job is to convert messy, raw payee names from bank transactions into clean, consistent names that are easier to read and categorize.

      |Rules for cleaning up payee names:
      |1. Remove unnecessary transaction details like reference numbers, dates, card numbers
      |2. Remove generic payment method descriptions (e.g., "PAYMENT", "DEBIT", "CARD PURCHASE")
      |3. Fix capitalization (avoid ALL CAPS, use Title Case for business names)
      |4. Ensure consistency (e.g., "McDonald's" not "McDonalds", "Mcdonald", etc.)
      |5. Spell out common abbreviations if obvious (e.g., "Ltd" → "Limited")
      |6. Remove location identifiers unless they help distinguish the merchant (e.g., "STARBUCKS MAIN ST" → "Starbucks")
      |7. Preserve meaningful information about the transaction purpose

      |Response format:
      |{
      |  "cleaned_payee": "The cleaned payee name",
      |  "confidence": 0.95, // Float between 0 and 1
      |  "rule_suggestion": {
      |    "pattern": "string that matches the original name",
      |    "pattern_type": "EXACT | CONTAINS | STARTS_WITH | REGEX",
      |    "replacement": "consistent replacement name",
      |    "explanation": "why this rule makes sense"
      |  } // If you can identify a reusable pattern, otherwise null
      |}""".stripMargin

    /** Clean up a payee name using OpenAI
      *
      * This method:
      *   1. Constructs a prompt with the transaction context 2. Sends the request to OpenAI with
      *      retry logic 3. Parses the JSON response to extract the cleaned name and rule suggestion
      */
    override def cleanupPayee(
        original: String,
        context: Map[String, String]
    ): IO[OpenAIError, (String, Option[PayeeCleanupRule])] =
        // Build messages for the chat completion
        val messages = buildMessagesForCleanup(original, context)

        // Create request body
        val requestBody = ChatBody(
            model = chatCompletionModelFromConfig(config.model),
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            responseFormat = Some(ResponseFormat.JsonObject)
        )

        // Send request with retry logic
        callOpenAIWithRetry(
            openAI.createChatCompletion(requestBody).send(backend)
        ).flatMap(response => parseCleanupResponse(original, response))
    end cleanupPayee

    /** Health check to verify OpenAI API connectivity */
    override def healthCheck(): IO[OpenAIError, Unit] =
        val messages = Seq(
            Message.SystemMessage(content = "You are a helpful assistant."),
            Message.UserMessage(
                content =
                    Content.TextContent("Hello, this is a health check. Please respond with 'OK'.")
            )
        )

        val requestBody = ChatBody(
            model = chatCompletionModelFromConfig(config.model),
            messages = messages,
            maxTokens = Some(5)
        )

        callOpenAIWithRetry(
            openAI.createChatCompletion(requestBody).send(backend)
        ).flatMap { response =>
            val text = response.choices.headOption.map(_.message.content).getOrElse("")
            if text.contains("OK") then ZIO.unit
            else
                ZIO.fail(OpenAIError.UnexpectedError(
                    "Health check failed, unexpected response",
                    None
                ))
            end if
        }
    end healthCheck

    /** Build message sequence for OpenAI chat completion */
    private def buildMessagesForCleanup(
        original: String,
        context: Map[String, String]
    ): Seq[Message] =
        // Create system message with instructions
        val systemMessage = Message.SystemMessage(
            content = systemPromptTemplate
        )

        // Build context information
        val contextInfo = context.map { case (key, value) =>
            s"|$key: $value"
        }.mkString("\n")

        // Create user message with the request
        val userMessage = Message.UserMessage(
            content = Content.TextContent(
                s"""Please clean up this raw transaction payee name:
                   |
                   |Original payee name: "$original"
                   |
                   |Additional context:
                   $contextInfo
                   |
                   |Respond in the JSON format specified in your instructions.
                   |""".stripMargin
            )
        )

        Seq(systemMessage, userMessage)
    end buildMessagesForCleanup

    /** Apply retry logic to OpenAI API calls */
    private def callOpenAIWithRetry[A](
        effect: Task[Response[Either[OpenAIException, A]]]
    ): IO[OpenAIError, A] =
        effect
            .retry(
                Schedule.exponential(100.milliseconds) &&
                    Schedule.recurs(config.maxRetries)
            )
            .flatMap { response =>
                response.body match
                    case Right(body) => ZIO.succeed(body)
                    case Left(error) => ZIO.fail(mapOpenAIExceptionToDomain(error))
            }
            .mapError {
                case error: OpenAIError => error
                case throwable =>
                    OpenAIError.ConnectionError(
                        s"Connection error: ${throwable.getMessage}",
                        Some(throwable)
                    )
            }
            .timeoutFail(
                OpenAIError.ConnectionError("Request timed out", None)
            )(30.seconds)

    /** Parse the JSON response from OpenAI into domain types */
    private def parseCleanupResponse(
        original: String,
        response: ChatResponse
    ): IO[OpenAIError, (String, Option[PayeeCleanupRule])] =
        ZIO.attempt {
            // Extract response text
            val responseText = response.choices.headOption
                .map(_.message.content)
                .getOrElse(throw new RuntimeException("No content in OpenAI response"))

            // Parse JSON manually since we don't want to introduce another JSON library dependency
            // This is a simplified parsing implementation - in production you might want to use a real JSON parser
            val jsonMap = parseSimpleJson(responseText)

            // Extract cleaned payee name
            val cleanedPayee = jsonMap.getOrElse("cleaned_payee", original)

            // Extract confidence
            val confidence = jsonMap.get("confidence")
                .flatMap(s => s.toDoubleOption)
                .getOrElse(0.7) // Default confidence

            // Extract rule suggestion if present
            val ruleSuggestion = jsonMap.get("rule_suggestion").flatMap { ruleSuggestionStr =>
                if ruleSuggestionStr == "null" then None
                else
                    val ruleMap = parseSimpleJson(ruleSuggestionStr)

                    val pattern = ruleMap.getOrElse("pattern", "")
                    val patternTypeStr = ruleMap.getOrElse("pattern_type", "CONTAINS")
                    val replacement = ruleMap.getOrElse("replacement", cleanedPayee)

                    // Map pattern type string to domain PatternType
                    val patternType = patternTypeStr match
                        case "EXACT"       => PatternType.Exact
                        case "REGEX"       => PatternType.Regex
                        case "STARTS_WITH" => PatternType.StartsWith
                        case _             => PatternType.Contains

                    // Create rule if we have valid pattern and replacement
                    if pattern.nonEmpty && replacement.nonEmpty then
                        Some(PayeeCleanupRule.newFromLLM(
                            pattern = pattern,
                            patternType = patternType,
                            replacement = replacement,
                            confidence = confidence
                        ))
                    else None
                    end if
            }

            (cleanedPayee, ruleSuggestion)
        }.mapError(ex =>
            OpenAIError.ResponseParsingError(
                s"Failed to parse OpenAI response: ${ex.getMessage}",
                Some(ex)
            )
        )

    /** Simple JSON parser for OpenAI responses (only handles basic key-value pairs and nested
      * objects)
      */
    private def parseSimpleJson(json: String): Map[String, String] =
        val keyValuePattern = """"([^"]+)"\s*:\s*(?:"([^"]*)"|(null|\d+(?:\.\d+)?))""".r
        val nestedObjectPattern = """"([^"]+)"\s*:\s*(\{[^}]+\})""".r

        val simpleMatches = keyValuePattern.findAllMatchIn(json).map { m =>
            (m.group(1), Option(m.group(2)).getOrElse(m.group(3)))
        }.toMap

        // Handle nested objects
        val nestedMatches = nestedObjectPattern.findAllMatchIn(json).map { m =>
            (m.group(1), m.group(2))
        }.toMap

        simpleMatches ++ nestedMatches
    end parseSimpleJson

    /** Map sttp-openai exceptions to our domain exceptions */
    private def mapOpenAIExceptionToDomain(exception: OpenAIException): OpenAIError =
        exception match
            case _: OpenAIException.AuthenticationException =>
                OpenAIError.AuthenticationError(exception.getMessage)
            case _: OpenAIException.RateLimitException =>
                OpenAIError.RateLimitError(exception.getMessage)
            case _: OpenAIException.APIException if exception.getMessage.contains("validation") =>
                OpenAIError.ValidationError(exception.getMessage)
            case _: OpenAIException.APIException if exception.getMessage.contains("unavailable") =>
                OpenAIError.ServiceUnavailableError(exception.getMessage)
            case _ =>
                OpenAIError.UnexpectedError(
                    s"OpenAI API error: ${exception.getMessage}",
                    Some(exception)
                )

    /** Convert config model string to ChatCompletionModel */
    private def chatCompletionModelFromConfig(modelName: String): ChatCompletionModel =
        modelName match
            case "gpt-4"         => ChatCompletionModel.GPT4
            case "gpt-4o"        => ChatCompletionModel.GPT4o
            case "gpt-4o-mini"   => ChatCompletionModel.GPT4oMini
            case "gpt-3.5-turbo" => ChatCompletionModel.GPT35Turbo
            case _               => ChatCompletionModel.CustomChatCompletionModel(modelName)
end OpenAIClientLive

object OpenAIClientLive:
    /** Layer that constructs OpenAIClientLive from its dependencies */
    val layer: ZLayer[Backend[Task], Config.Error, OpenAIClient] =
        ZLayer {
            for
                config <- ZIO.config[OpenAIConfig]
                backend <- ZIO.service[Backend[Task]]
            yield OpenAIClientLive(config, backend)
        }

    /** Live layer that includes all dependencies */
    val live: ZLayer[Any, Throwable, OpenAIClient] =
        HttpClientZioBackend.layer() >+> layer

    /** Test layer that takes an explicit configuration */
    def testLayer(config: OpenAIConfig): ZLayer[Any, Throwable, OpenAIClient] =
        ZLayer.succeed(config) >+>
            HttpClientZioBackend.layer() >+>
            layer
end OpenAIClientLive
