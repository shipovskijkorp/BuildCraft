package dev.bcce.apifixture;

import buildcraft.api.v2.content.BuildCraftContent;
import buildcraft.api.v2.content.ContentRegistrar;
import buildcraft.api.v2.persistence.ApiCodec;
import buildcraft.api.v2.persistence.CodecResult;
import buildcraft.api.v2.persistence.OpaqueData;
import buildcraft.api.v2.persistence.PersistentType;
import buildcraft.api.v2.schematic.InventoryCopyPolicy;
import buildcraft.api.v2.schematic.SchematicAdapter;
import buildcraft.api.v2.schematic.SchematicCaptureContext;
import buildcraft.api.v2.schematic.SchematicPlacementContext;
import buildcraft.api.v2.schematic.SchematicResult;
import buildcraft.api.v2.schematic.SnapshotElement;
import buildcraft.api.v2.schematic.SnapshotElementType;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Compile-only acceptance example for addon-owned Builder/schematic content. */
public final class SchematicFixtureAddon {
    private static final ResourceLocation ELEMENT_ID = id("moonbuildcraft:moon_machine_snapshot");
    private static final ResourceLocation FORMAT_ID = id("moonbuildcraft:utf8");

    private SchematicFixtureAddon() {}

    public static void register() {
        ContentRegistrar bc = BuildCraftContent.addon("moonbuildcraft");

        PersistentType<MoonMachineSnapshot, OpaqueData> persistence = PersistentType
            .builder(ELEMENT_ID, 0, new ApiCodec<>() {
                @Override
                public CodecResult<MoonMachineSnapshot> decode(OpaqueData payload) {
                    return CodecResult.success(new MoonMachineSnapshot(new String(payload.bytes(), StandardCharsets.UTF_8)));
                }

                @Override
                public CodecResult<OpaqueData> encode(MoonMachineSnapshot value) {
                    return CodecResult.success(new OpaqueData(FORMAT_ID, value.variant().getBytes(StandardCharsets.UTF_8)));
                }
            })
            .build();

        bc.blockSchematic("moon_machine_snapshot", new SnapshotElementType<>(ELEMENT_ID, persistence), new SchematicAdapter() {
            @Override public int priority() { return 500; }
            @Override public boolean supports(SchematicCaptureContext context) { return !context.state().isAir(); }
            @Override public Optional<? extends SnapshotElement> capture(SchematicCaptureContext context) {
                return Optional.of(new MoonMachineSnapshot("mk2"));
            }
            @Override public boolean supportsElement(SnapshotElement element) { return element instanceof MoonMachineSnapshot; }
            @Override public SchematicResult place(SnapshotElement element, SchematicPlacementContext context) {
                // A real addon would perform its placement through its own stable block/item integration here.
                return new SchematicResult(SchematicResult.Status.SUCCESS, context.mode().isSimulation() ? "simulated" : "placed");
            }
        });

        // Hidden inventories not exposed through loader item-storage APIs can opt in without touching Builder internals.
        bc.inventoryCopyPolicy(InventoryCopyPolicy
            .forBlock(bc.id("moon_machine_inventory"), id("moonbuildcraft:moon_machine"))
            .allow("Items")
            .build());
    }

    private record MoonMachineSnapshot(String variant) implements SnapshotElement {
        @Override public ResourceLocation typeId() { return ELEMENT_ID; }
    }

    private static ResourceLocation id(String value) {
        return Objects.requireNonNull(ResourceLocation.tryParse(value));
    }
}
