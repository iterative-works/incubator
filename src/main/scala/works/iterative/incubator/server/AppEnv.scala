package works.iterative.incubator
package server

import zio.*

type AppEnv =
    transactions.service.TransactionRepository &
        transactions.service.TransactionProcessingStateRepository &
        transactions.service.TransactionImportService &
        transactions.service.TransactionManagerService &
        transactions.service.TransactionProcessor

type AppTask[A] = RIO[AppEnv, A]
