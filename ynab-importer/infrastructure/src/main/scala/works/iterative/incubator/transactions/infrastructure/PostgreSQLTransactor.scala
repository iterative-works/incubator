package works.iterative.incubator.transactions.infrastructure

import zio.*
import com.augustnagro.magnum.magzio.*

/** Provides a shared Magnum Transactor for PostgreSQL database operations
  */
class PostgreSQLTransactor(val transactor: Transactor)

object PostgreSQLTransactor:
    val layer: ZLayer[PostgreSQLDataSource & Scope, Throwable, Transactor] =
        ZLayer.service[PostgreSQLDataSource].flatMap { env =>
            Transactor.layer(env.get[PostgreSQLDataSource].dataSource)
        }

    val managedLayer: ZLayer[PostgreSQLDataSource & Scope, Throwable, PostgreSQLTransactor] =
        layer >>> ZLayer.fromFunction(PostgreSQLTransactor.apply)
end PostgreSQLTransactor
