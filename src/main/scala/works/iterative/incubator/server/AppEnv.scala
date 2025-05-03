package works.iterative.incubator
package server

import zio.*
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportService

type AppEnv = TransactionImportService

type AppTask[A] = RIO[AppEnv, A]
