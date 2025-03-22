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

Integration tests use TestContainers to spin up a PostgreSQL database in a Docker container. The schema is created directly with SQL commands for each test to ensure a clean environment. While Flyway is used for migrations in the production code, the integration tests set up the schema manually to avoid migration-related issues in the test context.

## Development Workflow

### Pre-commit/Pre-PR Checklist

Before committing changes or creating a pull request, always run the following commands to ensure your code is clean and working properly:

1. `sbtn clean` - Clean all compiled artifacts to ensure a fresh build
2. `sbtn compile` - Compile the code and verify there are no compiler warnings
3. `sbtn test` - Run all tests to ensure everything is working correctly

This cycle helps catch issues early and ensures that our codebase remains clean and maintainable.

## Code Style Guidelines
- **Architecture**: Follow Functional Core/Imperative Shell pattern (see principles.md)
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
- **Testing**: Write tests first, focus on domain invariants
- **Comments**: Explain "why", not "what" - code should be self-documenting
