# Build File Update for Bounded Contexts

## Background

After completing the package restructuring by bounded context migration, we needed to update the build.sbt file to reflect our new organization. This document outlines the changes we made and the ongoing work to complete the migration.

## Changes Made

1. **Updated build.sbt**:
   - Created module definitions for each bounded context (transactions, ynab, fio, categorization, auth)
   - Each bounded context is now a single SBT module containing its complete vertical slice
   - Maintained backward compatibility by keeping the original module structure
   - Added appropriate dependencies for each bounded context

2. **Directory Structure Approach**:
   - Each bounded context has a single root directory following the pattern:
     ```
     bounded-contexts/
       └── transactions/
           └── src/main/scala/works/iterative/incubator/transactions/
               ├── domain/          # Domain models, repositories, queries
               ├── application/     # Application services, ports
               ├── infrastructure/  # Technical implementations
               └── web/             # UI components
     ```
   - This structure aligns with DDD principles where each bounded context contains its complete vertical slice
   - Package organization reflects architectural layers within each bounded context

## Current Status

We've updated the build.sbt file to reflect the correct bounded context structure. The project now builds successfully with the new module organization. We need to begin migrating files to the correct locations following our layered architecture.

## Next Steps

1. **Migration of Transaction Context**:
   - Move domain models, repository interfaces, and query objects
   - Move application services and ports
   - Move infrastructure implementations
   - Move web components
   - Update import statements to reflect the new package structure

2. **Complete Other Bounded Contexts**:
   - Apply the same migration pattern to YNAB, Fio, Categorization, and Auth contexts
   - Ensure proper dependencies between bounded contexts
   - Update all import statements to reflect new locations

3. **Incremental Testing**:
   - After each bounded context is migrated, run tests to ensure functionality is preserved
   - Verify interoperability between bounded contexts

4. **Update Documentation**:
   - Document the new build structure
   - Update developer guidelines to reflect the new organization

## Bounded Context Dependencies

The dependencies between our bounded contexts are:

1. **Core** - Shared utilities and base traits used across all contexts
2. **Transactions** - The central bounded context for transaction management
3. **YNAB** - Depends on Transactions for integration with YNAB
4. **Fio** - Depends on Transactions for importing bank data
5. **Categorization** - Depends on Transactions for AI categorization
6. **Auth** - Independent context for user authentication and authorization

## Module Structure and Dependencies

Our new build.sbt structure:

```scala
// Core module (shared across all contexts)
lazy val core = (project in file("bounded-contexts/core"))
  .settings(name := "core")
  .enablePlugins(IWScalaProjectPlugin)
  .settings(commonDependencies)

// Transaction Management Context
lazy val transactions = (project in file("bounded-contexts/transactions"))
  .settings(name := "transactions")
  .enablePlugins(IWScalaProjectPlugin)
  .settings(
    commonDependencies,
    // All dependencies required for this context
    IWDeps.zioJson,
    IWDeps.magnumZIO,
    IWDeps.magnumPG,
    IWDeps.http4sBlazeServer,
    IWDeps.scalatags,
    // Database dependencies
    ...
  )
  .dependsOn(core, webUi)

// Similar structure for other bounded contexts
lazy val ynab = ...
lazy val fio = ...
lazy val categorization = ...
lazy val auth = ...
```

This structure:
- Aligns SBT modules with our bounded contexts
- Places all layers (domain, application, infrastructure, web) within a single module
- Maintains clear dependencies between contexts
- Allows for independent evolution of each context

## Challenges and Solutions

The main challenge we're facing is maintaining backward compatibility while restructuring the codebase. Our approach is:

1. **Parallel Structure**: Maintain both old and new structures until migration is complete
2. **Export Directives**: Use Scala 3's export directives to maintain backward compatibility
3. **Incremental Migration**: Migrate one bounded context at a time
4. **Comprehensive Testing**: Ensure all functionality is preserved through thorough testing

## Conclusion

We've corrected our build structure to align with DDD principles, where each bounded context is a single module containing its complete vertical slice. The next step is to migrate files to their new locations following this structure, maintaining backward compatibility during the transition.