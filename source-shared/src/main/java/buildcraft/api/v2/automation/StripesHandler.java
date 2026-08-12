package buildcraft.api.v2.automation;

@FunctionalInterface
public interface StripesHandler {
    AutomationResult activate(StripesContext context);

    /** Higher values run first. The default keeps addon handlers in the normal priority band. */
    default int priority() { return 0; }
}
