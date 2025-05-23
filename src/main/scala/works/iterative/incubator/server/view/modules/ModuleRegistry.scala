package works.iterative.incubator.server
package view.modules

import works.iterative.server.http.ZIOWebModule
import zio.*
import works.iterative.tapir.BaseUri
import scala.annotation.unused
import works.iterative.incubator.components.ScalatagsAppShell
import works.iterative.server.http.ScalatagsViteSupport

class ModuleRegistry(
    @unused baseUri: BaseUri,
    viteConfig: AssetsModule.ViteConfig,
    @unused viteSupport: ScalatagsViteSupport
):

    @unused
    private val appShell = ScalatagsAppShell(viteSupport)

    private val helloWorldModule = HelloWorldModule
    private val assetsModule = AssetsModule(viteConfig)

    def modules: List[ZIOWebModule[AppEnv]] = List(
        helloWorldModule.widen,
        assetsModule.widen
    )
end ModuleRegistry

object ModuleRegistry:
    val layer: ZLayer[ScalatagsViteSupport, Config.Error, ModuleRegistry] =
        ZLayer {
            for
                baseUri <- ZIO.config[BaseUri](BaseUri.config)
                viteConfig <- ZIO.config(AssetsModule.ViteConfig.config)
                viteSupport <- ZIO.service[ScalatagsViteSupport]
            yield ModuleRegistry(baseUri, viteConfig, viteSupport)
        }
end ModuleRegistry
