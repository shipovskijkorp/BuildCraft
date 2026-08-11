package buildcraft.api.v2.automation;

@FunctionalInterface
public interface StripesHandler {
    AutomationResult activate(StripesContext context);
}
