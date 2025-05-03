package works.iterative.incubator.ui.preview

import zio.*
import works.iterative.tapir.BaseUri
import works.iterative.server.http.WebFeatureModule
import works.iterative.server.http.ScalatagsViteSupport
import works.iterative.server.http.tapir.TapirWebModuleAdapter
import sttp.tapir.server.http4s.Http4sServerOptions
import zio.interop.catz.*

/** Registry of all preview modules Similar to ModuleRegistry in the main application but for
  * preview modules
  */
class PreviewModuleRegistry(
    baseUri: BaseUri,
    viteSupport: ScalatagsViteSupport
):
    // Create the preview app shell
    private val appShell = PreviewAppShell(viteSupport)

    // Create module for home/index
    private val homeModule = HomePreviewModule(appShell, baseUri)

    // Create navigation module
    private val navigationModule = NavigationPreviewModule(appShell, baseUri)

    // Create component preview modules
    private val dateRangeSelectorPreviewModule = DateRangeSelectorPreviewModule(appShell, baseUri)
    private val importButtonPreviewModule = ImportButtonPreviewModule(appShell, baseUri)
    private val resultsPanelPreviewModule = ResultsPanelPreviewModule(appShell, baseUri)

    // Create web module adapters
    private val homeWebModule =
        TapirWebModuleAdapter.adapt[PreviewEnv](
            options = Http4sServerOptions.default,
            module = homeModule
        )

    private val navigationWebModule =
        TapirWebModuleAdapter.adapt[PreviewEnv](
            options = Http4sServerOptions.default,
            module = navigationModule
        )

    private val dateRangeSelectorWebModule =
        TapirWebModuleAdapter.adapt[PreviewEnv](
            options = Http4sServerOptions.default,
            module = dateRangeSelectorPreviewModule
        )
        
    private val importButtonWebModule =
        TapirWebModuleAdapter.adapt[PreviewEnv](
            options = Http4sServerOptions.default,
            module = importButtonPreviewModule
        )
        
    private val resultsPanelWebModule =
        TapirWebModuleAdapter.adapt[PreviewEnv](
            options = Http4sServerOptions.default,
            module = resultsPanelPreviewModule
        )

    // List of all modules to be registered
    val modules: List[WebFeatureModule[RIO[PreviewEnv, *]]] = List(
        homeWebModule,
        navigationWebModule,
        dateRangeSelectorWebModule,
        importButtonWebModule,
        resultsPanelWebModule
    )
end PreviewModuleRegistry

object PreviewModuleRegistry:
    val layer: ZLayer[ScalatagsViteSupport, Config.Error, PreviewModuleRegistry] =
        ZLayer {
            for
                // Default base URI for preview server
                baseUri <- ZIO.succeed(BaseUri("http://localhost:8080"))
                viteSupport <- ZIO.service[ScalatagsViteSupport]
            yield PreviewModuleRegistry(baseUri, viteSupport)
        }
end PreviewModuleRegistry
