package works.iterative.incubator.auth.application.service

import zio.Task
import works.iterative.incubator.auth.domain.model.*

/** Service interface for user management
  *
  * This service provides methods for managing users, including creation, retrieval, and updates.
  *
  * Classification: Application Service
  */
trait UserService:
    /** Create a new user
      *
      * @param request
      *   The user creation request
      * @return
      *   The ID of the newly created user
      */
    def createUser(request: CreateUserRequest): Task[String]

    /** Get a user by ID
      *
      * @param id
      *   The user ID
      * @return
      *   The user, if found
      */
    def getUser(id: String): Task[Option[User]]

    /** Get a user by username
      *
      * @param username
      *   The username
      * @return
      *   The user, if found
      */
    def getUserByUsername(username: String): Task[Option[User]]

    /** Get a user by email
      *
      * @param email
      *   The email address
      * @return
      *   The user, if found
      */
    def getUserByEmail(email: String): Task[Option[User]]

    /** Update a user
      *
      * @param user
      *   The updated user
      * @return
      *   Unit
      */
    def updateUser(user: User): Task[Unit]

    /** Deactivate a user
      *
      * @param id
      *   The user ID
      * @return
      *   True if the user was deactivated, false if not found
      */
    def deactivateUser(id: String): Task[Boolean]

    /** Activate a user
      *
      * @param id
      *   The user ID
      * @return
      *   True if the user was activated, false if not found
      */
    def activateUser(id: String): Task[Boolean]

    /** Change a user's password
      *
      * @param id
      *   The user ID
      * @param newPassword
      *   The new password
      * @return
      *   Unit
      */
    def changePassword(id: String, newPassword: String): Task[Unit]

    /** Get all roles
      *
      * @return
      *   List of all roles
      */
    def getAllRoles(): Task[List[Role]]

    /** Get all permissions
      *
      * @return
      *   List of all permissions
      */
    def getAllPermissions(): Task[List[Permission]]
end UserService
