package works.iterative.incubator.ynab.infrastructure.persistence

import zio.*
import works.iterative.incubator.ynab.domain.model.{CreateYnabAccountMapping, YnabAccountMapping}
import works.iterative.incubator.ynab.domain.repository.YnabAccountMappingRepository
import works.iterative.sqldb.{PostgreSQLDataSource, PostgreSQLTransactor}
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import io.scalaland.chimney.dsl.*

/** PostgreSQL implementation of YnabAccountMappingRepository
  *
  * @param xa
  *   Transactor for executing database operations
  */
class PostgreSQLYnabAccountMappingRepository(xa: Transactor) extends YnabAccountMappingRepository:
    import PostgreSQLYnabAccountMappingRepository.{
        mappingRepo,
        YnabAccountMappingDTO,
        CreateYnabAccountMappingDTO
    }

    /** Find mapping by source account ID
      *
      * @param sourceAccountId
      *   ID of the source account
      * @return
      *   The mapping if found
      */
    override def findBySourceAccountId(sourceAccountId: Long)
        : IO[Throwable, Option[YnabAccountMapping]] =
        xa.connect {
            val spec = Spec[YnabAccountMappingDTO].where(sql"sourceAccountId = $sourceAccountId")
            mappingRepo.findAll(spec).headOption.map(_.toModel)
        }.orDie

    /** Find mapping by YNAB account ID
      *
      * @param ynabAccountId
      *   ID of the YNAB account
      * @return
      *   The mapping if found
      */
    override def findByYnabAccountId(ynabAccountId: String)
        : IO[Throwable, Option[YnabAccountMapping]] =
        xa.connect {
            val spec = Spec[YnabAccountMappingDTO].where(sql"ynabAccountId = $ynabAccountId")
            mappingRepo.findAll(spec).headOption.map(_.toModel)
        }.orDie

    /** Find all mappings
      *
      * @return
      *   List of all account mappings
      */
    override def findAll(): IO[Throwable, List[YnabAccountMapping]] =
        xa.connect {
            mappingRepo.findAll.map(_.toModel).toList
        }.orDie

    /** Find all active mappings
      *
      * @return
      *   List of all active account mappings
      */
    override def findAllActive(): IO[Throwable, List[YnabAccountMapping]] =
        xa.connect {
            val spec = Spec[YnabAccountMappingDTO].where(sql"active = true")
            mappingRepo.findAll(spec).map(_.toModel).toList
        }.orDie

    /** Save a new mapping
      *
      * @param mapping
      *   The mapping to save
      * @return
      *   The saved mapping
      */
    override def save(mapping: CreateYnabAccountMapping): IO[Throwable, YnabAccountMapping] =
        xa.transact {
            // Check if a mapping with this source_account_id already exists
            val spec =
                Spec[YnabAccountMappingDTO].where(sql"sourceAccountId = ${mapping.sourceAccountId}")

            mappingRepo.findAll(spec).headOption match
                case Some(_) =>
                    throw new RuntimeException(
                        s"Mapping for source account ID ${mapping.sourceAccountId} already exists"
                    )
                case None =>
                    val dto = CreateYnabAccountMappingDTO.fromModel(mapping)
                    val savedId = mappingRepo.insertReturning(dto).sourceAccountId
                    YnabAccountMapping(
                        sourceAccountId = savedId,
                        ynabAccountId = mapping.ynabAccountId,
                        active = mapping.active
                    )
            end match
        }.orDie

    /** Update an existing mapping
      *
      * @param mapping
      *   The mapping to update
      * @return
      *   The updated mapping
      */
    override def update(mapping: YnabAccountMapping): IO[Throwable, YnabAccountMapping] =
        xa.transact {
            val dto = YnabAccountMappingDTO.fromModel(mapping)
            mappingRepo.update(dto)
            mapping
        }.orDie

    /** Delete a mapping
      *
      * @param sourceAccountId
      *   ID of the source account
      * @return
      *   Unit if successful
      */
    override def delete(sourceAccountId: Long): IO[Throwable, Unit] =
        xa.transact {
            // Delete the mapping with the given source account ID
            val spec = Spec[YnabAccountMappingDTO].where(sql"sourceAccountId = $sourceAccountId")
            mappingRepo.findAll(spec).headOption.foreach { mapping =>
                mappingRepo.deleteById(mapping)
            }
        }.orDie
end PostgreSQLYnabAccountMappingRepository

object PostgreSQLYnabAccountMappingRepository:
    /** DTO for YNAB account mapping table */
    @SqlName("ynab_account_mappings")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class YnabAccountMappingDTO(
        sourceAccountId: Long,
        ynabAccountId: String,
        active: Boolean = true
    ) derives DbCodec:
        def toModel: YnabAccountMapping = this.into[YnabAccountMapping].transform
    end YnabAccountMappingDTO

    object YnabAccountMappingDTO:
        def fromModel(model: YnabAccountMapping): YnabAccountMappingDTO =
            model.into[YnabAccountMappingDTO].transform
    end YnabAccountMappingDTO

    /** DTO for creating new YNAB account mappings */
    @SqlName("ynab_account_mappings")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class CreateYnabAccountMappingDTO(
        sourceAccountId: Long,
        ynabAccountId: String,
        active: Boolean = true
    ) derives DbCodec

    object CreateYnabAccountMappingDTO:
        def fromModel(model: CreateYnabAccountMapping): CreateYnabAccountMappingDTO =
            model.into[CreateYnabAccountMappingDTO].transform
    end CreateYnabAccountMappingDTO

    // Repository definition using Magnum's Repo pattern
    val mappingRepo =
        Repo[CreateYnabAccountMappingDTO, YnabAccountMappingDTO, YnabAccountMappingDTO]

    /** ZIO layer for the repository */
    val layer: ZLayer[PostgreSQLTransactor, Nothing, YnabAccountMappingRepository] =
        ZLayer.fromFunction { (ts: PostgreSQLTransactor) =>
            new PostgreSQLYnabAccountMappingRepository(ts.transactor)
        }

    /** Full layer with all dependencies */
    val fullLayer: ZLayer[Scope, Throwable, YnabAccountMappingRepository] =
        PostgreSQLDataSource.managedLayer >>> PostgreSQLTransactor.managedLayer >>> layer
end PostgreSQLYnabAccountMappingRepository
