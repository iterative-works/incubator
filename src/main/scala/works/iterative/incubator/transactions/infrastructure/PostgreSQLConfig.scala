package works.iterative.incubator.transactions.infrastructure

import zio.*

case class PostgreSQLConfig(jdbcUrl: String, username: String, password: String)

object PostgreSQLConfig:
    val config: Config[PostgreSQLConfig] =
        import Config.*
        (string("url") zip string("username") zip string("password")).nested("pg").map(
            PostgreSQLConfig.apply
        )
    end config
end PostgreSQLConfig
