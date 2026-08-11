package buildcraft.lib.internal.api.v2.reload;

import buildcraft.api.v2.reload.*;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReloadableDefinitionRegistryTester {
    @Test
    public void failedReloadKeepsLastKnownGoodSnapshot() {
        ReloadableDefinitionRegistry<String> registry = new ReloadableDefinitionRegistry<>();

        ReloadTransaction<String> first = registry.beginReload();
        first.add(id("fuel"), "base", new DefinitionProvenance("base", "base.json", 0));
        assertTrue(first.resolve());
        assertTrue(first.publish());
        assertEquals("base", registry.current().get(id("fuel")).orElseThrow());

        ReloadTransaction<String> conflicting = registry.beginReload();
        conflicting.add(id("fuel"), "one", new DefinitionProvenance("addon-a", "a.json", 10));
        conflicting.add(id("fuel"), "two", new DefinitionProvenance("addon-b", "b.json", 10));

        assertFalse(conflicting.resolve());
        assertEquals(ReloadPhase.FAILED, conflicting.phase());
        assertEquals("base", registry.current().get(id("fuel")).orElseThrow());
    }

    @Test
    public void higherPriorityWinsAndKeepsOverrideChain() {
        ReloadableDefinitionRegistry<String> registry = new ReloadableDefinitionRegistry<>();
        ReloadTransaction<String> reload = registry.beginReload();
        reload.add(id("fuel"), "base", new DefinitionProvenance("buildcraft", "base.json", 0));
        reload.add(id("fuel"), "override", new DefinitionProvenance("datapack", "override.json", 20));

        assertTrue(reload.resolve());
        assertTrue(reload.publish());

        ResolvedDefinition<String> resolved = registry.current().resolved(id("fuel")).orElseThrow();
        assertEquals("override", resolved.value());
        assertEquals("datapack", resolved.provenance().owner());
        assertEquals(1, resolved.overridden().size());
        assertEquals("buildcraft", resolved.overridden().get(0).provenance().owner());
    }

    @Test
    public void staleTransactionCannotOverwriteNewerSnapshot() {
        ReloadableDefinitionRegistry<String> registry = new ReloadableDefinitionRegistry<>();
        ReloadTransaction<String> first = registry.beginReload();
        ReloadTransaction<String> stale = registry.beginReload();

        first.add(id("x"), "first", new DefinitionProvenance("a", "a.json", 0));
        stale.add(id("x"), "stale", new DefinitionProvenance("b", "b.json", 0));

        assertTrue(first.resolve());
        assertTrue(first.publish());
        assertTrue(stale.resolve());
        assertFalse(stale.publish());

        assertEquals(ReloadPhase.FAILED, stale.phase());
        assertEquals("first", registry.current().get(id("x")).orElseThrow());
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
