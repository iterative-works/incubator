package works.iterative.incubator.ui.preview

import zio.*
import org.http4s.*
import org.http4s.server.websocket.WebSocketBuilder2
import works.iterative.server.http.HttpServer
import works.iterative.server.http.WebFeatureModule

/**
 * Preview server for UI components
 * Allows viewing components in isolation with different states
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
        
    def serve(registry: PreviewModuleRegistry): ZIO[HttpServer & Scope, Throwable, Unit] =
        ZIO.logInfo("Starting preview server...") *>
        HttpServer.serve[PreviewEnv](setupRoutes(registry))
end PreviewServer