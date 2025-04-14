package works.iterative.incubator.transactions.infrastructure.config

import zio.*
import works.iterative.incubator.transactions.domain.repository.*
import works.iterative.incubator.transactions.infrastructure.persistence.*

/** Combines transaction-specific repositories with the database infrastructure
  *
  * This module implements the repository module for the transactions bounded context by providing
  * the specific repositories needed.
  *
  * Classification: Infrastructure Configuration
  */
object PostgreSQLTransactionsDatabaseModule:
    /** Repository type that includes base infrastructure and all transaction-specific repositories
      */
    type Repositories =
        TransactionRepository & SourceAccountRepository & TransactionProcessingStateRepository

    /** Implements the repository layers for transaction-specific repositories
      */
    val repoLayers
        : ZLayer[PostgreSQLDatabaseSupport.BaseDatabaseInfrastructure, Throwable, Repositories] =
        // Create all repositories using the base infrastructure
        PostgreSQLTransactionRepository.layer ++
            PostgreSQLSourceAccountRepository.layer ++
            PostgreSQLTransactionProcessingStateRepository.layer
    end repoLayers
end PostgreSQLTransactionsDatabaseModule
