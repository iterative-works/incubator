package works.iterative.incubator
package server

import zio.*

type AppEnv = transactions.service.TransactionRepository

type AppTask[A] = RIO[AppEnv, A]
