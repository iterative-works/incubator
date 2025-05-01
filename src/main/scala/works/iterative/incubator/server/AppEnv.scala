package works.iterative.incubator
package server

import zio.*

type AppEnv = Any

type AppTask[A] = RIO[AppEnv, A]
