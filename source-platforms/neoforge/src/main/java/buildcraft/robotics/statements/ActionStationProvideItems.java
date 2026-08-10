package buildcraft.robotics.statements;

import buildcraft.api.robots.DockingStation;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementSlot;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.core.render.ISprite;
import buildcraft.robotics.BCRoboticsSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ActionStationProvideItems extends ActionStationInputItems {

    public ActionStationProvideItems() {
        super("buildcraft:station.provide_items");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("gate.action.station.provideItems");
    }

    @Override
    public void actionActivate(IStatementContainer container, IStatementParameter[] parameters) {}

    public static boolean canExtractItem(DockingStation station, ItemStack stack) {
        if (station == null || stack.isEmpty()) {
            return false;
        }

        for (StatementSlot slot : station.getActiveActions()) {
            IStatement statement = slot.statement;
            if (statement instanceof ActionStationProvideItems && canExtractItem(slot.parameters, stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canExtractItem(IStatementParameter[] parameters, ItemStack stack) {
        if (parameters == null || parameters.length == 0) return true;

        boolean hasFilter = false;
        for (IStatementParameter p : parameters) {
            if (p == null) continue;
            ItemStack filter = p.getItemStack();
            if (!filter.isEmpty()) {
                hasFilter = true;
                if (ItemStack.isSameItemSameComponents(filter, stack)) return true;
            }
        }
        return !hasFilter;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ISprite getSprite() {
        return BCRoboticsSprites.ACTION_STATION_PROVIDE_ITEMS;
    }
}
