package works.iterative.incubator.components

import works.iterative.ui.components.UIStylesModule

/** UIStylesModule implementation that provides Tailwind CSS classes
  */
trait TailwindUIStylesModule extends UIStylesModule[Nothing]:
    // Default styles for components (empty map since we're using classes instead of inline styles)
    override def getStyle(
        componentType: String,
        variant: String = "default",
        state: String = "default"
    ): Map[String, String] = Map.empty

    // Tailwind classes for components
    override def getClasses(
        componentType: String,
        variant: String = "default",
        state: String = "default"
    ): Seq[String] =
        (componentType, variant, state) match
            // Table classes
            case ("table", _, _)            => Seq("min-w-full", "divide-y", "divide-gray-200")
            case ("table.header", _, _)     => Seq("bg-gray-50")
            case ("table.header.row", _, _) => Seq()
            case ("table.header.cell", _, _) => Seq(
                    "px-4",
                    "py-3",
                    "text-left",
                    "text-xs",
                    "font-medium",
                    "text-gray-500",
                    "uppercase",
                    "tracking-wider"
                )
            case ("table.body", _, _)          => Seq("bg-white", "divide-y", "divide-gray-200")
            case ("table.row", _, "alternate") => Seq("bg-gray-50", "hover:bg-gray-100")
            case ("table.row", _, _)           => Seq("hover:bg-gray-50")
            case ("table.cell", _, _) =>
                Seq("px-4", "py-3", "whitespace-nowrap", "text-sm", "text-gray-500")

            // Default empty
            case _ => Seq()
end TailwindUIStylesModule

// Default implementation that can be used directly
object DefaultTailwindUIStylesModule extends TailwindUIStylesModule
