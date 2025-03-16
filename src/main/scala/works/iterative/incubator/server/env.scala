package works.iterative.incubator
package server

import zio.*

type AppEnv =
    transactions.service.TransactionRepository & transactions.service.TransactionImportService

type AppTask[A] = RIO[AppEnv, A]
