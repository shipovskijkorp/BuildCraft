package buildcraft.api.v2.pipe;

public interface ItemRouteComponent extends PipeComponent {
    RouteDecision route(ItemRouteContext context);
}
