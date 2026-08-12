package buildcraft.lib.gui.statement;

import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.lib.gui.ISimpleDrawable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** An {@link IStatementParameter} that provides methods to draw itself. */
public interface IDrawingParameter extends IStatementParameter {
    @OnlyIn(Dist.CLIENT)
    ISimpleDrawable getDrawable();
}
