package buildcraft.robotics.statements;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.StatementMouseClick;
import buildcraft.api.statements.StatementParameterItemStack;
import buildcraft.api.items.IMapLocation;

public class StatementParameterMapLocation extends StatementParameterItemStack {

    public static final String TAG = "buildcraft:maplocation";

    public StatementParameterMapLocation() { super(); }
    public StatementParameterMapLocation(ItemStack stack) { super(stack); }
    public StatementParameterMapLocation(CompoundTag nbt) { super(nbt); }

    @Override
    public String getUniqueTag() { return TAG; }

    @Override
    public StatementParameterMapLocation onClick(
            IStatementContainer source, IStatement stmt,
            ItemStack clicked, StatementMouseClick mouse) {
        if (clicked.isEmpty()) return new StatementParameterMapLocation();
        if (!(clicked.getItem() instanceof IMapLocation)) return this;
        ItemStack copy = clicked.copy();
        copy.setCount(1);
        return new StatementParameterMapLocation(copy);
    }

    public static StatementParameterMapLocation readFromNbt(CompoundTag nbt) {
        return new StatementParameterMapLocation(nbt);
    }
}
