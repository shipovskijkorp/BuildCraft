package buildcraft.api.capabilities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Helpers for exposing BuildCraft block-entity capabilities through NeoForge. */
public final class BCCapabilityRegistration {
    private BCCapabilityRegistration() {
    }

    public static <T, BE extends BlockEntity & IBCCapabilityProvider> void registerBlockEntity(
        RegisterCapabilitiesEvent event,
        BlockCapability<T, Direction> capability,
        BlockEntityType<BE> blockEntityType
    ) {
        event.registerBlockEntity(
            capability,
            blockEntityType,
            (blockEntity, side) -> blockEntity.getCapability(capability, side)
        );
    }
}
