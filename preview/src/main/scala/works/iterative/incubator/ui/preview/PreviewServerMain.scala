package works.iterative.incubator.ui.preview

import zio.*
import zio.logging.*
import com.typesafe.config.ConfigFactory
import zio.config.typesafe.TypesafeConfigProvider
import works.iterative.server.http.impl.blaze.BlazeHttpServer
import works.iterative.server.http.ScalatagsViteSupport

/** Entry point for the Component Preview Server A simplified test server that displays UI
  * components in isolation
  */
object PreviewServerMain extends ZIOAppDefault:

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

    val program =
        for
            _ <- ZIO.logInfo("Starting Component Preview Server...")
            registry <- ZIO.service[PreviewModuleRegistry]
            _ <- PreviewServer.serve(registry)
        yield ()

    def run =
        program.provideSome[Scope](
            BlazeHttpServer.layer,
            PreviewModuleRegistry.layer,
            ScalatagsViteSupport.layer
        )
    end run
end PreviewServerMain
