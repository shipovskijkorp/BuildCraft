package buildcraft.energy;

import buildcraft.energy.generation.features.OilFeatureConfiguration;
import buildcraft.energy.generation.features.OilGenFeature;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** Registers the code-backed oil feature. Biome injection is data-driven through Forge biome modifiers. */
public final class BCEnergyWorldGen {
    public static final DeferredRegister<Feature<?>> FEATURE_REGISTER =
        DeferredRegister.create(ForgeRegistries.FEATURES, BCEnergy.MODID);

    public static final TagKey<Biome> IS_OIL_BIOME = TagKey.create(
        Registry.BIOME_REGISTRY, new ResourceLocation(BCEnergy.MODID, "is_oil_biome")
    );

    public static final OilGenFeature OIL_FEATURE = new OilGenFeature(OilFeatureConfiguration.CODEC);

    private BCEnergyWorldGen() {
    }

    public static void preInit(IEventBus modEventBus) {
        BCEnergyBiomeModifiers.register(modEventBus);
        FEATURE_REGISTER.register("worldgen.feature.oil", () -> OIL_FEATURE);
        FEATURE_REGISTER.register(modEventBus);
    }
}
