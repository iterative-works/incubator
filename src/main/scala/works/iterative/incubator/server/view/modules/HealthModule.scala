package works.iterative.incubator.server
package view.modules

import zio.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import works.iterative.server.http.ZIOWebModule
import zio.json.*
import zio.json.JsonEncoder
import zio.interop.catz.*
import org.typelevel.ci.CIStringSyntax

/** Health Module provides endpoints for container health checks Used by Docker for health check and
  * TestContainers for wait strategy
  */
object HealthModule extends ZIOWebModule[AppEnv]:
    /** HealthStatus represents the health of the application and its dependencies */
    case class HealthStatus(
        status: String,
        dependencies: Map[String, HealthStatusResult]
    )

    /** Dependency health result */
    case class HealthStatusResult(
        status: String,
        message: Option[String] = None
    )

    /** Response encoding */
    given JsonEncoder[HealthStatusResult] = DeriveJsonEncoder.gen[HealthStatusResult]
    given JsonEncoder[HealthStatus] = DeriveJsonEncoder.gen[HealthStatus]

    // Health check route
    override val routes: HttpRoutes[AppTask] =
        val dsl = Http4sDsl[AppTask]
        import dsl.*

        HttpRoutes.of[AppTask] {
            // Main health check endpoint
            case GET -> Root / "health" =>
                for
                    status <- checkAllDependencies
                    response <- status.dependencies.values.exists(_.status != "UP") match
                        case true =>
                            // If any dependency is down, return 503 Service Unavailable
                            ServiceUnavailable(status.toJson)
                        case false =>
                            // All dependencies are up, return 200 OK
                            Ok(status.toJson)
                yield response.withHeaders(Header.Raw(ci"Content-Type", "application/json"))
        }
    end routes

    /** Check the health of all dependencies Uses TransactionRepository to verify database
      * connectivity Can be extended to check other dependencies as needed
      */
    private def checkAllDependencies: URIO[AppEnv, HealthStatus] =
        for
            dbStatus <- checkDatabaseHealth.catchAll(error =>
                ZIO.succeed(HealthStatusResult("DOWN", Some(error.getMessage)))
            )
            // Add other dependency checks here as needed
        yield HealthStatus(
            status = if dbStatus.status == "UP" then "UP" else "DOWN",
            dependencies = Map(
                "database" -> dbStatus
            )
        )

    /** Check database connectivity using TransactionRepository Simply tries to perform a basic
      * operation to verify connectivity
      */
    private def checkDatabaseHealth: ZIO[AppEnv, Throwable, HealthStatusResult] =
        ZIO.scoped {
            for
                // Use an existing repository to check database connectivity
                repo <- ZIO.service[
                    works.iterative.incubator.transactions.domain.repository.TransactionRepository
                ]
                // Execute a simple find query with no filters to check DB connectivity
                _ <- ZIO.attemptBlockingInterrupt {
                    // This is just to trigger the find and make sure it works
                    repo.find(
                        works.iterative.incubator.transactions.domain.query.TransactionQuery()
                    )
                }.flatMap(task => task)
            yield HealthStatusResult("UP")
        }.catchAll(err =>
            ZIO.fail(new Exception(s"Database connection error: ${err.getMessage}", err))
        )
end HealthModule
