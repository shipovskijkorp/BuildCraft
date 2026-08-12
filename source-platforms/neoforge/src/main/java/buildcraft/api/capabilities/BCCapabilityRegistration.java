package buildcraft.api.capabilities;

import buildcraft.lib.internal.mj.MjCapabilities;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
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
        // Register the standard FE capability alongside the MJ receiver. MjCapabilityHelper
        // only returns it when powerMode enables automatic conversion.
        if (capability == MjCapabilities.CAP_RECEIVER) {
            event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                blockEntityType,
                (blockEntity, side) -> blockEntity.getCapability(Capabilities.EnergyStorage.BLOCK, side)
            );
        }
    }
}
