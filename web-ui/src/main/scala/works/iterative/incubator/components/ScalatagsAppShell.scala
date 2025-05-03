package works.iterative.incubator.components

import scalatags.Text
import scalatags.Text.all.*
import scalatags.Text.tags2
import works.iterative.server.http.ScalatagsViteSupport

class ScalatagsAppShell(viteSupport: ScalatagsViteSupport)
    extends works.iterative.scalatags.components.ScalatagsAppShell:
    def wrap(pageTitle: String, contentFrag: Frag): Frag =
        html(
            cls := "h-full bg-white",
            lang := "en"
        )(
            head(
                meta(charset := "UTF-8"),
                meta(
                    name := "viewport",
                    content := "width=device-width, initial-scale=1.0"
                ),
                tags2.title(pageTitle),
                // Include Vite's main CSS and other assets
                viteSupport.mainCss,
                // Include the entry point specific assets
                // TODO: module-based entry points?
                viteSupport.preambleFor("main.js")
            ),
            body(cls := "h-full", contentFrag)
        )
end ScalatagsAppShell
