package works.iterative.incubator.fio.infrastructure.config

import zio.Config

/** Configuration for Fio Bank API
 * 
 * Classification: Infrastructure Configuration
 */
case class FioConfig(token: String)

object FioConfig:
    val config: Config[FioConfig] =
        import Config.*
        (string("token")).nested("fio").map(
            FioConfig.apply
        )
    end config
end FioConfig