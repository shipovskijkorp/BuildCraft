package ct.buildcraft.robotics.statements;

import ct.buildcraft.api.robots.DockingStation;
import ct.buildcraft.api.statements.IStatementContainer;
import ct.buildcraft.api.statements.IStatementParameter;
import ct.buildcraft.core.statements.StatementParameterItemStackExact;
import ct.buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;
import ct.buildcraft.robotics.BCRoboticsSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

    public static boolean canExtractItem(IStatementParameter[] parameters, ItemStack stack) {
        if (parameters == null) return false;
        for (IStatementParameter p : parameters) {
            if (p instanceof StatementParameterItemStackExact e) {
                ItemStack filter = e.getItemStack();
                if (!filter.isEmpty() && ItemStack.isSameItemSameTags(filter, stack)) return true;
            }
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public SpriteHolder getSprite() {
        return BCRoboticsSprites.ACTION_STATION_PROVIDE_ITEMS;
    }
}
