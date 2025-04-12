package works.iterative.incubator.fio.application.service

import zio.*
import java.time.LocalDate
import works.iterative.incubator.fio.domain.model.*
import works.iterative.incubator.fio.infrastructure.client.FioClient
import works.iterative.incubator.fio.infrastructure.security.FioTokenManager

/** Service for managing Fio Bank accounts
  *
  * This service provides operations for creating and managing Fio Bank accounts,
  * including setting initial bookmarks to avoid issues with historical data retrieval.
  *
  * Classification: Application Service
  */
trait FioAccountService:
    /** Create a new Fio Bank account with initial bookmark
      *
      * This method creates a new Fio account and automatically sets a bookmark (zarážka)
      * to avoid issues with retrieving historical data older than 90 days.
      *
      * @param sourceAccountId The source account ID to associate with this Fio account
      * @param token The Fio API token
      * @return The created account ID
      */
    def createAccount(sourceAccountId: Long, token: String): Task[Long]

    /** Get a Fio Bank account by ID
      *
      * @param id The account ID
      * @return The account if found
      */
    def getAccount(id: Long): Task[Option[FioAccount]]
    
    /** Get all Fio Bank accounts
      * 
      * @return List of all Fio accounts
      */
    def getAccounts(): Task[List[FioAccount]]

    /** Delete a Fio Bank account
      *
      * @param id The account ID
      * @return Unit on success
      */
    def deleteAccount(id: Long): Task[Unit]

    /** Set the bookmark (zarážka) for a Fio account
      *
      * @param accountId The account ID
      * @param date The date to set as bookmark
      * @return Unit on success
      */
    def setBookmark(accountId: Long, date: LocalDate): Task[Unit]
end FioAccountService

/** Live implementation of FioAccountService
  *
  * Classification: Application Service Implementation
  */
class FioAccountServiceLive(
    repository: FioAccountRepository,
    tokenManager: FioTokenManager,
    fioClient: FioClient,
    securityConfig: works.iterative.incubator.fio.infrastructure.security.FioSecurityConfig
) extends FioAccountService:
    /** Create a new Fio Bank account with initial bookmark
      */
    override def createAccount(sourceAccountId: Long, token: String): Task[Long] =
        for
            // Create the account in the repository
            // Use the encrypt function from FioTokenManagerLive object for token encryption
            encryptedToken <- ZIO.attempt(works.iterative.incubator.fio.infrastructure.security.FioTokenManagerLive.encrypt(token, securityConfig.encryptionKey))
                .catchAll(error => ZIO.fail(new RuntimeException(s"Failed to encrypt token: ${error.getMessage}")))
            accountId <- repository.create(CreateFioAccount(sourceAccountId, encryptedToken))
            
            // Set bookmark to 60 days ago to avoid issues with historical data
            _ <- setBookmark(accountId, LocalDate.now.minusDays(60))
                .catchAll(error => 
                    ZIO.logWarning(s"Failed to set initial bookmark for account $accountId: ${error.getMessage}") *>
                    ZIO.unit
                )
        yield accountId
    end createAccount

    /** Get a Fio Bank account by ID
      */
    override def getAccount(id: Long): Task[Option[FioAccount]] =
        repository.getById(id)
        
    /** Get all Fio Bank accounts
      */
    override def getAccounts(): Task[List[FioAccount]] =
        repository.getAll()

    /** Delete a Fio Bank account
      */
    override def deleteAccount(id: Long): Task[Unit] =
        repository.delete(id)

    /** Set the bookmark (zarážka) for a Fio account
      */
    override def setBookmark(accountId: Long, date: LocalDate): Task[Unit] =
        for
            // Get the token for this account
            tokenOpt <- tokenManager.getToken(accountId)
            _ <- ZIO.foreach(tokenOpt) { token =>
                // Set the bookmark using the Fio client
                fioClient.setLastDate(token, date)
            }.orElseFail(new RuntimeException(s"Could not retrieve token for account $accountId"))
        yield ()
    end setBookmark
end FioAccountServiceLive

object FioAccountService:
    /** ZIO access pattern for the service
      */
    def createAccount(sourceAccountId: Long, token: String): ZIO[FioAccountService, Throwable, Long] =
        ZIO.serviceWithZIO[FioAccountService](_.createAccount(sourceAccountId, token))

    def getAccount(id: Long): ZIO[FioAccountService, Throwable, Option[FioAccount]] =
        ZIO.serviceWithZIO[FioAccountService](_.getAccount(id))
        
    def getAccounts(): ZIO[FioAccountService, Throwable, List[FioAccount]] =
        ZIO.serviceWithZIO[FioAccountService](_.getAccounts())

    def deleteAccount(id: Long): ZIO[FioAccountService, Throwable, Unit] =
        ZIO.serviceWithZIO[FioAccountService](_.deleteAccount(id))

    def setBookmark(accountId: Long, date: LocalDate): ZIO[FioAccountService, Throwable, Unit] =
        ZIO.serviceWithZIO[FioAccountService](_.setBookmark(accountId, date))

    /** Layer for the service
      */
    val layer: ZLayer[FioAccountRepository & FioTokenManager & FioClient & works.iterative.incubator.fio.infrastructure.security.FioSecurityConfig, Nothing, FioAccountService] =
        ZLayer {
            for
                repo <- ZIO.service[FioAccountRepository]
                tokenManager <- ZIO.service[FioTokenManager]
                client <- ZIO.service[FioClient]
                securityConfig <- ZIO.service[works.iterative.incubator.fio.infrastructure.security.FioSecurityConfig]
            yield FioAccountServiceLive(repo, tokenManager, client, securityConfig)
        }
end FioAccountService