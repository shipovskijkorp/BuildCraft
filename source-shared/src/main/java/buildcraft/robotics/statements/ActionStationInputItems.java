package buildcraft.robotics.statements;

import buildcraft.lib.internal.statement.IActionInternal;
import buildcraft.lib.internal.statement.IStatementParameter;
import buildcraft.core.statements.BCStatement;
import buildcraft.core.statements.StatementParameterItemStackExact;

public abstract class ActionStationInputItems extends BCStatement implements IActionInternal {

    public ActionStationInputItems(String... tags) {
        super(tags);
    }

    @Override
    public int maxParameters() { return 3; }
    @Override
    public int minParameters() { return 1; }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStackExact();
    }
}
