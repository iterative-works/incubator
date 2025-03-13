package works.iterative.incubator.components

/** Complete Scalatags + Tailwind implementation of the Table UI module
  */
trait ScalatagsTailwindTableUIModule extends ScalatagsTableUIModule with TailwindUIStylesModule

// Singleton implementation for use without mixing in
object ScalatagsTailwindTable extends ScalatagsTailwindTableUIModule
