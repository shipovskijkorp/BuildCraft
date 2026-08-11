package buildcraft.api.v2.permission;

import java.util.Optional;
import java.util.UUID;

/** Read-only ownership contract for machines, robots and other automated content. */
public interface OwnedView {
    Optional<UUID> ownerId();
    default boolean owned() { return ownerId().isPresent(); }
}
