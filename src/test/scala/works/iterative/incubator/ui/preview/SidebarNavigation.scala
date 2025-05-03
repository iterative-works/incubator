package works.iterative.incubator.ui.preview

import scalatags.Text.all.*

/**
 * Sidebar navigation component for the preview server
 * Provides links to all available component previews
 */
object SidebarNavigation:

    /**
     * Render the sidebar navigation
     * 
     * @param currentPath The current path to highlight the active link
     * @return Scalatags fragment with the navigation structure
     */
    def render(currentPath: String): Frag =
        val items = List(
            NavCategory("Home", List(
                NavItem("Home", "/preview")
            )),
            NavCategory("Transaction Import", List(
                NavItem("Date Range Selector", "/preview/transaction-import/date-range-selector")
                // Add more components as they're implemented
            ))
            // Add more categories as needed
        )
        
        div(cls := "py-2")(
            items.map { category =>
                frag(
                    div(cls := "category-title")(category.name),
                    ul(cls := "mb-4")(
                        category.items.map { item =>
                            li(
                                a(
                                    href := item.path,
                                    cls := s"sidebar-link ${if (currentPath == item.path) "active" else ""}",
                                    item.name
                                )
                            )
                        }
                    )
                )
            }
        )
    end render
    
    /**
     * Navigation category with grouped items
     */
    case class NavCategory(name: String, items: List[NavItem])
    
    /**
     * Navigation item with name and link path
     */
    case class NavItem(name: String, path: String)
end SidebarNavigation