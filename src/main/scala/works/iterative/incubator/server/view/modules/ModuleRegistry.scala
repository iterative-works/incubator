package works.iterative.incubator.server
package view.modules

import zio.*
import works.iterative.tapir.BaseUri
import scala.annotation.unused
import works.iterative.incubator.components.ScalatagsAppShell
import works.iterative.server.http.ScalatagsViteSupport
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportModule
import works.iterative.server.http.tapir.TapirWebModuleAdapter
import works.iterative.incubator.budget.ui.transaction_import.TransactionImportPresenter
import sttp.tapir.server.http4s.Http4sServerOptions
import zio.interop.catz.*
import works.iterative.server.http.WebFeatureModule

class ModuleRegistry(
    baseUri: BaseUri,
    viteConfig: AssetsModule.ViteConfig,
    @unused viteSupport: ScalatagsViteSupport
):

    @unused
    private val appShell = ScalatagsAppShell(viteSupport)

    private val helloWorldModule = HelloWorldModule
    private val assetsModule = AssetsModule(viteConfig)

    private val transactionImportModule: TransactionImportModule =
        TransactionImportModule(appShell, baseUri)

    private val transactionImportWebModule =
        TapirWebModuleAdapter.adapt[TransactionImportPresenter](
            options = Http4sServerOptions.default,
            module = transactionImportModule
        )

    def modules: List[WebFeatureModule[RIO[AppEnv, *]]] = List(
        helloWorldModule.widen,
        assetsModule.widen,
        transactionImportWebModule
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
