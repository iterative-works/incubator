package works.iterative.incubator
package server

import zio.*

type AppEnv =
    transactions.domain.repository.TransactionRepository &
        transactions.domain.repository.TransactionProcessingStateRepository &
        transactions.application.service.TransactionImportService &
        transactions.application.service.TransactionManagerService &
        transactions.application.service.TransactionProcessor &
        transactions.domain.repository.SourceAccountRepository

type AppTask[A] = RIO[AppEnv, A]
