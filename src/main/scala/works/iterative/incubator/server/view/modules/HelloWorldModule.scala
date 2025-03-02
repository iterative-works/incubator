package works.iterative.incubator.server.view.modules

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.Response
import zio.interop.catz.*

object HelloWorldModule extends ZIOWebModule[Any]:

    override def routes: HttpRoutes[WebTask] =
        val dsl = Http4sDsl[WebTask]
        import dsl.*
        HttpRoutes.of[WebTask] {
            case GET -> Root / "hello" =>
                Ok("Hello, World!")
        }
    end routes
end HelloWorldModule
