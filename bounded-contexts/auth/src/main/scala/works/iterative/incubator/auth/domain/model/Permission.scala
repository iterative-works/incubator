package works.iterative.incubator.auth.domain.model

/** Represents a permission in the system
  *
  * Permissions define specific actions that users can perform in the system. They are assigned to
  * roles, which are in turn assigned to users.
  *
  * Classification: Domain Entity
  */
case class Permission(
    id: String, // Unique identifier for the permission
    name: String, // Human-readable name
    description: Option[String], // Optional description
    resource: String, // The resource this permission applies to (e.g., "transactions")
    action: String, // The action this permission allows (e.g., "read", "write", "delete")
    system: Boolean // Whether this is a system permission (cannot be deleted)
)
