package buildcraft.energy.menu;

import buildcraft.energy.BCEnergyGuis;
import buildcraft.energy.tile.TileEngineFE;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** Menu for the BuildCraft FE Engine. */
public class ContainerEngineFE extends ContainerBCTile<TileEngineFE> {
    public final IItemHandlerAdv upgrades;
    public ContainerEngineFE(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemHandlerSimple(4), createLevelAccess(playerInventory, buf));
    }

    public ContainerEngineFE(int containerId, Inventory playerInventory, IItemHandlerAdv upgrades, ContainerLevelAccess access) {
        super(BCEnergyGuis.MENU_FE.get(), playerInventory, containerId, access);
        this.upgrades = upgrades;
        addFullPlayerInventory(95);
        for (int slot = 0; slot < 4; slot++) {
            addSlot(new SlotBase(upgrades, slot, 62 + 18 * slot, 44));
        }
    }
}
