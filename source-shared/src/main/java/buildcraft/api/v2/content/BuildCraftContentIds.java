package buildcraft.api.v2.content;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable ids for built-in content intended to be reused as addon archetypes/profiles. */
public final class BuildCraftContentIds {
    public static final class Machines {
        public static final ResourceLocation QUARRY = id("buildcraftbuilders:quarry");
        public static final ResourceLocation DISTILLER = id("buildcraftfactory:distiller");
        public static final ResourceLocation MINING_WELL = id("buildcraftfactory:mining_well");
        public static final ResourceLocation PUMP = id("buildcraftfactory:pump");

        private Machines() {}
    }

    public static final class MachineComponents {
        public static final ResourceLocation ENERGY = id("buildcraft:energy");
        public static final ResourceLocation AREA = id("buildcraft:area");
        public static final ResourceLocation MINING = id("buildcraft:mining");
        public static final ResourceLocation PUMPING = id("buildcraft:pumping");
        public static final ResourceLocation DISTILLATION = id("buildcraft:distillation");
        public static final ResourceLocation INVENTORY_OUTPUT = id("buildcraft:inventory_output");
        public static final ResourceLocation FLUID_INPUT = id("buildcraft:fluid_input");
        public static final ResourceLocation FLUID_OUTPUT = id("buildcraft:fluid_output");
        public static final ResourceLocation CHUNK_LOADING = id("buildcraft:chunk_loading");

        private MachineComponents() {}
    }

    public static final class Worldgen {
        /** Standard BuildCraft oil-deposit generator profile. */
        public static final ResourceLocation STANDARD_OIL = id("buildcraftenergy:oil");

        private Worldgen() {}
    }

    private BuildCraftContentIds() {
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
