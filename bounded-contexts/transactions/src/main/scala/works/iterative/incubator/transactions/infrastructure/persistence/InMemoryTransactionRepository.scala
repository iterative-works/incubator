package works.iterative.incubator.transactions.infrastructure.persistence

import zio.*
import zio.stream.*
import zio.json.*
import works.iterative.incubator.transactions.domain.repository.TransactionRepository
import works.iterative.incubator.transactions.domain.model.{Transaction, TransactionId}
import works.iterative.incubator.transactions.domain.query.TransactionQuery
import works.iterative.incubator.transactions.infrastructure.Codecs.given

/** In-memory implementation of TransactionRepository, primarily for testing
  *
  * This repository holds transaction data in memory, without persistence to a database.
  * It's useful for testing and development.
  *
  * Classification: Infrastructure Repository Implementation (Test Double)
  */
class InMemoryTransactionRepository(transactions: List[Transaction])
    extends TransactionRepository:
    override def find(filter: TransactionQuery): UIO[Seq[Transaction]] =
        ZIO.succeed(transactions)
    override def save(key: TransactionId, value: Transaction): UIO[Unit] = ???
    override def load(id: TransactionId): UIO[Option[Transaction]] = ???
end InMemoryTransactionRepository

object InMemoryTransactionRepository:
    def fromJsonResource(resourcePath: String): ZLayer[Any, Throwable, TransactionRepository] =
        ZLayer {
            for
                stream <- ZStream.fromResource("mock_transactions.json")
                    .via(zio.stream.ZPipeline.utf8Decode)
                    .runCollect
                jsonStr = stream.mkString
                transactions <- ZIO.fromEither(jsonStr.fromJson[List[Transaction]])
                    .mapError(err =>
                        new RuntimeException(s"Failed to parse transaction data: $err")
                    )
            yield InMemoryTransactionRepository(transactions)
        }
end InMemoryTransactionRepository