package works.iterative.incubator
package server

import zio.*
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportService
import works.iterative.incubator.budget.ui.transaction_import.MockTransactionImportService
import works.iterative.server.http.ScalatagsViteSupport

type AppEnv = TransactionImportService

type AppTask[A] = RIO[AppEnv, A]

/** Companion object with layer definitions */
object AppEnv:
  /** Live layer that provides all required services */
  val live: ZLayer[ScalatagsViteSupport, Nothing, AppEnv] =
    ZLayer.make[AppEnv](
      // Use MockTransactionImportService for the prototype
      MockTransactionImportService.layer
    )
end AppEnv
