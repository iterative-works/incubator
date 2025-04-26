package works.iterative.incubator.auth.domain.model

import java.time.Instant

/** Represents a user in the system
  *
  * This entity contains user information, authentication details, and references to assigned roles
  * and permissions.
  *
  * Classification: Domain Entity
  */
case class User(
    id: String, // Unique identifier for the user
    username: String, // Username for login
    email: String, // Email address
    firstName: Option[String], // Optional first name
    lastName: Option[String], // Optional last name
    passwordHash: String, // Hashed password (never store plaintext)
    roles: List[Role], // Assigned roles
    active: Boolean, // Whether the account is active
    lastLogin: Option[Instant], // When the user last logged in
    createdAt: Instant, // When the account was created
    updatedAt: Instant // When the account was last updated
)

/** Represents a request to create a new user
  *
  * Classification: Domain Value Object (Command)
  */
case class CreateUserRequest(
    username: String,
    email: String,
    password: String,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    roles: List[String] = List.empty
)
