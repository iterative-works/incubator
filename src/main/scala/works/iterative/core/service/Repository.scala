package works.iterative.core.service

import zio.UIO

/** Base repository interface for domain entities
  *
  * @tparam K
  *   The type of the entity's key/ID
  * @tparam V
  *   The type of the entity
  * @tparam Q
  *   The type of query used to filter entities
  */
trait Repository[K, V, Q]:
    /** Find entities matching the given query
      *
      * @param query
      *   The query criteria
      * @return
      *   A sequence of entities matching the criteria
      */
    def find(query: Q): UIO[Seq[V]]

    /** Save an entity
      *
      * @param key
      *   The entity's ID/key
      * @param value
      *   The entity to save
      * @return
      *   Unit effect indicating success
      */
    def save(key: K, value: V): UIO[Unit]

    /** Load an entity by ID
      *
      * @param id
      *   The entity ID to load
      * @return
      *   An optional entity if found
      */
    def load(id: K): UIO[Option[V]]
end Repository