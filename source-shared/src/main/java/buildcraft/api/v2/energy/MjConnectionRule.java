package buildcraft.api.v2.energy;

@FunctionalInterface
public interface MjConnectionRule {
    boolean canConnect(MjConnectionContext context);
}
