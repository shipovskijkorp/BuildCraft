package buildcraft.api.v2.worldgen;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A code/datapack-friendly rule that places a registered BuildCraft resource-deposit profile
 * in selected dimensions/biomes.
 *
 * <p>The profile identifies the actual generator (for example BuildCraft's standard oil
 * deposit). Addons can therefore reuse BuildCraft generation without depending on its
 * generator implementation classes.
 */
public final class ResourceDepositRule {
    private final ResourceLocation id;
    private final ResourceLocation profile;
    private final WorldTargetSelector target;
    private final double frequencyMultiplier;
    private final int priority;
    private final boolean enabled;

    private ResourceDepositRule(Builder builder) {
        id = builder.id;
        profile = builder.profile;
        target = builder.target;
        frequencyMultiplier = builder.frequencyMultiplier;
        priority = builder.priority;
        enabled = builder.enabled;
    }

    public static Builder builder(ResourceLocation id, ResourceLocation profile) {
        return new Builder(id, profile);
    }

    public ResourceLocation id() { return id; }
    public ResourceLocation profile() { return profile; }
    public WorldTargetSelector target() { return target; }
    public double frequencyMultiplier() { return frequencyMultiplier; }
    public int priority() { return priority; }
    public boolean enabled() { return enabled; }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation profile;
        private WorldTargetSelector target = WorldTargetSelector.ALL;
        private double frequencyMultiplier = 1.0;
        private int priority;
        private boolean enabled = true;

        private Builder(ResourceLocation id, ResourceLocation profile) {
            this.id = Objects.requireNonNull(id, "id");
            this.profile = Objects.requireNonNull(profile, "profile");
        }

        public Builder target(WorldTargetSelector target) {
            this.target = Objects.requireNonNull(target, "target");
            return this;
        }

        public Builder frequencyMultiplier(double multiplier) {
            if (!Double.isFinite(multiplier) || multiplier < 0) {
                throw new IllegalArgumentException("frequencyMultiplier must be finite and >= 0");
            }
            this.frequencyMultiplier = multiplier;
            return this;
        }

        public Builder priority(int priority) { this.priority = priority; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public ResourceDepositRule build() { return new ResourceDepositRule(this); }
    }
}
