package buildcraft.api.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.api.v2.energy.MjAmount;
import buildcraft.api.v2.pipe.ItemTransportProfile;
import buildcraft.api.v2.pipe.PipeMedium;
import buildcraft.api.v2.pipe.PipeType;
import buildcraft.api.v2.registry.ApiRegistry;
import buildcraft.api.v2.registry.RegistryKey;
import buildcraft.api.v2.registry.SimpleApiRegistry;
import buildcraft.api.v2.robot.RobotEventDecision;
import buildcraft.api.v2.testkit.TestApiRuntime;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

public class ApiV2CompleteSurfaceTester {
    @Test
    void registryAliasesPreserveCanonicalIdentityAndFreeze() {
        SimpleApiRegistry<String> registry = new SimpleApiRegistry<>();
        ResourceLocation canonical = id("canonical");
        ResourceLocation alias = id("legacy_name");
        registry.register(canonical, "value", () -> "fixture");
        registry.registerAlias(alias, canonical, () -> "fixture");

        assertEquals(canonical, registry.canonicalId(alias));
        assertEquals("value", registry.get(alias));
        assertEquals("fixture", registry.entry(alias).orElseThrow().owner());

        registry.freeze();
        assertThrows(IllegalStateException.class, () -> registry.register(id("late"), "late"));
    }

    @Test
    void pipeTypeIsCompositionBasedAndValidated() {
        PipeType type = PipeType.builder(id("brass"))
            .itemProfile(new ItemTransportProfile(16, 4))
            .component(id("filter"))
            .build();

        assertTrue(type.media().contains(PipeMedium.ITEM));
        assertTrue(type.defaultComponents().contains(id("filter")));
        assertTrue(type.itemProfile().isPresent());
        assertThrows(IllegalStateException.class, () -> PipeType.builder(id("empty")).build());
    }

    @Test
    void featureNegotiationUsesMinimumLevels() {
        ApiFeatureSet features = new ImmutableApiFeatureSet(List.of(new ApiFeature(id("pipes"), 2)));
        assertTrue(features.supports(id("pipes")));
        assertTrue(features.supports(id("pipes"), 2));
        assertFalse(features.supports(id("pipes"), 3));
    }

    @Test
    void testRuntimeExposesTypedRegistriesWithoutImplementationCode() {
        TestApiRuntime runtime = new TestApiRuntime(BuildCraftApi.VERSION, ApiFeatureSet.EMPTY);
        RegistryKey<MjAmount> key = new RegistryKey<>(id("amounts"));
        ApiRegistry<MjAmount> registry = runtime.addRegistry(key);
        MjAmount value = MjAmount.ofMj(4);
        registry.register(id("four"), value);
        assertSame(value, runtime.requireRegistry(key).get(id("four")));
    }

    @Test
    void robotEventDecisionAggregationIsDeterministic() {
        assertEquals(RobotEventDecision.PASS, RobotEventDecision.PASS.merge(RobotEventDecision.PASS));
        assertEquals(RobotEventDecision.ALLOW, RobotEventDecision.PASS.merge(RobotEventDecision.ALLOW));
        assertEquals(RobotEventDecision.ALLOW, RobotEventDecision.ALLOW.merge(RobotEventDecision.PASS));
        assertEquals(RobotEventDecision.DENY, RobotEventDecision.ALLOW.merge(RobotEventDecision.DENY));
        assertTrue(RobotEventDecision.DENY.isTerminal());
        assertFalse(RobotEventDecision.ALLOW.isTerminal());
    }

    private static ResourceLocation id(String path) {
        return java.util.Objects.requireNonNull(ResourceLocation.tryParse("api_v2_test:" + path));
    }
}
