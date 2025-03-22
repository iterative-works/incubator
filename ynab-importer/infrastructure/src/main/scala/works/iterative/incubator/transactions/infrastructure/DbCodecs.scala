package works.iterative.incubator.transactions
package infrastructure

import com.augustnagro.magnum.magzio.*
import java.time.Instant

/** Shared database codecs for use across repository implementations */
object DbCodecs:
    // Common DbCodec for Instant used across repositories
    given DbCodec[Instant] = DbCodec.SqlTimestampCodec.biMap(
        i => i.toInstant,
        i => java.sql.Timestamp.from(i)
    )
    // Option[Instant] will be handled automatically
end DbCodecs