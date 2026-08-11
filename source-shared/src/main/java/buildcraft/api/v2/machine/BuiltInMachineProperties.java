package buildcraft.api.v2.machine;

import buildcraft.api.v2.energy.MjAmount;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Common configuration keys understood by reusable BuildCraft machine components.
 *
 * <p>A machine may ignore a property when none of its components consume that property.
 * Addons can define additional {@link MachineProperty} keys for their own components.
 */
public final class BuiltInMachineProperties {
    public static final MachineProperty<Double> WORK_SPEED_MULTIPLIER = MachineProperty.constrained(
        id("work_speed_multiplier"), Double.class, value -> Double.isFinite(value) && value > 0
    );
    public static final MachineProperty<Double> ENERGY_COST_MULTIPLIER = MachineProperty.constrained(
        id("energy_cost_multiplier"), Double.class, value -> Double.isFinite(value) && value >= 0
    );
    public static final MachineProperty<MjAmount> MAX_MJ_INPUT_PER_TICK = MachineProperty.of(
        id("max_mj_input_per_tick"), MjAmount.class
    );
    public static final MachineProperty<MjAmount> MJ_CAPACITY = MachineProperty.of(
        id("mj_capacity"), MjAmount.class
    );
    public static final MachineProperty<Integer> INVENTORY_SLOTS = MachineProperty.constrained(
        id("inventory_slots"), Integer.class, value -> value >= 0
    );
    public static final MachineProperty<Integer> RANGE = MachineProperty.constrained(
        id("range"), Integer.class, value -> value >= 0
    );
    public static final MachineProperty<Boolean> CHUNK_LOADING = MachineProperty.of(
        id("chunk_loading"), Boolean.class
    );

    private BuiltInMachineProperties() {
    }

    public static MachineProperty<?>[] values() {
        return new MachineProperty<?>[] {
            WORK_SPEED_MULTIPLIER,
            ENERGY_COST_MULTIPLIER,
            MAX_MJ_INPUT_PER_TICK,
            MJ_CAPACITY,
            INVENTORY_SLOTS,
            RANGE,
            CHUNK_LOADING
        };
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("buildcraft:" + path));
    }
}
