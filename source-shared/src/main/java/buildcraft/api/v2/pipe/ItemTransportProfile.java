package buildcraft.api.v2.pipe;

public record ItemTransportProfile(int maxItemsPerCycle, int routingWeight) {
    public ItemTransportProfile {
        if (maxItemsPerCycle <= 0) throw new IllegalArgumentException("maxItemsPerCycle must be positive");
        if (routingWeight < 0) throw new IllegalArgumentException("routingWeight must be non-negative");
    }
}
