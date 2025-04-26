package works.iterative.incubator.ynab.web.module

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
import works.iterative.incubator.ynab.domain.repository.YnabAccountMappingRepository
import works.iterative.incubator.ynab.domain.model.{YnabAccountMapping, CreateYnabAccountMapping}
import works.iterative.incubator.ynab.application.service.YnabService
import works.iterative.incubator.transactions.domain.repository.SourceAccountRepository
import works.iterative.incubator.transactions.domain.query.SourceAccountQuery
import org.http4s.headers.Location
import org.http4s.Uri
import org.http4s.UrlForm
import works.iterative.incubator.ynab.web.view.YnabAccountMappingViews
import works.iterative.incubator.ynab.web.view.YnabAccountMappingViewsImpl

/** Web module for YNAB Account Mapping management
  *
  * This module provides UI for creating, editing, and managing mappings between source accounts and
  * YNAB accounts.
  *
  * Classification: Web Module
  */
class YnabAccountMappingModule(appShell: ScalatagsAppShell)
    extends ZIOWebModule[YnabAccountMappingRepository & YnabService & SourceAccountRepository]
    with ScalatagsSupport:

    // Create view implementation
    private val views: YnabAccountMappingViews = new YnabAccountMappingViewsImpl()

    object service:
        // Get all YNAB account mappings
        def getAllMappings(using req: Request[WebTask]): WebTask[List[YnabAccountMapping]] =
            ZIO.serviceWithZIO[YnabAccountMappingRepository](_.findAll())

        // Get all active YNAB account mappings
        def getActiveMappings(using req: Request[WebTask]): WebTask[List[YnabAccountMapping]] =
            ZIO.serviceWithZIO[YnabAccountMappingRepository](_.findAllActive())

        // Create a new YNAB account mapping
        def createMapping(mapping: CreateYnabAccountMapping): WebTask[YnabAccountMapping] =
            ZIO.serviceWithZIO[YnabAccountMappingRepository](_.save(mapping))

        // Update a YNAB account mapping
        def updateMapping(mapping: YnabAccountMapping): WebTask[YnabAccountMapping] =
            ZIO.serviceWithZIO[YnabAccountMappingRepository](_.update(mapping))

        // Delete a YNAB account mapping
        def deleteMapping(sourceAccountId: Long): WebTask[Unit] =
            ZIO.serviceWithZIO[YnabAccountMappingRepository](_.delete(sourceAccountId))

        // Get source accounts that don't have a YNAB mapping
        def getUnmappedSourceAccounts(using req: Request[WebTask]): WebTask[Seq[(Long, String)]] =
            for
                sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                mappingRepo <- ZIO.service[YnabAccountMappingRepository]
                sourceAccounts <- sourceAccountRepo.find(SourceAccountQuery(active = Some(true)))
                mappings <- mappingRepo.findAll()
                mappedAccountIds = mappings.map(_.sourceAccountId).toSet
                unmappedAccounts =
                    sourceAccounts.filterNot(account => mappedAccountIds.contains(account.id))
                accountsWithNames = unmappedAccounts.map(account => (account.id, account.name))
            yield accountsWithNames

        // Get YNAB accounts available for mapping
        def getYnabAccounts(using req: Request[WebTask]): WebTask[Seq[(String, String)]] =
            for
                ynabService <- ZIO.service[YnabService]
                // Get all available budgets
                budgets <- ynabService.getBudgets()
                // Get accounts from the first budget
                budgetId = budgets.headOption.map(_.id).getOrElse("")
                accounts <-
                    if budgetId.nonEmpty then
                        ynabService.getBudgetService(budgetId).getAccounts()
                    else
                        ZIO.succeed(Seq.empty)
                accountsWithNames = accounts.map(account => (account.id, account.name))
            yield accountsWithNames
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
                    Ok(appShell.wrap("YNAB Account Mappings", render(data)))
                )

        def respondZIOFull[A](
            provide: Request[WebTask] ?=> WebTask[A],
            reply: A => WebTask[Response[WebTask]]
        ): Request[WebTask] => WebTask[Response[WebTask]] =
            req =>
                provide(using req).flatMap(resp => reply(resp))

        of[WebTask] {
            // List all account mappings
            case GET -> Root / "ynab" / "account-mappings" =>
                respondZIO(
                    for
                        mappings <- service.getAllMappings
                        // Get source accounts to display names
                        sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                        sourceAccounts <- sourceAccountRepo.find(SourceAccountQuery())

                        // Get YNAB accounts to display names
                        ynabService <- ZIO.service[YnabService]
                        budgets <- ynabService.getBudgets()
                        budgetId = budgets.headOption.map(_.id).getOrElse("")
                        ynabAccounts <-
                            if budgetId.nonEmpty then
                                ynabService.getBudgetService(budgetId).getAccounts()
                            else
                                ZIO.succeed(Seq.empty)
                    yield (mappings, sourceAccounts, ynabAccounts),
                    dataTriple =>
                        // Explicitly extract and type the components
                        val (mappings, sourceAccounts, ynabAccounts) = dataTriple

                        // Create lookup maps for account names
                        val sourceAccountMap =
                            sourceAccounts.asInstanceOf[Seq[
                                works.iterative.incubator.transactions.domain.model.SourceAccount
                            ]]
                                .map(acc => acc.id -> acc.name).toMap

                        val ynabAccountMap =
                            ynabAccounts.asInstanceOf[Seq[
                                works.iterative.incubator.ynab.domain.model.YnabAccount
                            ]]
                                .map(acc => acc.id -> acc.name).toMap

                        views.accountMappingList(
                            mappings.asInstanceOf[List[
                                works.iterative.incubator.ynab.domain.model.YnabAccountMapping
                            ]],
                            sourceAccountMap,
                            ynabAccountMap
                        )
                )

            // Show form to create a new mapping
            case GET -> Root / "ynab" / "account-mappings" / "new" =>
                respondZIO(
                    for
                        unmappedAccounts <- service.getUnmappedSourceAccounts
                        ynabAccounts <- service.getYnabAccounts
                    yield (unmappedAccounts, ynabAccounts),
                    data =>
                        val (unmappedAccounts, ynabAccounts) = data
                        views.accountMappingForm(None, unmappedAccounts, ynabAccounts)
                )

            // Show form to edit a mapping
            case GET -> Root / "ynab" / "account-mappings" / LongVar(sourceAccountId) / "edit" =>
                respondZIO(
                    for
                        mappingRepo <- ZIO.service[YnabAccountMappingRepository]
                        mappingOpt <- mappingRepo.findBySourceAccountId(sourceAccountId)
                        ynabAccounts <- service.getYnabAccounts

                        // We also need the source account name
                        sourceAccountRepo <- ZIO.service[SourceAccountRepository]
                        sourceAccountOpt <- sourceAccountRepo.load(sourceAccountId)
                    yield (mappingOpt, ynabAccounts, sourceAccountOpt),
                    data =>
                        val (mappingOpt, ynabAccounts, sourceAccountOpt) = data

                        // If we find both the mapping and the account, show the edit form
                        if mappingOpt.isDefined && sourceAccountOpt.isDefined then
                            val sourceAccount = sourceAccountOpt.get
                            val sourceAccountInfo = Seq((sourceAccount.id, sourceAccount.name))
                            views.accountMappingForm(mappingOpt, sourceAccountInfo, ynabAccounts)
                        else
                            views.mappingNotFound(sourceAccountId)
                        end if
                )

            // Create or update a mapping (form submission)
            case req @ POST -> Root / "ynab" / "account-mappings" =>
                respondZIOFull(
                    ZIO.succeed(()),
                    _ =>
                        req.as[UrlForm].flatMap { form =>
                            // Get the source account ID and YNAB account ID from the form
                            val sourceAccountIdOpt = form.values.get("sourceAccountId").flatMap(
                                _.headOption.flatMap(_.toLongOption)
                            )
                            val ynabAccountIdOpt =
                                form.values.get("ynabAccountId").flatMap(_.headOption)
                            val active = form.values.get("active").exists(_.contains("true"))

                            // Validate form data
                            sourceAccountIdOpt.flatMap(sourceId =>
                                ynabAccountIdOpt.map(ynabId => (sourceId, ynabId))
                            ) match
                                case Some((sourceId, ynabId)) =>
                                    // Check if this is an update of an existing mapping
                                    for
                                        mappingRepo <- ZIO.service[YnabAccountMappingRepository]
                                        existingOpt <- mappingRepo.findBySourceAccountId(sourceId)
                                        result <- existingOpt match
                                            case Some(existing) =>
                                                // Update existing mapping
                                                val updated = existing.copy(
                                                    ynabAccountId = ynabId,
                                                    active = active
                                                )
                                                service.updateMapping(updated)
                                            case None =>
                                                // Create new mapping
                                                val newMapping = CreateYnabAccountMapping(
                                                    sourceAccountId = sourceId,
                                                    ynabAccountId = ynabId,
                                                    active = active
                                                )
                                                service.createMapping(newMapping)
                                        resp <- SeeOther(
                                            Location(Uri.unsafeFromString("/ynab/account-mappings"))
                                        )
                                    yield resp
                                case None =>
                                    BadRequest("Missing required fields")
                            end match
                        }
                )

            // Delete a mapping
            case POST -> Root / "ynab" / "account-mappings" / LongVar(sourceAccountId) / "delete" =>
                respondZIOFull(
                    ZIO.succeed(()),
                    _ =>
                        for
                            _ <- service.deleteMapping(sourceAccountId)
                            resp <-
                                SeeOther(Location(Uri.unsafeFromString("/ynab/account-mappings")))
                        yield resp
                )
        }
    end routes
end YnabAccountMappingModule
