package works.iterative.incubator.ui.preview

import zio.*
import org.http4s.*
import org.http4s.server.websocket.WebSocketBuilder2
import works.iterative.server.http.HttpServer
import works.iterative.server.http.WebFeatureModule
import works.iterative.server.http.impl.blaze.BlazeServerConfig
import java.net.InetAddress

/** Preview server for UI components Allows viewing components in isolation with different states
  */
object PreviewServer:

    def routes(registry: PreviewModuleRegistry): HttpRoutes[PreviewTask] =
        import zio.interop.catz.*
        WebFeatureModule.combineRoutes[RIO[PreviewEnv, *]](registry.modules*)
    end routes

    def setupRoutes(
        registry: PreviewModuleRegistry
    )(wsb: WebSocketBuilder2[PreviewTask]): HttpRoutes[PreviewTask] =
        routes(registry)

    def getPreviewServerConfig: ZIO[Any, Throwable, BlazeServerConfig] =
        for
            // Get host and port from environment variables, with defaults
            host <-
                ZIO.attempt(Option(java.lang.System.getenv("BLAZE_HOST")).getOrElse("localhost"))
            portStr <- ZIO.attempt(Option(java.lang.System.getenv("BLAZE_PORT")).getOrElse("8090"))
            port <- ZIO.attempt(portStr.toInt)
            // Create BlazeServerConfig
            config = BlazeServerConfig(
                host = host,
                port = port,
                responseHeaderTimeout = zio.Duration.fromSeconds(60),
                idleTimeout = zio.Duration.fromSeconds(30)
            )
            _ <- ZIO.logDebug(s"Created server config: host=$host, port=$port")
        yield config

    def serve(registry: PreviewModuleRegistry): ZIO[HttpServer & Scope, Throwable, Unit] =
        for
            config <- getPreviewServerConfig
            _ <- ZIO.logInfo(s"Starting preview server on ${config.host}:${config.port}...")
            _ <- HttpServer.serve[PreviewEnv](setupRoutes(registry))
            address = InetAddress.getByName(config.host)
            _ <- ZIO.logInfo(
                s"Preview server running at http://${address.getHostAddress}:${config.port}/preview"
            )
        yield ()
end PreviewServer
