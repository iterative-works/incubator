package works.iterative.incubator.ynab.domain.repository

import works.iterative.incubator.ynab.domain.model.{CreateYnabAccountMapping, YnabAccountMapping}
import zio.{IO, Task, UIO}

/**
 * Repository interface for YnabAccountMapping entities
 *
 * This repository is responsible for managing the mappings between source accounts
 * in the transactions context and YNAB accounts.
 *
 * Classification: Domain Repository Interface
 */
trait YnabAccountMappingRepository {
  /**
   * Find mapping by source account ID
   *
   * @param sourceAccountId ID of the source account
   * @return The mapping if found
   */
  def findBySourceAccountId(sourceAccountId: Long): IO[Throwable, Option[YnabAccountMapping]]

  /**
   * Find mapping by YNAB account ID
   *
   * @param ynabAccountId ID of the YNAB account
   * @return The mapping if found
   */
  def findByYnabAccountId(ynabAccountId: String): IO[Throwable, Option[YnabAccountMapping]]
  
  /**
   * Find all mappings
   *
   * @return List of all account mappings
   */
  def findAll(): IO[Throwable, List[YnabAccountMapping]]
  
  /**
   * Find all active mappings
   *
   * @return List of all active account mappings
   */
  def findAllActive(): IO[Throwable, List[YnabAccountMapping]]
  
  /**
   * Save a new mapping
   *
   * @param mapping The mapping to save
   * @return The saved mapping
   */
  def save(mapping: CreateYnabAccountMapping): IO[Throwable, YnabAccountMapping]
  
  /**
   * Update an existing mapping
   *
   * @param mapping The mapping to update
   * @return The updated mapping
   */
  def update(mapping: YnabAccountMapping): IO[Throwable, YnabAccountMapping]
  
  /**
   * Delete a mapping
   *
   * @param sourceAccountId ID of the source account
   * @return Unit if successful
   */
  def delete(sourceAccountId: Long): IO[Throwable, Unit]
}