package buildcraft.api.v2.pipe;

public interface MjRouteComponent extends PipeComponent {
    RouteDecision route(MjRouteContext context);
}
