package buildcraft.api.v2.permission;

import buildcraft.api.v2.OperationMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PermissionServiceTester {
    @Test
    public void denyIsTerminalAndStopsLaterProviders() {
        AtomicInteger laterCalls = new AtomicInteger();
        PermissionService service = PermissionServices.compose(List.of(
            context -> PermissionDecision.allow(id("first"), "preliminary allow"),
            context -> PermissionDecision.deny(id("protection"), "claimed chunk"),
            context -> {
                laterCalls.incrementAndGet();
                return PermissionDecision.allow(id("late"), "must not run");
            }
        ));

        PermissionDecision result = service.decide(context(OperationMode.EXECUTE));
        assertEquals(PermissionVerdict.DENY, result.verdict());
        assertEquals(id("protection"), result.authority().orElseThrow());
        assertEquals(0, laterCalls.get());
    }

    @Test
    public void allowWinsOverPassWithoutBecomingTerminal() {
        AtomicInteger calls = new AtomicInteger();
        PermissionService service = PermissionServices.compose(List.of(
            context -> PermissionDecision.pass(),
            context -> PermissionDecision.allow(id("claims"), "owner"),
            context -> {
                calls.incrementAndGet();
                return PermissionDecision.pass();
            }
        ));
        assertEquals(PermissionVerdict.ALLOW, service.decide(context(OperationMode.SIMULATE)).verdict());
        assertEquals(1, calls.get());
    }

    @Test
    public void actorAndContextPreserveSimulationIdentity() {
        AutomationActor actor = AutomationActor.machineOwner(
            java.util.UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"),
            "Builder",
            id("builder")
        );
        WorldOperationContext context = context(OperationMode.SIMULATE, actor);
        assertTrue(context.isSimulation());
        assertTrue(context.actor().representsPlayer());
        assertEquals(ActorType.MACHINE_OWNER, context.actor().type());
        assertEquals(id("test_dimension"), context.dimensionId());
        assertFalse(context.target().blockPos().isEmpty());
    }

    @Test
    public void runtimeRegistryOrdersProvidersAndRejectsDuplicateIds() {
        buildcraft.lib.api.v2.PermissionServiceRegistryImpl registry =
            new buildcraft.lib.api.v2.PermissionServiceRegistryImpl();
        java.util.ArrayList<String> calls = new java.util.ArrayList<>();
        registry.register(id("low"), 0, context -> {
            calls.add("low");
            return PermissionDecision.pass();
        });
        registry.register(id("high"), 100, context -> {
            calls.add("high");
            return PermissionDecision.allow(id("high"), "high priority allow");
        });
        assertEquals(PermissionVerdict.ALLOW, registry.decide(context(OperationMode.EXECUTE)).verdict());
        assertEquals(java.util.List.of("high", "low"), calls);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
            registry.register(id("low"), 1, PermissionService.passThrough())
        );
    }

    private static WorldOperationContext context(OperationMode mode) {
        return context(mode, AutomationActor.system(id("test_system")));
    }

    private static WorldOperationContext context(OperationMode mode, AutomationActor actor) {
        return WorldOperationContext.detachedForTesting(
            actor,
            id("test_dimension"),
            BlockPos.ZERO,
            WorldOperationTarget.block(new BlockPos(1, 2, 3)),
            WorldOperationKind.BLOCK_BREAK,
            mode,
            id("test_reason")
        );
    }

    private static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryParse("test:" + path));
    }
}
