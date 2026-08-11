package buildcraft.api.v2.automation;

@FunctionalInterface
public interface AutomationHandler<R extends AutomationRequest> {
    AutomationResult execute(R request);
}
