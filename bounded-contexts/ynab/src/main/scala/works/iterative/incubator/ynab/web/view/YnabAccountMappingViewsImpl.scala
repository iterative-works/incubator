package works.iterative.incubator.ynab.web.view

import works.iterative.scalatags.ScalatagsSupport
import scalatags.Text.TypedTag
import works.iterative.incubator.ynab.domain.model.YnabAccountMapping
import works.iterative.incubator.components.ScalatagsTailwindTable

/** Implementation of YnabAccountMappingViews using ScalaTags
  *
  * This class renders the HTML for YNAB account mapping related views.
  *
  * Classification: Web View Implementation
  */
class YnabAccountMappingViewsImpl extends YnabAccountMappingViews with ScalatagsSupport:
    import scalatags.Text.all.*
    import works.iterative.scalatags.sl as sl

    /** View for the account mapping list page
      *
      * @param mappings
      *   List of account mappings
      * @param sourceAccountMap
      *   Map of source account IDs to names
      * @param ynabAccountMap
      *   Map of YNAB account IDs to names
      * @return
      *   HTML for the account mapping list page
      */
    override def accountMappingList(
        mappings: List[YnabAccountMapping],
        sourceAccountMap: Map[Long, String],
        ynabAccountMap: Map[String, String]
    ): TypedTag[String] =
        def statusBadge(active: Boolean): TypedTag[String] =
            if active then
                sl.Badge(sl.variant := "success")("Active")
            else
                sl.Badge(sl.variant := "neutral")("Inactive")

        // Define table columns
        val columns = Seq(
            // Source Account column
            ScalatagsTailwindTable.Column[YnabAccountMapping](
                header = "Source Account",
                render = mapping =>
                    sourceAccountMap.get(mapping.sourceAccountId) match
                        case Some(name) =>
                            div(
                                div(name),
                                div(cls := "text-xs text-gray-500")(
                                    s"ID: ${mapping.sourceAccountId}"
                                )
                            )
                        case None =>
                            span(cls := "text-gray-500")(s"Unknown (${mapping.sourceAccountId})")
            ),

            // YNAB Account column
            ScalatagsTailwindTable.Column[YnabAccountMapping](
                header = "YNAB Account",
                render = mapping =>
                    ynabAccountMap.get(mapping.ynabAccountId) match
                        case Some(name) =>
                            div(
                                div(name),
                                div(cls := "text-xs text-gray-500")(s"ID: ${mapping.ynabAccountId}")
                            )
                        case None =>
                            span(cls := "text-gray-500")(s"Unknown (${mapping.ynabAccountId})")
            ),

            // Status column
            ScalatagsTailwindTable.Column[YnabAccountMapping](
                header = "Status",
                render = mapping => statusBadge(mapping.active)
            ),

            // Actions column
            ScalatagsTailwindTable.Column[YnabAccountMapping](
                header = "Actions",
                render = mapping =>
                    div(cls := "flex gap-2")(
                        a(
                            href := s"/ynab/account-mappings/${mapping.sourceAccountId}/edit",
                            cls := "px-2 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600"
                        )("Edit"),
                        form(
                            action := s"/ynab/account-mappings/${mapping.sourceAccountId}/delete",
                            method := "post",
                            onsubmit := "return confirm('Are you sure you want to delete this mapping?')",
                            cls := "inline"
                        )(
                            button(
                                `type` := "submit",
                                cls := "px-2 py-1 bg-red-500 text-white rounded text-sm hover:bg-red-600"
                            )("Delete")
                        )
                    )
            )
        )

        val mappingTable = ScalatagsTailwindTable
            .table(columns, mappings)
            .withClass("border-collapse mappings-table")
            .withHeaderClasses(Seq("bg-gray-100"))
            .render

        div(cls := "p-4")(
            // Header with title and actions
            div(cls := "flex justify-between items-center mb-4")(
                h1(cls := "text-2xl font-bold")("YNAB Account Mappings"),

                // Add new mapping button
                a(
                    href := "/ynab/account-mappings/new",
                    cls := "px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
                )("Add New Mapping")
            ),

            // Account mappings table
            div(cls := "overflow-x-auto")(
                if mappings.isEmpty then
                    div(cls := "text-center py-8 text-gray-500")(
                        "No account mappings found. Click 'Add New Mapping' to create one."
                    )
                else
                    mappingTable
            ),

            // Help text
            div(cls := "mt-6 p-4 bg-blue-50 rounded")(
                h3(cls := "text-lg font-medium text-blue-800")("About YNAB Account Mappings"),
                p(cls := "mt-2 text-blue-700")(
                    "Account mappings connect your source accounts (from banks) to YNAB accounts. ",
                    "This allows transactions to be automatically imported to the correct YNAB account."
                )
            )
        )
    end accountMappingList

    /** View for the account mapping form (create/edit)
      *
      * @param mapping
      *   Optional existing mapping (if editing)
      * @param sourceAccounts
      *   Available source accounts for mapping
      * @param ynabAccounts
      *   Available YNAB accounts for mapping
      * @return
      *   HTML for the account mapping form
      */
    override def accountMappingForm(
        mapping: Option[YnabAccountMapping],
        sourceAccounts: Seq[(Long, String)],
        ynabAccounts: Seq[(String, String)]
    ): TypedTag[String] =
        val isNew = mapping.isEmpty
        val formTitle =
            if isNew then "Add New YNAB Account Mapping" else "Edit YNAB Account Mapping"
        val submitButtonText = if isNew then "Create Mapping" else "Update Mapping"

        div(cls := "p-4 max-w-2xl mx-auto")(
            h1(cls := "text-2xl font-bold mb-6")(formTitle),
            form(
                action := "/ynab/account-mappings",
                method := "post",
                cls := "space-y-6"
            )(
                // Source Account selection
                div(cls := "space-y-2")(
                    label(
                        cls := "block text-sm font-medium text-gray-700",
                        `for` := "sourceAccountId"
                    )("Source Account"),
                    if sourceAccounts.isEmpty then
                        div(cls := "text-red-500 text-sm")(
                            "No unmapped source accounts available. All accounts are already mapped or no source accounts exist."
                        )
                    else
                        select(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            id := "sourceAccountId",
                            name := "sourceAccountId",
                            disabled := !isNew, // Can't change source account when editing
                            required := true
                        )(
                            if isNew then
                                option(value := "", disabled := true, selected := true)(
                                    "-- Select Source Account --"
                                )
                            else
                                frag()
                            ,
                            sourceAccounts.map { case (id, name) =>
                                option(
                                    value := id.toString,
                                    selected := mapping.exists(_.sourceAccountId == id)
                                )(s"$name (ID: $id)")
                            }
                        )
                    ,
                    if !isNew then
                        input(
                            `type` := "hidden",
                            name := "sourceAccountId",
                            value := mapping.map(_.sourceAccountId).getOrElse(0).toString
                        )
                    else
                        frag()
                ),

                // YNAB Account selection
                div(cls := "space-y-2")(
                    label(
                        cls := "block text-sm font-medium text-gray-700",
                        `for` := "ynabAccountId"
                    )("YNAB Account"),
                    if ynabAccounts.isEmpty then
                        div(cls := "text-red-500 text-sm")(
                            "No YNAB accounts available. Please check your YNAB connection."
                        )
                    else
                        select(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            id := "ynabAccountId",
                            name := "ynabAccountId",
                            required := true
                        )(
                            option(value := "", disabled := true, selected := !mapping.isDefined)(
                                "-- Select YNAB Account --"
                            ),
                            ynabAccounts.map { case (id, name) =>
                                option(
                                    value := id,
                                    selected := mapping.exists(_.ynabAccountId == id)
                                )(s"$name (ID: $id)")
                            }
                        )
                ),

                // Active status
                div(cls := "flex items-center")(
                    input(
                        cls := "h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded",
                        `type` := "checkbox",
                        id := "active",
                        name := "active",
                        value := "true",
                        checked := mapping.forall(_.active)
                    ),
                    label(
                        cls := "ml-2 block text-sm text-gray-700",
                        `for` := "active"
                    )("Active")
                ),

                // Form actions
                div(cls := "flex gap-4 pt-4")(
                    a(
                        href := "/ynab/account-mappings",
                        cls := "px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                    )("Cancel"),
                    button(
                        `type` := "submit",
                        cls := "px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
                    )(submitButtonText)
                )
            )
        )
    end accountMappingForm

    /** View for when a mapping is not found
      *
      * @param sourceAccountId
      *   The source account ID that wasn't found
      * @return
      *   HTML for the not found page
      */
    override def mappingNotFound(sourceAccountId: Long): TypedTag[String] =
        div(cls := "p-4")(
            div(cls := "bg-red-50 border-l-4 border-red-400 p-4 mb-4")(
                div(cls := "flex")(
                    div(cls := "flex-shrink-0")(
                        // Warning icon (using an emoji instead of SVG for simplicity)
                        span(cls := "text-red-400 text-xl")("⚠️")
                    ),
                    div(cls := "ml-3")(
                        h3(cls := "text-lg font-medium text-red-800")(
                            "Mapping Not Found"
                        ),
                        div(cls := "mt-2 text-red-700")(
                            s"Could not find a YNAB account mapping for source account with ID: $sourceAccountId"
                        )
                    )
                )
            ),
            div(cls := "flex mt-4")(
                a(
                    href := "/ynab/account-mappings",
                    cls := "px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                )("Back to Mappings")
            )
        )
end YnabAccountMappingViewsImpl
