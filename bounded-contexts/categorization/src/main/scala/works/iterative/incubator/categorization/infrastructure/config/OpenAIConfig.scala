package works.iterative.incubator.categorization.infrastructure.config

import zio.*
import java.net.URI

/** Configuration for OpenAI client
  *
  * Contains all necessary configuration options for connecting to the OpenAI API.
  */
case class OpenAIConfig(
    apiKey: Config.Secret,
    model: String,
    maxRetries: Int,
    baseUrl: Option[URI] = None,
    temperature: Option[Double] = None,
    maxTokens: Option[Int] = None
)

object OpenAIConfig:
    given config: Config[OpenAIConfig] =
        import Config.*
        (secret("key").nested("api") zip
            string("model") zip
            int("maxRetries") zip
            uri("baseUrl").optional zip
            double("temperature").optional zip
            int("maxTokens").optional).nested("openai").map(OpenAIConfig.apply)
    end config
end OpenAIConfig
