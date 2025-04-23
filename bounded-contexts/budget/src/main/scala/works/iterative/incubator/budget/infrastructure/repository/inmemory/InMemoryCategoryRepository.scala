package works.iterative.incubator.budget.infrastructure.repository.inmemory

import zio.*
import works.iterative.incubator.budget.domain.repository.CategoryRepository
import works.iterative.incubator.budget.domain.model.Category
import works.iterative.incubator.budget.domain.query.CategoryQuery

/** In-memory implementation of CategoryRepository for testing
  *
  * This repository holds category data in memory, without persistence to a database.
  * It's useful for testing, development, and UI-first implementation.
  *
  * Classification: Infrastructure Repository Implementation (Test Double)
  */
class InMemoryCategoryRepository extends CategoryRepository:
    private val storage: Ref[Map[String, Category]] =
        Unsafe.unsafely:
            Ref.unsafe.make(Map.empty[String, Category])

    /** Find categories matching the given query
      */
    override def find(query: CategoryQuery): UIO[Seq[Category]] =
        storage.get.map { categories =>
            categories.values
                .filter { category =>
                    // Filter by ID if provided
                    query.id.forall(_ == category.id) &&
                    // Filter by name if provided
                    query.name.forall(name => category.name.contains(name)) &&
                    // Filter by parent ID if provided
                    query.parentId.forall(pid => category.parentId.contains(pid)) &&
                    // Filter by active flag if provided
                    query.active.forall(_ == category.active) &&
                    // Filter by top-level status if provided
                    query.isTopLevel.forall(isTop =>
                        if isTop then category.parentId.isEmpty else category.parentId.isDefined
                    )
                }
                .toSeq
        }

    /** Save a category
      */
    override def save(key: String, value: Category): UIO[Unit] =
        storage.update(_ + (key -> value))

    /** Load a category by ID
      */
    override def load(id: String): UIO[Option[Category]] =
        storage.get.map(_.get(id))

    /** Add a batch of categories (for testing)
      */
    def addBatch(categories: Seq[Category]): UIO[Unit] =
        storage.update(existing => existing ++ categories.map(category => category.id -> category))

    /** Reset the repository (for testing)
      */
    def reset(): UIO[Unit] =
        storage.set(Map.empty)

    /** Find all top-level categories
      */
    def findTopLevel(): UIO[Seq[Category]] =
        find(CategoryQuery(isTopLevel = Some(true), active = Some(true)))

    /** Find all subcategories for a parent category
      */
    def findSubcategories(parentId: String): UIO[Seq[Category]] =
        find(CategoryQuery(parentId = Some(parentId), active = Some(true)))
end InMemoryCategoryRepository

object InMemoryCategoryRepository:
    /** Create a new in-memory repository
      */
    def make(): UIO[InMemoryCategoryRepository] =
        ZIO.succeed(new InMemoryCategoryRepository())

    /** ZIO layer for the repository
      */
    val layer: ULayer[CategoryRepository] =
        ZLayer.succeed(new InMemoryCategoryRepository())

    /** ZIO layer with access to the concrete implementation
      */
    val testLayer: ULayer[InMemoryCategoryRepository] =
        ZLayer.succeed(new InMemoryCategoryRepository())
end InMemoryCategoryRepository