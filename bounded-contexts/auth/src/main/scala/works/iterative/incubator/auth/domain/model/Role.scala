package works.iterative.incubator.auth.domain.model

/** Represents a role in the system
  *
  * A role is a collection of permissions that can be assigned to users.
  * It provides a way to group permissions for easier management.
  *
  * Classification: Domain Entity
  */
case class Role(
    id: String,                      // Unique identifier for the role
    name: String,                    // Human-readable name
    description: Option[String],     // Optional description
    permissions: List[Permission],   // Permissions granted by this role
    system: Boolean                  // Whether this is a system role (cannot be deleted)
)