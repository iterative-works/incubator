package works.iterative.incubator.server.view.modules

import works.iterative.server.http.ZIOWebModule
import zio.*
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import org.http4s.*

object AssetsModule:
    final case class ViteConfig(
        distPath: String = "./target/vite",
        resourceDirs: List[String] = List("assets", "js", "css")
    )

    object ViteConfig:
        val config: Config[ViteConfig] =
            import Config.*
            (string("distPath") zip listOf(string("resourceDir")).withDefault(List(
                "assets",
                "js",
                "css"
            ))).nested("vite").map(ViteConfig.apply)
        end config
    end ViteConfig

    val layer: ZLayer[Any, Config.Error, AssetsModule] = ZLayer {
        for
            config <- ZIO.config(ViteConfig.config)
        yield AssetsModule(config)
    }
end AssetsModule

import AssetsModule.*

class AssetsModule(conf: ViteConfig) extends ZIOWebModule[Any]:
    private val dsl = Http4sDsl[WebTask]
    import dsl.*

    private val baseDir = fs2.io.file.Path(conf.distPath)

    private def serve(dir: String, req: Request[WebTask], path: Uri.Path) =
        StaticFile.fromPath(baseDir / dir / path.toString, Some(req))(using
            fs2.io.file.Files.forAsync[WebTask]
        ).getOrElseF(NotFound())

    override def routes: HttpRoutes[WebTask] =
        HttpRoutes.of[WebTask]:
            case req @ GET -> dir /: path if conf.resourceDirs.contains(dir) =>
                serve(dir, req, path)
    end routes
end AssetsModule
