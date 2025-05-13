package works.iterative.incubator.budget.infrastructure.persistence

import works.iterative.incubator.budget.domain.repository.*
import works.iterative.incubator.budget.infrastructure.adapter.fio.FioAccountRepository
import works.iterative.sqldb.*
import zio.*

/**
 * Repository module that provides ZLayers for all PostgreSQL-based repository implementations.
 * 
 * This module serves as the central access point for repository layers in the application,
 * making it easier to provide all repositories at once.
 */
object RepositoryModule:

  /**
   * Combined layer providing both TransactionRepository and ImportBatchRepository.
   * This layer requires PostgreSQLTransactor as a dependency.
   */
  val repositories: ZLayer[PostgreSQLTransactor, Nothing, TransactionRepository & ImportBatchRepository] = 
    PostgreSQLTransactionRepository.layer ++ PostgreSQLImportBatchRepository.layer
    
  /**
   * Combined layer providing TransactionRepository, ImportBatchRepository, and FioAccountRepository.
   * This layer requires PostgreSQLTransactor as a dependency.
   */
  val allRepositories: ZLayer[PostgreSQLTransactor, Nothing, TransactionRepository & ImportBatchRepository & FioAccountRepository] = 
    PostgreSQLTransactionRepository.layer ++ PostgreSQLImportBatchRepository.layer ++ PostgreSQLFioAccountRepository.layer