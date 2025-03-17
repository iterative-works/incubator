package works.iterative.incubator.transactions
package infrastructure
package adapter.fio

import zio.Config

case class FioConfig(token: String)

object FioConfig:
    val config: Config[FioConfig] =
        import Config.*
        (string("token")).nested("fio").map(
            FioConfig.apply
        )
    end config
end FioConfig
