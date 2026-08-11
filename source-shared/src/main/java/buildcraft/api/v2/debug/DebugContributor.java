package buildcraft.api.v2.debug;

import java.util.Collection;

@FunctionalInterface
public interface DebugContributor {
    Collection<DebugSection> collect(DebugContext context);
}
