package buildcraft.robotics.statements;

import buildcraft.api.statements.IActionInternal;
import buildcraft.api.statements.IStatementParameter;
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
