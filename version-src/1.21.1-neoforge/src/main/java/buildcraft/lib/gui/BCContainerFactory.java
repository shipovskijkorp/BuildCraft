package buildcraft.lib.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;

public class BCContainerFactory<T extends MenuBC_Neptune> implements IContainerFactory<T> {
    final BCMenuSupplier<T> constructor;

    public BCContainerFactory(BCMenuSupplier<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        return constructor.create(windowId, inv, data);
    }

    public interface BCMenuSupplier<T extends MenuBC_Neptune> {
        T create(int windowId, Inventory inv, FriendlyByteBuf data);
    }

    public static <T extends MenuBC_Neptune> MenuType<T> create(BCMenuSupplier<T> constructor) {
        return IMenuTypeExtension.create(new BCContainerFactory<>(constructor));
    }
}
