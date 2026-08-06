package buildcraft.energy;

import buildcraft.energy.generation.features.OilFeatureConfiguration;
import buildcraft.energy.generation.features.OilGenFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers the code-backed oil feature. Biome injection is data-driven through Forge biome modifiers on 1.21.1. */
public final class BCEnergyWorldGen {
    public static final DeferredRegister<Feature<?>> FEATURE_REGISTER =
        DeferredRegister.create(Registries.FEATURE, BCEnergy.MODID);

    public static final TagKey<Biome> IS_OIL_BIOME = TagKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BCEnergy.MODID, "is_oil_biome")
    );

    /** Retained as stable API keys; custom biomes are no longer registered imperatively. */
    public static final ResourceKey<Biome> OIL_DESERT_KEY = ResourceKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BCEnergy.MODID, "oil_desert")
    );
    public static final ResourceKey<Biome> OIL_DEEP_OCEAN_KEY = ResourceKey.create(
        Registries.BIOME, ResourceLocation.fromNamespaceAndPath(BCEnergy.MODID, "oil_deep_ocean")
    );

    public static final RegistryObject<Feature<?>> OIL_FEATURE = FEATURE_REGISTER.register(
        "worldgen.feature.oil", () -> new OilGenFeature(OilFeatureConfiguration.CODEC)
    );

    private BCEnergyWorldGen() {
    }

    public static void preInit(IEventBus modEventBus) {
        FEATURE_REGISTER.register(modEventBus);
    }
}
