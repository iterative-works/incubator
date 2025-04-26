package works.iterative.incubator.auth.application.service

import zio.Task
import works.iterative.incubator.auth.domain.model.*

/** Service interface for authentication
  *
  * This service provides methods for user authentication, token management, and permission checks.
  *
  * Classification: Application Service
  */
trait AuthenticationService:
    /** Authenticate a user with username/email and password
      *
      * @param usernameOrEmail
      *   The username or email
      * @param password
      *   The password
      * @return
      *   Authentication token if successful, None otherwise
      */
    def authenticate(usernameOrEmail: String, password: String): Task[Option[String]]

    /** Validate an authentication token
      *
      * @param token
      *   The authentication token
      * @return
      *   User ID if valid, None otherwise
      */
    def validateToken(token: String): Task[Option[String]]

    /** Get the user associated with a token
      *
      * @param token
      *   The authentication token
      * @return
      *   The user if token is valid, None otherwise
      */
    def getUserFromToken(token: String): Task[Option[User]]

    /** Invalidate a token (logout)
      *
      * @param token
      *   The authentication token
      * @return
      *   Unit
      */
    def invalidateToken(token: String): Task[Unit]

    /** Check if a user has a specific permission
      *
      * @param userId
      *   The user ID
      * @param resource
      *   The resource to check
      * @param action
      *   The action to check
      * @return
      *   True if the user has the permission, false otherwise
      */
    def hasPermission(userId: String, resource: String, action: String): Task[Boolean]

    /** Check if a user has a specific role
      *
      * @param userId
      *   The user ID
      * @param roleName
      *   The role name to check
      * @return
      *   True if the user has the role, false otherwise
      */
    def hasRole(userId: String, roleName: String): Task[Boolean]
end AuthenticationService
