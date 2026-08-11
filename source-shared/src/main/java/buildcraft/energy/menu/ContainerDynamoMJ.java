package buildcraft.energy.menu;

import buildcraft.energy.BCEnergyGuis;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;

/** Menu for the BuildCraft MJ Dynamo. */
public class ContainerDynamoMJ extends ContainerBCTile<TileDynamoMJ> {
    public final IItemHandlerAdv upgrades;
    public ContainerDynamoMJ(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, new ItemHandlerSimple(4), createLevelAccess(playerInventory, buf));
    }

    public ContainerDynamoMJ(int containerId, Inventory playerInventory, IItemHandlerAdv upgrades, ContainerLevelAccess access) {
        super(BCEnergyGuis.MENU_DYNAMO_MJ.get(), playerInventory, containerId, access);
        this.upgrades = upgrades;
        addFullPlayerInventory(95);
        for (int slot = 0; slot < 4; slot++) {
            addSlot(new SlotBase(upgrades, slot, 44 + 18 * slot, 44));
        }
    }
}
