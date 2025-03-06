package works.iterative.incubator.server.view.modules

import works.iterative.server.http.ZIOWebModule
import zio.*
import works.iterative.tapir.BaseUri
import scala.annotation.unused
import works.iterative.incubator.transactions.infrastructure.TransactionImportModule
import works.iterative.incubator.components.ScalatagsAppShell
import works.iterative.server.http.ScalatagsViteSupport

class ModuleRegistry(
    @unused baseUri: BaseUri,
    viteConfig: AssetsModule.ViteConfig,
    viteSupport: ScalatagsViteSupport
):

    private val appShell = ScalatagsAppShell(viteSupport)

    private val helloWorldModule = HelloWorldModule
    private val assetsModule = AssetsModule(viteConfig)
    private val transactionImportModule = TransactionImportModule(appShell)

    def modules: List[ZIOWebModule[Any]] = List(
        helloWorldModule.widen,
        assetsModule.widen,
        transactionImportModule.widen
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
