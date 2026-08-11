package buildcraft.api.v2.pipe;

public record ExternalEnergyTransportProfile(long maxPerTick, boolean extractor) {
    public ExternalEnergyTransportProfile {
        if (maxPerTick < 0) throw new IllegalArgumentException("maxPerTick must be non-negative");
    }
}
