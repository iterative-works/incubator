package works.iterative.incubator.components

import works.iterative.ui.components.TableUIModule
import works.iterative.ui.components.UIStylesModule
import scalatags.Text.all.*

trait ScalatagsTableUIModule extends TableUIModule[Frag]:
    this: UIStylesModule[Nothing] =>

    // Implement the rendering method for tables
    override protected def renderTable[A](builder: TableBuilder[A]): Frag =
        val tableAttrs = builder.attributes.toSeq.map { case (k, v) => attr(k) := v }
        val tableClasses = cls := builder.tableClasses.mkString(" ")

        // Construct the table header
        val tableHeader = thead(
            cls := builder.headerClasses.mkString(" ")
        )(
            tr(
                cls := builder.headerRowClasses.mkString(" ")
            )(
                builder.columns.map { col =>
                    th(cls := builder.headerCellClasses.mkString(" "))(col.header)
                }
            )
        )

        // Construct the table body
        val tableBody = tbody(
            cls := builder.bodyClasses.mkString(" ")
        )(
            builder.data.map { item =>
                tr(cls := builder.rowClasses.mkString(" "))(
                    builder.columns.map { col =>
                        val cellClassValue = builder.cellClasses ++ Seq(col.className(item))
                        td(cls := cellClassValue.filterNot(_.isEmpty).mkString(" "))(
                            col.render(item)
                        )
                    }
                )
            }
        )

        // Assemble the complete table
        scalatags.Text.tags.table(tableClasses, tableAttrs)(
            tableHeader,
            tableBody
        )
    end renderTable
end ScalatagsTableUIModule
