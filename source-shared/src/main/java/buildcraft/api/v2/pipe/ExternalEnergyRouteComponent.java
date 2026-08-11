package buildcraft.api.v2.pipe;

public interface ExternalEnergyRouteComponent extends PipeComponent {
    RouteDecision route(ExternalEnergyRouteContext context);
}
