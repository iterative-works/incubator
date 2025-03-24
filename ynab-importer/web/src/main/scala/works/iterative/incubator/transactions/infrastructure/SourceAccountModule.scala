package works.iterative.incubator.transactions
package infrastructure

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import zio.*
import works.iterative.scalatags.ScalatagsSupport
import works.iterative.scalatags.components.ScalatagsAppShell
import scalatags.text.Frag
import scalatags.Text.all.stringAttr
import org.http4s.Response
import org.http4s.Request
import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*
import cats.Applicative
import works.iterative.incubator.components.ScalatagsTailwindTable
import service.SourceAccountRepository
import java.time.{Instant, LocalDateTime, ZoneId}
import org.http4s.headers.Location
import org.http4s.Uri
import org.http4s.UrlForm

/** Web module for SourceAccount management
  *
  * This module provides UI for listing, adding, and editing source accounts.
  * Source accounts represent bank accounts from which transactions are imported.
  */
class SourceAccountModule(appShell: ScalatagsAppShell)
    extends ZIOWebModule[SourceAccountRepository]
    with ScalatagsSupport:

    object service:
        // Get all source accounts
        def getSourceAccounts(using
            req: Request[WebTask]
        ): WebTask[Seq[SourceAccount]] =
            ZIO.serviceWithZIO[SourceAccountRepository](
                _.find(SourceAccountQuery())
            )

        // Get a specific source account by ID
        def getSourceAccount(id: Long): WebTask[Option[SourceAccount]] =
            ZIO.serviceWithZIO[SourceAccountRepository](
                _.load(id)
            )

        // Save a source account (create or update)
        def saveSourceAccount(account: SourceAccount): WebTask[Unit] =
            ZIO.serviceWithZIO[SourceAccountRepository](
                _.save(account.id, account)
            )
    end service

    object view:
        import scalatags.Text.all.*
        import works.iterative.scalatags.sl as sl
        
        def accountNotFound(accountId: Long): scalatags.Text.TypedTag[String] =
            div(cls := "p-4 text-red-600")(
                s"Account with ID $accountId not found"
            )
            
        def sourceAccountList(
            accounts: Seq[SourceAccount]
        ): scalatags.Text.TypedTag[String] =

            def formatLastSync(instant: Option[Instant]): String =
                instant.map(i => 
                    LocalDateTime.ofInstant(i, ZoneId.systemDefault()).toString
                ).getOrElse("Never")

            def statusBadge(active: Boolean): scalatags.Text.TypedTag[String] =
                if (active)
                    sl.Badge(sl.variant := "success")("Active")
                else
                    sl.Badge(sl.variant := "neutral")("Inactive")

            // Define the table columns
            val columns = Seq(
                // ID column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "ID",
                    render = account =>
                        span(account.id.toString)
                ),

                // Name column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Name",
                    render = account =>
                        a(href := s"/source-accounts/${account.id}", cls := "text-blue-600 hover:underline")(
                            account.name
                        )
                ),

                // Account ID column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Account ID",
                    render = account =>
                        span(account.accountId)
                ),

                // Bank ID column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Bank ID",
                    render = account =>
                        span(account.bankId)
                ),

                // Currency column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Currency",
                    render = account =>
                        span(account.currency)
                ),

                // YNAB Account column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "YNAB Account",
                    render = account =>
                        account.ynabAccountId.map(id => 
                            span(id)
                        ).getOrElse(
                            span(cls := "text-gray-400 italic")("Not linked")
                        )
                ),

                // Status column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Status",
                    render = account =>
                        statusBadge(account.active)
                ),

                // Last Sync column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Last Sync",
                    render = account =>
                        span(formatLastSync(account.lastSyncTime))
                ),

                // Actions column
                ScalatagsTailwindTable.Column[SourceAccount](
                    header = "Actions",
                    render = account =>
                        div(cls := "flex gap-2")(
                            a(href := s"/source-accounts/${account.id}/edit", cls := "px-2 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600")(
                                "Edit"
                            ),
                            button(cls := "px-2 py-1 bg-red-500 text-white rounded text-sm hover:bg-red-600")(
                                "Delete"
                            )
                        )
                )
            )

            val accountTable = ScalatagsTailwindTable
                .table(columns, accounts)
                .withClass("border-collapse")
                .withHeaderClasses(Seq("bg-gray-100"))
                .render

            div(cls := "p-4")(
                // Header with title and actions
                div(cls := "flex justify-between items-center mb-4")(
                    h1(cls := "text-2xl font-bold")("Source Accounts"),

                    // Add new account button
                    a(href := "/source-accounts/new", cls := "px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600")(
                        "Add New Account"
                    )
                ),

                // Filters and controls
                div(cls := "mb-4 flex gap-2")(
                    sl.Select(
                        cls := "min-w-40",
                        sl.label := "Status",
                        sl.Option(value := "all", selected := true)("All"),
                        sl.Option(value := "active")("Active"),
                        sl.Option(value := "inactive")("Inactive")
                    ),
                    sl.Input(
                        cls := "min-w-60",
                        sl.label := "Search",
                        placeholder := "Search accounts..."
                    )
                ),

                // Account table
                div(cls := "overflow-x-auto")(
                    if (accounts.isEmpty)
                        div(cls := "text-center py-8 text-gray-500")(
                            "No source accounts found. Click 'Add New Account' to create one."
                        )
                    else
                        accountTable
                )
            )
        end sourceAccountList

        def sourceAccountForm(
            account: Option[SourceAccount] = None
        ): scalatags.Text.TypedTag[String] =

            val isNew = account.isEmpty
            val formTitle = if (isNew) "Add New Source Account" else "Edit Source Account"
            val submitButtonText = if (isNew) "Create Account" else "Update Account"
            val accountId = account.map(_.id).getOrElse(0L)
            val formAction = if (isNew) "/source-accounts" else s"/source-accounts/${accountId}"

            div(cls := "p-4 max-w-2xl mx-auto")(
                h1(cls := "text-2xl font-bold mb-6")(formTitle),

                form(action := formAction, method := "post", cls := "space-y-6")(
                    // Hidden ID field for updates
                    if (!isNew) input(`type` := "hidden", name := "id", value := accountId.toString) else frag(),

                    // Account Name
                    div(cls := "space-y-2")(
                        label(cls := "block text-sm font-medium text-gray-700", `for` := "name")("Account Name"),
                        input(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            `type` := "text",
                            id := "name",
                            name := "name",
                            value := account.map(_.name).getOrElse(""),
                            required := true
                        )
                    ),

                    // Account ID
                    div(cls := "space-y-2")(
                        label(cls := "block text-sm font-medium text-gray-700", `for` := "accountId")("Account ID"),
                        input(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            `type` := "text", 
                            id := "accountId",
                            name := "accountId",
                            value := account.map(_.accountId).getOrElse(""),
                            required := true
                        )
                    ),

                    // Bank ID
                    div(cls := "space-y-2")(
                        label(cls := "block text-sm font-medium text-gray-700", `for` := "bankId")("Bank ID"),
                        input(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            `type` := "text",
                            id := "bankId",
                            name := "bankId",
                            value := account.map(_.bankId).getOrElse(""),
                            required := true
                        )
                    ),

                    // Currency
                    div(cls := "space-y-2")(
                        label(cls := "block text-sm font-medium text-gray-700", `for` := "currency")("Currency"),
                        select(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            id := "currency",
                            name := "currency",
                            required := true
                        )(
                            option(value := "CZK", selected := account.exists(_.currency == "CZK"))("CZK"),
                            option(value := "EUR", selected := account.exists(_.currency == "EUR"))("EUR"),
                            option(value := "USD", selected := account.exists(_.currency == "USD"))("USD"),
                            option(value := "GBP", selected := account.exists(_.currency == "GBP"))("GBP")
                        )
                    ),

                    // YNAB Account ID
                    div(cls := "space-y-2")(
                        label(cls := "block text-sm font-medium text-gray-700", `for` := "ynabAccountId")("YNAB Account ID"),
                        input(
                            cls := "block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3",
                            `type` := "text",
                            id := "ynabAccountId",
                            name := "ynabAccountId",
                            value := account.flatMap(_.ynabAccountId).getOrElse("")
                        ),
                        p(cls := "text-xs text-gray-500")("Leave empty if not linked to YNAB")
                    ),

                    // Active status
                    div(cls := "flex items-center")(
                        input(
                            cls := "h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded",
                            `type` := "checkbox",
                            id := "active",
                            name := "active",
                            value := "true",
                            checked := account.forall(_.active)
                        ),
                        label(cls := "ml-2 block text-sm text-gray-700", `for` := "active")("Active")
                    ),

                    // Form actions
                    div(cls := "flex gap-4 pt-4")(
                        a(
                            href := "/source-accounts",
                            cls := "px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                        )("Cancel"),
                        button(
                            `type` := "submit",
                            cls := "px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
                        )(submitButtonText)
                    )
                )
            )
        end sourceAccountForm

        def sourceAccountDetail(
            account: SourceAccount
        ): scalatags.Text.TypedTag[String] =

            def formatLastSync(instant: Option[Instant]): String =
                instant.map(i => 
                    LocalDateTime.ofInstant(i, ZoneId.systemDefault()).toString
                ).getOrElse("Never")

            div(cls := "p-4 max-w-3xl mx-auto")(
                // Header with navigation and actions
                div(cls := "flex justify-between items-center mb-6")(
                    div(cls := "flex items-center gap-2")(
                        a(href := "/source-accounts", cls := "text-blue-600 hover:underline")("← Back to Accounts"),
                        h1(cls := "text-2xl font-bold")(account.name)
                    ),
                    div(cls := "flex gap-2")(
                        a(
                            href := s"/source-accounts/${account.id}/edit",
                            cls := "px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                        )("Edit"),
                        button(cls := "px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600")("Delete")
                    )
                ),

                // Account details
                div(cls := "bg-white shadow rounded-lg p-6")(
                    h2(cls := "text-xl font-semibold mb-4")("Account Details"),
                    div(cls := "grid grid-cols-1 md:grid-cols-2 gap-4")(
                        // Left column
                        div(cls := "space-y-4")(
                            div(
                                p(cls := "font-medium text-gray-500")("ID"),
                                p(cls := "text-lg")(account.id.toString)
                            ),
                            div(
                                p(cls := "font-medium text-gray-500")("Account Name"),
                                p(cls := "text-lg")(account.name)
                            ),
                            div(
                                p(cls := "font-medium text-gray-500")("Account ID"),
                                p(cls := "text-lg")(account.accountId)
                            ),
                            div(
                                p(cls := "font-medium text-gray-500")("Bank ID"),
                                p(cls := "text-lg")(account.bankId)
                            )
                        ),
                        // Right column
                        div(cls := "space-y-4")(
                            div(
                                p(cls := "font-medium text-gray-500")("Currency"),
                                p(cls := "text-lg")(account.currency)
                            ),
                            div(
                                p(cls := "font-medium text-gray-500")("YNAB Account ID"),
                                p(cls := "text-lg")(
                                    account.ynabAccountId.getOrElse("") match {
                                      case "" => span(cls := "text-gray-400 italic")("Not linked")
                                      case id => span(id)
                                    }
                                )
                            ),
                            div(
                                p(cls := "font-medium text-gray-500")("Status"),
                                p(cls := "text-lg")(
                                    if (account.active)
                                        sl.Badge(sl.variant := "success")("Active")
                                    else
                                        sl.Badge(sl.variant := "neutral")("Inactive")
                                )
                            ),
                            div(
                                p(cls := "font-medium text-gray-500")("Last Sync"),
                                p(cls := "text-lg")(formatLastSync(account.lastSyncTime))
                            )
                        )
                    )
                ),

                // Transactions section placeholder
                div(cls := "mt-8 bg-white shadow rounded-lg p-6")(
                    h2(cls := "text-xl font-semibold mb-4")("Recent Transactions"),
                    p(cls := "text-gray-500 italic")(
                        "Showing transactions from this account will be implemented in a future update."
                    )
                )
            )
        end sourceAccountDetail
    end view

    override def routes: HttpRoutes[WebTask] =
        val dsl = Http4sDsl[WebTask]
        import dsl.*

        def of[F[_]: Monad](pf: PartialFunction[Request[F], Request[F] => F[Response[F]]])
            : HttpRoutes[F] =
            Kleisli: req =>
                OptionT(Applicative[F].unit >> pf.lift(req).map(_(req)).sequence)

        def respondZIO[A](
            provide: Request[WebTask] ?=> WebTask[A],
            render: A => Frag
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(data =>
                    Ok(appShell.wrap("Source Accounts", render(data)))
                )

        def respondZIOFull[A](
            provide: Request[WebTask] ?=> WebTask[A],
            reply: A => WebTask[Response[WebTask]]
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(resp => reply(resp))

        of[WebTask] {
            // List all source accounts
            case GET -> Root / "source-accounts" =>
                respondZIO(
                    service.getSourceAccounts,
                    accounts => view.sourceAccountList(accounts)
                )

            // Show form to create a new source account
            case GET -> Root / "source-accounts" / "new" =>
                respondZIO(
                    ZIO.succeed(()),
                    _ => view.sourceAccountForm(None)
                )

            // Show form to edit an existing source account
            case GET -> Root / "source-accounts" / LongVar(accountId) / "edit" =>
                respondZIO(
                    service.getSourceAccount(accountId),
                    accountOpt => 
                        accountOpt.fold(
                            view.accountNotFound(accountId)
                        )(account => 
                            view.sourceAccountForm(Some(account))
                        )
                )

            // Show source account details
            case GET -> Root / "source-accounts" / LongVar(accountId) =>
                respondZIO(
                    service.getSourceAccount(accountId),
                    accountOpt => 
                        accountOpt.fold(
                            view.accountNotFound(accountId)
                        )(account => 
                            view.sourceAccountDetail(account)
                        )
                )

            // Create a new source account (form submission)
            case req @ POST -> Root / "source-accounts" =>
                respondZIOFull(
                    ZIO.succeed(()),
                    _ => 
                        req.as[UrlForm].flatMap { form =>
                            // Parse form data to create a new SourceAccount
                            // Note: In a real implementation, you would validate input and handle errors
                            val id = form.values.get("id").flatMap(_.headOption).flatMap(_.toLongOption).getOrElse(0L)
                            val newAccount = SourceAccount(
                                id = id,
                                accountId = form.values.get("accountId").flatMap(_.headOption).getOrElse(""),
                                bankId = form.values.get("bankId").flatMap(_.headOption).getOrElse(""),
                                name = form.values.get("name").flatMap(_.headOption).getOrElse(""),
                                currency = form.values.get("currency").flatMap(_.headOption).getOrElse(""),
                                ynabAccountId = form.values.get("ynabAccountId").flatMap(_.headOption).filter(_.nonEmpty),
                                active = form.values.get("active").exists(_.contains("true"))
                            )

                            for
                                _ <- service.saveSourceAccount(newAccount)
                                resp <- SeeOther(Location(Uri.unsafeFromString("/source-accounts")))
                            yield resp
                        }
                )
        }
    end routes
end SourceAccountModule