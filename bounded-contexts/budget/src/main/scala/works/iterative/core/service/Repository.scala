package works.iterative.core.service

import zio.*

/** Base Repository interface providing common operations for entities.
  *
  * @tparam K The key type used to identify entities
  * @tparam T The entity type stored in the repository
  * @tparam Q The query type used for searching entities
  */
trait Repository[K, T, Q]:
    /** Find entities matching the given query
      *
      * @param query The query criteria
      * @return A sequence of entities matching the criteria
      */
    def find[Q](query: Q): UIO[Seq[T]]
    
    /** Save an entity
      *
      * @param key The key to store the entity under
      * @param value The entity to save
      * @return Unit effect indicating success
      */
    def save(key: K, value: T): UIO[Unit]
    
    /** Find an entity by its key
      *
      * @param id The key to look up
      * @return The entity if found, None otherwise
      */
    def findById(id: K): UIO[Option[T]]
end Repository