package buildcraft.api.v2.debug;

import java.util.Collection;

/** Runtime aggregation point replacing loader-specific IDebuggable/TilesAPI hooks. */
public interface DebugService {
    Collection<DebugSection> collect(DebugContext context);
}
