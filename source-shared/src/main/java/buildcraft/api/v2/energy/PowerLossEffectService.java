package buildcraft.api.v2.energy;

@FunctionalInterface
public interface PowerLossEffectService {
    void onPowerLost(PowerLossContext context);
}
