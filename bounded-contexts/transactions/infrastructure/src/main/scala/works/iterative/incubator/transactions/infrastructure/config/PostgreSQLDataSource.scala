package works.iterative.incubator.transactions.infrastructure.config

import zio.*
import javax.sql.DataSource
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/** Provides a shared DataSource for PostgreSQL connections
  *
  * This class manages a connection pool for PostgreSQL using HikariCP.
  *
  * Classification: Infrastructure Configuration
  */
class PostgreSQLDataSource(val dataSource: DataSource)

object PostgreSQLDataSource:
    val layer: ZLayer[Scope, Throwable, DataSource] = ZLayer.scoped:
        for
            _ <- ZIO.attempt(Class.forName("org.postgresql.Driver"))
            config <- ZIO.config[PostgreSQLConfig](PostgreSQLConfig.config)
            dataSource <- ZIO.attempt {
                val conf = HikariConfig()
                // TODO: use configurable properties
                conf.setJdbcUrl(config.jdbcUrl)
                conf.setUsername(config.username)
                conf.setPassword(config.password)
                conf.setMaximumPoolSize(10)
                conf.setMinimumIdle(5)
                conf.setConnectionTimeout(30000)
                conf.setIdleTimeout(600000)
                conf.setMaxLifetime(1800000)
                conf.setInitializationFailTimeout(-1)
                HikariDataSource(conf)
            }
        yield dataSource

    val managedLayer: ZLayer[Scope, Throwable, PostgreSQLDataSource] =
        layer >>> ZLayer.fromFunction(PostgreSQLDataSource.apply)
end PostgreSQLDataSource
