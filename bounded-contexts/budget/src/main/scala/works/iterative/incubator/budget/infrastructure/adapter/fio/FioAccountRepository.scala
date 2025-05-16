package works.iterative.incubator.budget.infrastructure.adapter.fio

import works.iterative.incubator.budget.domain.model.AccountId
import zio.*

/** Repository interface for FioAccount entities.
  *
  * Category: Repository Interface Layer: Infrastructure
  */
trait FioAccountRepository:
    /** Saves a FioAccount to the repository.
      *
      * @param account
      *   The FioAccount to save
      * @return
      *   A ZIO effect that returns Unit or an error string
      */
    def save(account: FioAccount): ZIO[Any, String, Unit]

    /** Finds a FioAccount by its ID.
      *
      * @param id
      *   The FioAccount ID to look for
      * @return
      *   A ZIO effect that returns an Option containing the FioAccount if found, or an error string
      */
    def findById(id: Long): ZIO[Any, String, Option[FioAccount]]

    /** Finds a FioAccount by source account ID.
      *
      * @param sourceAccountId
      *   The source account ID to look for
      * @return
      *   A ZIO effect that returns an Option containing the FioAccount if found, or an error string
      */
    def findBySourceAccountId(
        sourceAccountId: AccountId
    ): ZIO[Any, String, Option[FioAccount]]

    /** Generates a new ID for a FioAccount.
      *
      * @return
      *   A ZIO effect that returns a new ID
      */
    def nextId(): ZIO[Any, String, Long]

    /** Returns all FioAccounts in the repository.
      *
      * @return
      *   A ZIO effect that returns a list of all FioAccounts or an error string
      */
    def getAll(): ZIO[Any, String, List[FioAccount]]
end FioAccountRepository

/** Companion object for FioAccountRepository.
  */
object FioAccountRepository:
    /** Accesses the repository to save a FioAccount.
      *
      * @param account
      *   The FioAccount to save
      * @return
      *   A ZIO effect that requires FioAccountRepository and returns Unit or an error string
      */
    def save(account: FioAccount): ZIO[FioAccountRepository, String, Unit] =
        ZIO.serviceWithZIO(_.save(account))

    /** Accesses the repository to find a FioAccount by ID.
      *
      * @param id
      *   The FioAccount ID to look for
      * @return
      *   A ZIO effect that requires FioAccountRepository and returns an Option containing the
      *   FioAccount if found, or an error string
      */
    def findById(id: Long): ZIO[FioAccountRepository, String, Option[FioAccount]] =
        ZIO.serviceWithZIO(_.findById(id))

    /** Accesses the repository to find a FioAccount by source account ID.
      *
      * @param sourceAccountId
      *   The source account ID to look for
      * @return
      *   A ZIO effect that requires FioAccountRepository and returns an Option containing the
      *   FioAccount if found, or an error string
      */
    def findBySourceAccountId(
        sourceAccountId: AccountId
    ): ZIO[FioAccountRepository, String, Option[FioAccount]] =
        ZIO.serviceWithZIO(_.findBySourceAccountId(sourceAccountId))

    /** Accesses the repository to generate a new ID.
      *
      * @return
      *   A ZIO effect that requires FioAccountRepository and returns a new ID
      */
    def nextId(): ZIO[FioAccountRepository, String, Long] =
        ZIO.serviceWithZIO(_.nextId())

    /** Accesses the repository to retrieve all FioAccounts.
      *
      * @return
      *   A ZIO effect that requires FioAccountRepository and returns a list of FioAccounts
      */
    def getAll(): ZIO[FioAccountRepository, String, List[FioAccount]] =
        ZIO.serviceWithZIO(_.getAll())

    /** Creates a new FioAccount with an automatically generated ID.
      *
      * @param sourceAccountId
      *   The source account ID
      * @param encryptedToken
      *   The encrypted Fio API token
      * @return
      *   A ZIO effect that requires FioAccountRepository and returns the created FioAccount or an
      *   error string
      */
    def createAccount(
        sourceAccountId: AccountId,
        encryptedToken: String
    ): ZIO[FioAccountRepository, String, FioAccount] =
        for
            id <- nextId()
            accountEither = FioAccount.create(id, sourceAccountId, encryptedToken)
            account <- ZIO.fromEither(accountEither)
            _ <- save(account)
        yield account
end FioAccountRepository
