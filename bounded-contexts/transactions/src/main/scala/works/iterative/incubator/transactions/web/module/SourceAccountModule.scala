package works.iterative.incubator.transactions.web.module

import works.iterative.server.http.ZIOWebModule
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import zio.interop.catz.*
import zio.*
import works.iterative.scalatags.ScalatagsSupport
import works.iterative.scalatags.components.ScalatagsAppShell
import scalatags.text.Frag
import org.http4s.Response
import org.http4s.Request
import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*
import cats.Applicative
import works.iterative.incubator.transactions.domain.repository.SourceAccountRepository
import works.iterative.incubator.transactions.domain.model.{SourceAccount, CreateSourceAccount}
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery
import org.http4s.headers.Location
import org.http4s.Uri
import org.http4s.UrlForm
import works.iterative.incubator.transactions.web.view.SourceAccountViews
import works.iterative.incubator.transactions.web.view.SourceAccountViewsImpl

/** Web module for SourceAccount management
  *
  * This module provides UI for listing, adding, and editing source accounts. Source accounts
  * represent bank accounts from which transactions are imported.
  *
  * Classification: Web Module
  */
class SourceAccountModule(appShell: ScalatagsAppShell)
    extends ZIOWebModule[SourceAccountRepository]
    with ScalatagsSupport:

    // Use the extracted view implementation
    private val views: SourceAccountViews = new SourceAccountViewsImpl()

    object service:
        // Get source accounts with optional filtering
        def getSourceAccounts(showAll: Boolean = false)(using
            req: Request[WebTask]
        ): WebTask[Seq[SourceAccount]] =
            val query =
                if showAll then SourceAccountQuery() else SourceAccountQuery(active = Some(true))
            ZIO.serviceWithZIO[SourceAccountRepository](
                _.find(query)
            )
        end getSourceAccounts

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

        // Create a new source account using the Create pattern
        def createSourceAccount(createAccount: CreateSourceAccount): WebTask[Long] =
            ZIO.serviceWithZIO[SourceAccountRepository](
                _.create(createAccount)
            )

        // Delete a source account by ID
        def deleteSourceAccount(id: Long): WebTask[Unit] =
            ZIO.serviceWithZIO[SourceAccountRepository] { repo =>
                for
                    accountOpt <- repo.load(id)
                    _ <- ZIO.foreach(accountOpt)(account =>
                        // Mark the account as inactive instead of completely deleting it
                        // This is a soft delete approach that preserves data integrity
                        repo.save(id, account.copy(active = false))
                    )
                yield ()
            }
    end service

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
            // List source accounts with optional filter
            case GET -> Root / "source-accounts" :? params =>
                // Parse the status parameter with a default of "active"
                val statusParam = params.get("status").flatMap(_.headOption).getOrElse("active")
                val showAll = statusParam == "all"
                val showInactive = statusParam == "inactive"

                respondZIO(
                    if showInactive then
                        ZIO.serviceWithZIO[SourceAccountRepository](
                            _.find(SourceAccountQuery(active = Some(false)))
                        )
                    else if showAll then
                        service.getSourceAccounts(showAll = true)
                    else
                        service.getSourceAccounts(showAll = false)
                    ,
                    accounts => views.sourceAccountList(accounts, statusParam)
                )

            // Show form to create a new source account
            case GET -> Root / "source-accounts" / "new" =>
                respondZIO(
                    ZIO.succeed(()),
                    _ => views.sourceAccountForm(None)
                )

            // Show form to edit an existing source account
            case GET -> Root / "source-accounts" / LongVar(accountId) / "edit" =>
                respondZIO(
                    service.getSourceAccount(accountId),
                    accountOpt =>
                        accountOpt.fold(
                            views.accountNotFound(accountId)
                        )(account =>
                            views.sourceAccountForm(Some(account))
                        )
                )

            // Show source account details
            case GET -> Root / "source-accounts" / LongVar(accountId) =>
                respondZIO(
                    service.getSourceAccount(accountId),
                    accountOpt =>
                        accountOpt.fold(
                            views.accountNotFound(accountId)
                        )(account =>
                            views.sourceAccountDetail(account)
                        )
                )

            // Create a new source account (form submission)
            case req @ POST -> Root / "source-accounts" =>
                respondZIOFull(
                    ZIO.succeed(()),
                    _ =>
                        req.as[UrlForm].flatMap { form =>
                            // Check if this is an update (has ID) or a new account creation
                            val idOpt =
                                form.values.get("id").flatMap(_.headOption).flatMap(_.toLongOption)

                            val result = idOpt match
                                case Some(id) if id > 0 =>
                                    // This is an update of an existing account
                                    val updatedAccount = SourceAccount(
                                        id = id,
                                        accountId = form.values.get("accountId").flatMap(
                                            _.headOption
                                        ).getOrElse(""),
                                        bankId =
                                            form.values.get("bankId").flatMap(
                                                _.headOption
                                            ).getOrElse(""),
                                        name = form.values.get("name").flatMap(
                                            _.headOption
                                        ).getOrElse(""),
                                        currency =
                                            form.values.get("currency").flatMap(
                                                _.headOption
                                            ).getOrElse(""),
                                        active =
                                            form.values.get("active").exists(_.contains("true"))
                                    )
                                    service.saveSourceAccount(updatedAccount).as(id)

                                case _ =>
                                    // This is a new account creation
                                    val createAccount = CreateSourceAccount(
                                        accountId = form.values.get("accountId").flatMap(
                                            _.headOption
                                        ).getOrElse(""),
                                        bankId =
                                            form.values.get("bankId").flatMap(
                                                _.headOption
                                            ).getOrElse(""),
                                        name = form.values.get("name").flatMap(
                                            _.headOption
                                        ).getOrElse(""),
                                        currency =
                                            form.values.get("currency").flatMap(
                                                _.headOption
                                            ).getOrElse(""),
                                        active =
                                            form.values.get("active").exists(_.contains("true"))
                                    )
                                    service.createSourceAccount(createAccount)

                            for
                                id <- result // Now returns the ID in both cases
                                resp <- SeeOther(
                                    Location(Uri.unsafeFromString(s"/source-accounts/$id"))
                                )
                            yield resp
                            end for
                        }
                )

            // Delete a source account
            case POST -> Root / "source-accounts" / LongVar(accountId) / "delete" =>
                respondZIOFull(
                    ZIO.succeed(()),
                    _ =>
                        for
                            _ <- service.deleteSourceAccount(accountId)
                            resp <- SeeOther(Location(Uri.unsafeFromString("/source-accounts")))
                        yield resp
                )
        }
    end routes
end SourceAccountModule
