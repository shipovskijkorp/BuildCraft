package buildcraft.lib.gui.statement;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.lib.gui.ISimpleDrawable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** An {@link IStatementParameter} that provides methods to draw itself. */
public interface IDrawingParameter extends IStatementParameter {
    @OnlyIn(Dist.CLIENT)
    ISimpleDrawable getDrawable();
}
