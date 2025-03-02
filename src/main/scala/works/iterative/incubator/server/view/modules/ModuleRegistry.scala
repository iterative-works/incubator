package works.iterative.incubator.server.view.modules

import works.iterative.server.http.ZIOWebModule
import zio.*
import works.iterative.tapir.BaseUri
import scala.annotation.unused

class ModuleRegistry(
    @unused baseUri: BaseUri,
    viteConfig: AssetsModule.ViteConfig
):

    private val helloWorldModule = HelloWorldModule
    private val assetsModule = AssetsModule(viteConfig)

    def modules: List[ZIOWebModule[Any]] = List(
        helloWorldModule.widen,
        assetsModule.widen
    )
end ModuleRegistry

object ModuleRegistry:
    val layer: ZLayer[Any, Config.Error, ModuleRegistry] =
        ZLayer {
            for
                baseUri <- ZIO.config[BaseUri](BaseUri.config)
                viteConfig <- ZIO.config(AssetsModule.ViteConfig.config)
            yield ModuleRegistry(baseUri, viteConfig)
        }
end ModuleRegistry
