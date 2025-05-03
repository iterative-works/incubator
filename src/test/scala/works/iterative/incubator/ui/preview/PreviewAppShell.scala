package works.iterative.incubator.ui.preview

import scalatags.Text.all.*
import works.iterative.server.http.ScalatagsViteSupport
import scala.annotation.unused

/**
 * App shell for the preview server
 * Provides a consistent layout with navigation sidebar for all preview pages
 */
class PreviewAppShell(@unused viteSupport: ScalatagsViteSupport):
    
    /**
     * Wraps content in the preview app shell with navigation sidebar
     * 
     * @param pageTitle The title of the page
     * @param content The main content to wrap
     * @param currentPath The current path for highlighting navigation
     * @return A complete HTML page with proper layout
     */
    def wrap(pageTitle: String, content: Frag, currentPath: String = ""): Frag =
        html(
            head(
                meta(charset := "UTF-8"),
                meta(name := "viewport", attr("content") := "width=device-width, initial-scale=1.0"),
                tag("title")(s"Component Preview: $pageTitle"),
                link(rel := "stylesheet", href := "https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css"),
                script(src := "https://unpkg.com/htmx.org@1.9.4"),
                tag("style")("""
                    .sidebar { width: 280px; }
                    .main-content { margin-left: 280px; }
                    .preview-container { border: 1px solid #e2e8f0; padding: 1rem; border-radius: 0.5rem; }
                    .component-state { margin-bottom: 2rem; }
                    .state-title { font-weight: 600; margin-bottom: 0.5rem; color: #4a5568; }
                    .sidebar-link { display: block; padding: 0.5rem 1rem; border-radius: 0.25rem; }
                    .sidebar-link.active { background-color: #e2e8f0; }
                    .sidebar-link:hover { background-color: #edf2f7; }
                    .category-title { font-size: 1.125rem; font-weight: 600; margin: 1rem 0 0.5rem 0.75rem; color: #2d3748; }
                """)
            ),
            body(
                cls := "bg-gray-100 min-h-screen",
                // Sidebar navigation (fixed position)
                tag("aside")(
                    cls := "sidebar fixed top-0 left-0 h-full bg-white border-r border-gray-200 overflow-y-auto z-10",
                    div(
                        cls := "p-4 border-b border-gray-200",
                        h1(cls := "text-xl font-bold text-blue-600", "Component Preview")
                    ),
                    SidebarNavigation.render(currentPath)
                ),
                // Main content area
                tag("main")(
                    cls := "main-content min-h-screen p-8",
                    // Page header
                    div(
                        cls := "mb-6",
                        h2(cls := "text-2xl font-bold text-gray-800", pageTitle)
                    ),
                    // Main content container
                    div(
                        cls := "bg-white rounded-lg shadow-md p-6",
                        content
                    )
                )
            )
        )
    end wrap
end PreviewAppShell