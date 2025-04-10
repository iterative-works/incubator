package works.iterative.incubator.fio.infrastructure.config

import zio.Config

/** Configuration for Fio Bank API
  *
  * Classification: Infrastructure Configuration
  */
case class FioConfig(
    defaultToken: Option[String] = None, // Optional default token for backwards compatibility
    apiUrl: String = "https://www.fio.cz/ib_api/rest"
)

object FioConfig:
    given config: Config[FioConfig] =
        import Config.*
        (
            string("token").optional ?? "Optional default token for backwards compatibility" zip
                string("api_url").withDefault(
                    "https://www.fio.cz/ib_api/rest"
                ) ?? "Fio Bank API base URL"
        ).nested("fio").map {
            case (token, apiUrl) => FioConfig(token, apiUrl)
        }
    end config
end FioConfig
