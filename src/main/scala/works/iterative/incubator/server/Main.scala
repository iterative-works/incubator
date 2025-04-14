package works.iterative.incubator.server

import zio.*
import org.http4s.*
import org.http4s.server.websocket.WebSocketBuilder2
import works.iterative.server.http.HttpServer
import works.iterative.server.http.impl.blaze.BlazeHttpServer
import zio.logging.*
import com.typesafe.config.ConfigFactory
import zio.config.typesafe.TypesafeConfigProvider
import works.iterative.server.http.ZIOWebModule
import view.modules.*
import works.iterative.server.http.ScalatagsViteSupport
import works.iterative.incubator.transactions.infrastructure.config.PostgreSQLTransactionsDatabaseModule
import works.iterative.incubator.transactions.infrastructure.config.PostgreSQLDatabaseSupport
import works.iterative.incubator.fio.infrastructure.service.FioTransactionImportService
import works.iterative.incubator.fio.infrastructure.client.FioClient
import works.iterative.incubator.transactions.infrastructure.service.DefaultTransactionManagerService
import works.iterative.incubator.transactions.infrastructure.service.DefaultTransactionProcessor
import works.iterative.incubator.fio.infrastructure.security.FioTokenManagerLive
import works.iterative.incubator.fio.infrastructure.persistence.PostgreSQLFioAccountRepository

object Main extends ZIOAppDefault:

    def configuredLogger(
        loadConfig: => ZIO[Any, Config.Error, ConsoleLoggerConfig]
    ): ZLayer[Any, Config.Error, Unit] =
        ReconfigurableLogger
            .make[Any, Config.Error, String, Any, ConsoleLoggerConfig](
                loadConfig,
                (config, _) => zio.logging.makeConsoleLogger(config),
                Schedule.fixed(500.millis)
            )
            .installUnscoped

    override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
        Runtime.removeDefaultLoggers >>> configuredLogger(
            for
                config <- ZIO.succeed(ConfigFactory.load("logger.conf"))
                loggerConfig <- ConsoleLoggerConfig.load().withConfigProvider(
                    TypesafeConfigProvider.fromTypesafeConfig(config)
                )
            yield loggerConfig
        )

    def routes(registry: ModuleRegistry): HttpRoutes[AppTask] =
        ZIOWebModule.combineRoutes[AppEnv](registry.modules*)
    end routes

    def setupRoutes(
        registry: ModuleRegistry
        // authMiddleware: AuthMiddleware,
        // errorHandling: ErrorHandlingMiddleware
    )(wsb: WebSocketBuilder2[AppTask]): HttpRoutes[AppTask] =
        // authMiddleware(errorHandling(routes(registry)))
        routes(registry)

    val program =
        for
            registry <- ZIO.service[ModuleRegistry]
            // errorHandling <- ZIO.service[ErrorHandlingMiddleware]
            // authMiddleware <- ZIO.service[AuthMiddleware]
            _ <- HttpServer.serve[AppEnv](
                setupRoutes(registry /* , authMiddleware, errorHandling*/ )
            )
        yield ()

    def run =
        program.provideSome[Scope](
            BlazeHttpServer.layer,
            ModuleRegistry.layer,
            ScalatagsViteSupport.layer,
            // The default migration location is automatically included,
            PostgreSQLDatabaseSupport.layerWithMigrations(),
            PostgreSQLTransactionsDatabaseModule.repoLayers,
            // Transaction processor service
            DefaultTransactionProcessor.layer,
            // Transaction manager service
            DefaultTransactionManagerService.layer,
            // Fio import service
            FioTransactionImportService.minimalLayer,
            FioClient.live,
            FioTokenManagerLive.fullLayer,
            PostgreSQLFioAccountRepository.layer
        )
    end run
end Main
