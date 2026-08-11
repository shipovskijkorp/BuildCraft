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
