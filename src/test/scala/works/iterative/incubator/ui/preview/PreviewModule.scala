package works.iterative.incubator.ui.preview

import works.iterative.server.http.tapir.TapirEndpointModule
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import scalatags.Text.Frag
import scalatags.Text.all.raw
import zio.*
import works.iterative.tapir.BaseUri

/** Base trait for all component preview modules
  */
trait PreviewModule extends TapirEndpointModule[PreviewEnv]:
    /** Base path for this preview module */
    def basePath: List[String]

    /** Base path as endpoint input for this preview module */
    def basePathInput: EndpointInput[Unit] = basePath.foldLeft(emptyInput)(_ / _)

    /** AppShell for rendering pages */
    def appShell: PreviewAppShell

    /** Base URI for constructing URLs */
    def baseUri: BaseUri

    /** Title of this preview module */
    def title: String = basePath.lastOption.getOrElse("Preview").capitalize

    /** The current path for highlighting in navigation */
    def currentPath: String = s"/preview/${basePath.mkString("-")}"

    /** Body type for HTML fragments */
    val fragBody = htmlBodyUtf8.map[Frag](raw)(_.render)

    /** Base endpoint for preview with common path prefix and error handling */
    val baseEndpoint = endpoint
        .in("preview")
        .errorOut(stringBody)
        .out(fragBody)
end PreviewModule

/** A preview module that allows showing a component in different states
  *
  * @tparam VM
  *   The view model type for the component
  */
trait ComponentStatePreviewModule[VM] extends PreviewModule:

    /** Available states for this component */
    def states: List[ComponentState[VM]]

    /** Endpoint for the component state list */
    val listStatesEndpoint = baseEndpoint
        .in(basePathInput)
        .description(s"List of available states for $title component")
        .get

    /** Endpoint for viewing a specific component state */
    def stateEndpoint(stateName: String) = baseEndpoint
        .in(basePathInput / stateName)
        .description(s"Preview $title component in $stateName state")
        .get

    /** Render the list of available states */
    def renderStatesList: ZIO[Any, String, Frag]

    /** Render a specific component state */
    def renderState(stateName: String): ZIO[Any, String, Frag]

    /** Server endpoint for the state list */
    val listStatesServerEndpoint = listStatesEndpoint.zServerLogic(_ => renderStatesList)

    /** Server endpoints for all states - using lazy val to avoid initialization order issues */
    lazy val stateServerEndpoints = states.map { state =>
        stateEndpoint(state.name).zServerLogic(_ => renderState(state.name))
    }

    /** All endpoints for documentation */
    override lazy val endpoints =
        val stateEndpoints = states.map(state => stateEndpoint(state.name))
        listStatesEndpoint :: stateEndpoints

    /** All server endpoints for routing - using lazy val to avoid initialization order issues */
    override lazy val serverEndpoints: List[ServerEndpoint[Any, RIO[PreviewEnv, *]]] =
        val allEndpoints = listStatesServerEndpoint :: stateServerEndpoints
        allEndpoints.map(_.asInstanceOf[ServerEndpoint[Any, RIO[PreviewEnv, *]]])
end ComponentStatePreviewModule

/** Represents a specific state of a component for previewing
  *
  * @param name
  *   The name of the state
  * @param description
  *   A description of what this state represents
  * @param viewModel
  *   The view model for this state
  * @tparam VM
  *   The view model type
  */
case class ComponentState[VM](
    name: String,
    description: String,
    viewModel: VM
)
