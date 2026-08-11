package buildcraft.api.v2.pipe;

public interface FluidRouteComponent extends PipeComponent {
    RouteDecision route(FluidRouteContext context);
}
