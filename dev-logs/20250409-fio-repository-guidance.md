# Fio Repository Implementation Guidance - 2025-04-09

## Repository Integration with Magnum Library

Based on the analysis of the codebase and the Obsidian vault documentation, here's a comprehensive guide for implementing the Fio repositories correctly:

### 1. Key Architecture Patterns

The project uses several key architecture patterns for repository implementation:

1. **Magnum Library Integration**:
   - Magnum provides a type-safe SQL interface that works with ZIO
   - All PostgreSQL repositories should use Magnum for database access
   - DTOs are defined with `@Table` and `@SqlName` annotations and `derives DbCodec`

2. **Repository Layer Pattern**:
   - Domain repositories define interfaces in `domain/repository`
   - Infrastructure implements these repositories in `infrastructure/persistence`
   - Use constructor injection for dependencies like `Transactor`

3. **Data Transfer Objects (DTOs)**:
   - Define DTOs in the repository implementation class
   - Use chimney for transformations between DTOs and domain models
   - Follow naming conventions: `EntityDTO` for entities, `CreateEntityDTO` for creators

4. **ZIO Integration**:
   - Use `xa.connect` for read operations and `xa.transact` for write operations
   - Return `UIO` (infallible effects) from repository methods
   - Handle errors with `.orDie` unless specific error handling is needed

### 2. Creating PostgreSQLFioImportStateRepository

The correct implementation would follow this structure:

```scala
// PostgreSQLFioImportStateRepository.scala
package works.iterative.incubator.fio.infrastructure.persistence

import zio.*
import java.time.Instant
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.transactions.infrastructure.config.PostgreSQLTransactor
import com.augustnagro.magnum.PostgresDbType
import com.augustnagro.magnum.magzio.*
import io.scalaland.chimney.dsl.*

class PostgreSQLFioImportStateRepository(xa: Transactor) extends FioImportStateRepository:
    import PostgreSQLFioImportStateRepository.{importStateRepo, FioImportStateDTO}
    
    override def getImportState(sourceAccountId: Long): Task[Option[FioImportState]] =
        xa.connect {
            importStateRepo.findAll(
                Spec[FioImportStateDTO]
                    .where(sql"source_account_id = $sourceAccountId")
                    .limit(1)
            ).headOption.map(_.toDomain)
        }.orDie
            
    override def updateImportState(state: FioImportState): Task[Unit] =
        xa.transact {
            val dto = FioImportStateDTO.fromDomain(state)
            
            // Check if exists
            val exists = importStateRepo.findAll(
                Spec[FioImportStateDTO]
                    .where(sql"source_account_id = ${state.sourceAccountId}")
                    .limit(1)
            ).nonEmpty
            
            if exists then importStateRepo.update(dto)
            else importStateRepo.insert(dto)
        }.orDie

object PostgreSQLFioImportStateRepository:
    // DTO for FioImportState
    @SqlName("fio_import_state")
    @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase)
    case class FioImportStateDTO(
        sourceAccountId: Long,
        lastTransactionId: Option[Long],
        lastImportTimestamp: Instant
    ) derives DbCodec:
        def toDomain: FioImportState = this.into[FioImportState].transform
    
    object FioImportStateDTO:
        def fromDomain(domain: FioImportState): FioImportStateDTO =
            domain.into[FioImportStateDTO].transform
    
    // Define given DbCodecs for custom types
    given DbCodec[Instant] = DbCodec.SqlTimestampCodec.biMap(
        i => i.toInstant,
        i => java.sql.Timestamp.from(i)
    )
    
    // Repository definition
    val importStateRepo = Repo[FioImportStateDTO, FioImportStateDTO, Null]
    
    val initSql = """
        CREATE TABLE IF NOT EXISTS fio_import_state (
            source_account_id BIGINT PRIMARY KEY,
            last_transaction_id BIGINT,
            last_import_timestamp TIMESTAMP NOT NULL
        )
    """
    
    private def initTable(xa: Transactor): Task[Unit] =
        xa.transact {
            sql(initSql).execute()
        }.orDie
    
    val layer: ZLayer[PostgreSQLTransactor, Throwable, FioImportStateRepository] =
        ZLayer {
            for
                xa <- ZIO.service[PostgreSQLTransactor].map(_.transactor)
                _ <- initTable(xa)
                repo = new PostgreSQLFioImportStateRepository(xa)
            yield repo
        }
```

### 3. CLI Repository Mocks

For the CLI tool mock implementation of `SourceAccountRepository`, we need to conform to the contract:

```scala
// Mock implementation for CLI
new SourceAccountRepository {
    // Required by Repository[Long, SourceAccount, SourceAccountQuery]
    def find(filter: SourceAccountQuery): UIO[Seq[SourceAccount]] = 
        ZIO.succeed(Seq(SourceAccount(
            id = 1L,
            name = "Test Account",
            accountId = "2200000001", 
            bankId = "2010",
            currency = "CZK",
            active = true,
            ynabAccountId = None,
            lastSyncTime = None
        )))
        
    def load(id: Long): UIO[Option[SourceAccount]] = 
        ZIO.succeed(Some(SourceAccount(
            id = id,
            name = "Test Account",
            accountId = "2200000001",
            bankId = "2010", 
            currency = "CZK",
            active = true,
            ynabAccountId = None,
            lastSyncTime = None
        )))
        
    def save(id: Long, value: SourceAccount): UIO[Unit] = 
        ZIO.unit
        
    // Required by CreateRepository[Long, CreateSourceAccount]
    def create(value: CreateSourceAccount): UIO[Long] = 
        ZIO.succeed(1L)
}
```

### 4. Build File Dependencies

To properly integrate Magnum, the build.sbt file needs these dependencies:

```scala
val magnumVersion = "0.1.1"
libraryDependencies ++= Seq(
  "com.augustnagro" %% "magnum" % magnumVersion,
  "com.augustnagro" %% "magnumzio" % magnumVersion
)
```

### 5. Implementation Recommendations

1. **Working with existing interfaces**:
   - The Fio context should conform to the existing interfaces from the Transactions context
   - Don't modify `SourceAccountRepository` - adapt your code to work with it

2. **PostgreSQL Repository Implementation**:
   - Keep your temporary in-memory implementation until dependencies are resolved
   - Include comments (like you've done) showing how it would be properly implemented
   - Model your future implementation after `PostgreSQLTransactionRepository` and `PostgreSQLSourceAccountRepository`

3. **DTO Design**:
   - Follow the pattern of including DTOs as nested case classes in the repository companion object
   - Use chimney's transformation capabilities for clean, type-safe conversions
   - Match database column names using `SqlNameMapper.CamelToSnakeCase`

4. **Error Handling**:
   - Domain repositories typically deal with business logic errors
   - Infrastructure failures (like DB connection issues) are typically handled with `.orDie`
   - If specific error handling is needed, map infrastructure errors to domain errors

### Conclusion

The recommended approach for implementing the Fio repositories is to:

1. Keep the current in-memory implementation for now
2. Include clear documentation about how it will be properly implemented
3. Ensure repository interfaces conform to domain contracts
4. Use Magnum library when implementing the real PostgreSQL repository

This aligns with the project's architecture and follows the DDD principle of having the supporting bounded contexts (Fio) conform to the models established by the core bounded contexts (Transactions).