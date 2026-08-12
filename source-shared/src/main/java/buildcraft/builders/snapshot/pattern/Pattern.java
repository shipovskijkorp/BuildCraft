package buildcraft.builders.snapshot.pattern;

import buildcraft.builders.registry.FillerRegistry;
import buildcraft.builders.internal.filler.legacy.IFillerPattern;
import buildcraft.lib.internal.statement.IActionExternal;
import buildcraft.lib.internal.statement.IStatementContainer;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.lib.internal.statement.containers.IFillerStatementContainer;
import buildcraft.builders.BCBuildersStatements;
import buildcraft.core.statements.BCStatement;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class Pattern extends BCStatement implements IFillerPattern, IActionExternal {
    private final String desc;

    public Pattern(String tag) {
        super("buildcraft:" + tag);
        desc = "fillerpattern." + tag;
        FillerRegistry.INSTANCE.addPattern(this);
    }

    @Override
    public Component getDescription() {
        return Component.translatable(desc);
    }

    @Override
    public void actionActivate(BlockEntity target, Direction side, IStatementContainer source, IStatementParameter[] parameters) {
        if (source instanceof IFillerStatementContainer) {
            ((IFillerStatementContainer) source).setPattern(this, parameters);
        } else if (target instanceof IFillerStatementContainer) {
            ((IFillerStatementContainer) target).setPattern(this, parameters);
        }
    }

    @Override
    public IFillerPattern[] getPossible() {
        return BCBuildersStatements.PATTERNS;
    }
}
