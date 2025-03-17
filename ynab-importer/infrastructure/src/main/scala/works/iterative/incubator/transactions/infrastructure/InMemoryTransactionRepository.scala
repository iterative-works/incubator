package works.iterative.incubator.transactions
package infrastructure

import zio.*
import zio.stream.*
import zio.json.*
import Codecs.given
import service.TransactionRepository

class InMemoryTransactionRepository(transactions: List[Transaction])
    extends TransactionRepository:
    override def find(filter: TransactionQuery): UIO[List[Transaction]] =
        ZIO.succeed(transactions)
    override def save(key: TransactionId, value: Transaction): UIO[Unit] = ???
    override def load(id: TransactionId): UIO[Option[Transaction]] = ???
    override def loadAll(ids: Seq[TransactionId]): UIO[List[Transaction]] = ???
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
