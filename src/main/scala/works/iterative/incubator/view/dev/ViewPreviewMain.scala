package works.iterative.incubator.view.dev

import zio.*
import org.http4s.*
import org.http4s.server.websocket.WebSocketBuilder2
import works.iterative.server.http.HttpServer
import works.iterative.server.http.impl.blaze.BlazeHttpServer
import zio.logging.*
import com.typesafe.config.ConfigFactory
import zio.config.typesafe.TypesafeConfigProvider
import works.iterative.server.http.ZIOWebModule
import works.iterative.server.http.ScalatagsViteSupport
import works.iterative.incubator.server.view.modules.AssetsModule
import works.iterative.incubator.components.ScalatagsAppShell

/** Standalone server for previewing UI views with test data. Provides a lightweight environment
  * with only the necessary dependencies for UI development without requiring the full application
  * stack.
  */
object ViewPreviewMain extends ZIOAppDefault:

    // Define a simple environment type for our preview server
    type PreviewEnv = ScalatagsViteSupport & AssetsModule

    // Type alias for our preview tasks
    type PreviewTask[A] = RIO[PreviewEnv, A]

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

    def routes(registry: ViewPreviewModuleRegistry): HttpRoutes[PreviewTask] =
        ZIOWebModule.combineRoutes[PreviewEnv](registry.modules*)
    end routes

    def setupRoutes(
        registry: ViewPreviewModuleRegistry
    )(wsb: WebSocketBuilder2[PreviewTask]): HttpRoutes[PreviewTask] =
        routes(registry)

    val program =
        for
            registry <- ZIO.service[ViewPreviewModuleRegistry]
            _ <- Console.printLine("""
              |================================================
              | View Preview Server
              | Open http://localhost:8080/preview in browser
              |================================================
            """.stripMargin)
            _ <- HttpServer.serve[PreviewEnv](
                setupRoutes(registry)
            )
        yield ()

    def run =
        program.provideSome[Scope](
            BlazeHttpServer.layer,
            ViewPreviewModuleRegistry.layer,
            ScalatagsViteSupport.layer,
            AssetsModule.layer
        )
    end run
end ViewPreviewMain

/** Registry for view preview modules. Similar to the main ModuleRegistry but focused only on
  * preview modules.
  */
class ViewPreviewModuleRegistry(
    viteConfig: AssetsModule.ViteConfig,
    viteSupport: ScalatagsViteSupport
):
    private val appShell = ScalatagsAppShell(viteSupport)

    // Assets module for serving JS/CSS
    private val assetsModule = AssetsModule(viteConfig)

    // Preview module with example views
    private val viewPreviewModule = ViewPreviewModule(appShell)

    def modules: List[ZIOWebModule[ViewPreviewMain.PreviewEnv]] = List(
        assetsModule.widen,
        viewPreviewModule.widen
    )
end ViewPreviewModuleRegistry

object ViewPreviewModuleRegistry:
    val layer: ZLayer[ScalatagsViteSupport, Config.Error, ViewPreviewModuleRegistry] =
        ZLayer {
            for
                viteConfig <- ZIO.config(AssetsModule.ViteConfig.config)
                viteSupport <- ZIO.service[ScalatagsViteSupport]
            yield ViewPreviewModuleRegistry(viteConfig, viteSupport)
        }
end ViewPreviewModuleRegistry
