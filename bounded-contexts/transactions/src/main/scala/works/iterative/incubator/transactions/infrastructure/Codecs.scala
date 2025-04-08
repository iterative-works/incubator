package works.iterative.incubator.transactions
package infrastructure

import zio.json.*
import domain.model.*

object Codecs extends JsonCodecs

trait JsonCodecs:
    given JsonCodec[TransactionId] = DeriveJsonCodec.gen[TransactionId]
    given JsonCodec[TransactionStatus] = DeriveJsonCodec.gen[TransactionStatus]
    given JsonCodec[Transaction] = DeriveJsonCodec.gen[Transaction]
end JsonCodecs
