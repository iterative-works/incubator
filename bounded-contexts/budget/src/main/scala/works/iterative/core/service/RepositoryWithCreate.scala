package works.iterative.core.service

import zio.*

/** Base trait for repositories that can create entities.
  *
  * @tparam K The key type
  * @tparam T The entity type
  * @tparam Q The query type
  * @tparam C The creation type
  */
trait RepositoryWithCreate[K, T, Q, C <: Create[T]] extends Repository[K, T, Q] with GenericCreateRepository[K, C]

/** A trait for repository objects that can create entities and return their IDs.
  *
  * @tparam K The key type
  * @tparam C The creation type
  */
trait GenericCreateRepository[K, C]:
    /** Create a new entity from the creation object
      *
      * @param value The creation object
      * @return The ID of the created entity
      */
    def create(value: C): UIO[K]
end GenericCreateRepository

/** A trait for repository objects that can find entities by query.
  *
  * @tparam T The entity type
  * @tparam Q The query type
  */
trait GenericFindService[T, Q]:
    /** Find entities by query
      *
      * @param filter Query criteria
      * @return Matching entities
      */
    def find(filter: Q): UIO[Seq[T]]
end GenericFindService

/** A trait for repository objects that can load entities by ID.
  *
  * @tparam K The key type
  * @tparam T The entity type
  */
trait GenericLoadService[K, T]:
    /** Load an entity by ID
      *
      * @param id Entity ID
      * @return The entity if found, None otherwise
      */
    def load(id: K): UIO[Option[T]]
end GenericLoadService

/** Marker trait for creation classes.
  *
  * @tparam T The type of entity this creates
  */
trait Create[+T]