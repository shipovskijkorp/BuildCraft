package buildcraft.api.v2.permission;

/** Stable operation classes used by protection integrations. */
public enum WorldOperationKind {
    BLOCK_BREAK,
    BLOCK_PLACE,
    BLOCK_INTERACT,
    ENTITY_ATTACK,
    ENTITY_INTERACT,
    ITEM_USE,
    INVENTORY_ACCESS,
    FLUID_DRAIN,
    FLUID_PLACE,
    CHUNK_FORCE,
    OTHER
}
