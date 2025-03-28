# YNAB Importer Project Guidelines

## Build & Test Commands

We usually use metals for development. It is useful to run sbt server separately in a console, and than access the server using `sbtn`.

- **Compile**: `sbtn compile`
- **Run application**: `sbtn reStart`
- **Run tests**: `sbtn test`
- **Run specific test**: `sbtn "ynabImporterInfrastructureIT/testOnly works.iterative.incubator.transactions.infrastructure.PostgreSQLTransactionRepositorySpec"`
- **Run with specific test**: `sbtn "ynabImporterInfrastructureIT/testOnly *PostgreSQLTransactionRepositorySpec -- -t 'should save and retrieve a transaction'"`
- **Format code**: `sbtn scalafmtAll`

### Integration Testing

Integration tests use TestContainers to spin up a PostgreSQL database in a Docker container. The database schema is managed using Flyway migrations, ensuring consistency between tests and production environments. Before each test, Flyway cleans the database and applies all migrations to create a fresh schema.

## Development Workflow

### Pre-commit/Pre-PR Checklist

We are using Github, repository iterative-works/incubator. Each set of changes should be tested locally before pushing to the remote repository and creating a pull request. A standard review process is followed to ensure code quality and consistency.

Before committing changes or creating a pull request, always run the following commands to ensure your code is clean and working properly:

1. `sbtn scalafmtAll` - Format code according to our style guide
2. `sbtn clean` - Clean all compiled artifacts to ensure a fresh build
3. `sbtn compile` - Compile the code and verify there are no compiler warnings
4. `sbtn test` - Run all tests to ensure everything is working correctly
5. `sbtn ynabImporterInfrastructureIT/test` - Run integration tests separately

This cycle helps catch issues early and ensures that our codebase remains clean and maintainable.

## Environment Composition

The application uses ZIO's environment for dependency injection. When adding new modules or services:

1. **Define service interfaces** in the core module
2. **Implement services** in the infrastructure module
3. **Update AppEnv type** in `src/main/scala/works/iterative/incubator/server/AppEnv.scala` to include any new services
4. **Add service layers** to `Main.scala` in the `run` method
5. **Never use asInstanceOf** for environment compatibility - instead, properly extend the AppEnv type

## Code Style Guidelines
- **Architecture**: Follow Functional Core/Imperative Shell pattern (see principles.md and ynab-importer/doc/architecture.md)
- **Formatting**:
  - 4-space indentation, 100 column limit
  - Use new Scala 3 syntax without braces
  - End markers for methods with 5+ lines
- **Imports**: Group imports by package, sort alphabetically
- **Naming**:
  - Domain concepts in CamelCase reflecting ubiquitous language
  - Methods express intent (e.g., `findBySourceAccount` vs `getByAccount`)
- **Types**: Full type annotations for public APIs, immutable by default
- **Error Handling**: Use ZIO effects for error tracking, never throw exceptions
- **Performance Optimizations**:
  - Use caching for repeated database lookups (prefer ZIO Ref for thread-safety)
  - Clear caches at appropriate boundaries to avoid stale data
  - Document caching behavior in comments and test thoroughly
- **Testing**: Write tests first, focus on domain invariants
- **Comments**: Explain "why", not "what" - code should be self-documenting
