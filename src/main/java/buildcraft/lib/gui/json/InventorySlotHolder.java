package buildcraft.lib.gui.json;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class InventorySlotHolder {

    public final Slot[] slots;

    public InventorySlotHolder(AbstractContainerMenu inv, Container container) {
        List<Slot> list = new ArrayList<>();
        for (Slot s : inv.slots) {
            if (s.container == container) {
                list.add(s);
            }
        }
        slots = list.toArray(new Slot[0]);
    }

    public InventorySlotHolder(AbstractContainerMenu container, IItemHandler inventory) {
        List<Slot> list = new ArrayList<>();
        for (Slot slot : container.slots) {
            if (slot instanceof SlotItemHandler itemSlot && itemSlot.getItemHandler() == inventory) {
                list.add(slot);
            }
        }
        slots = list.toArray(new Slot[0]);
    }
}
