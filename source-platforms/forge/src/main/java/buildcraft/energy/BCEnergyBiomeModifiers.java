package buildcraft.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Platform bridge which installs the BuildCraft oil placed feature into every biome.
 *
 * <p>The feature itself performs the authoritative API 2 world/dimension/biome rule check at placement time.
 * Installing it broadly here is what allows an addon to opt a custom dimension into oil generation through
 * {@code WorldgenService} without also having to patch loader-specific biome modifier data.</p>
 */
public final class BCEnergyBiomeModifiers {
    private static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, BCEnergy.MODID);

    public static final RegistryObject<Codec<OilFeatureInjectionModifier>> OIL_FEATURE_INJECTION =
        SERIALIZERS.register("oil_feature_injection", () -> RecordCodecBuilder.create(builder -> builder.group(
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(OilFeatureInjectionModifier::features)
        ).apply(builder, OilFeatureInjectionModifier::new)));

    private BCEnergyBiomeModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }

    public record OilFeatureInjectionModifier(HolderSet<PlacedFeature> features) implements BiomeModifier {
        @Override
        public void modify(Holder<Biome> biome, Phase phase, BiomeInfo.Builder builder) {
            if (phase != Phase.ADD) {
                return;
            }
            features.forEach(feature -> builder.getGenerationSettings().addFeature(Decoration.FLUID_SPRINGS, feature));
        }

        @Override
        public Codec<? extends BiomeModifier> codec() {
            return OIL_FEATURE_INJECTION.get();
        }
    }
}
