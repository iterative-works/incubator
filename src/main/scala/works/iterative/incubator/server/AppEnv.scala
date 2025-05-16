package works.iterative.incubator
package server

import zio.*
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportPresenter
import works.iterative.sqldb.PostgreSQLDatabaseSupport
import works.iterative.incubator.budget.TransactionImportModule

type AppEnv = TransactionImportPresenter

type AppTask[A] = RIO[AppEnv, A]

/** Companion object with layer definitions */
object AppEnv:
    /** Live layer that provides all required services */
    val live: ZLayer[Scope, Throwable, AppEnv] =
        ZLayer.makeSome[Scope, AppEnv](
            PostgreSQLDatabaseSupport.layerWithMigrations(),
            TransactionImportModule.liveLayer
        )
end AppEnv
