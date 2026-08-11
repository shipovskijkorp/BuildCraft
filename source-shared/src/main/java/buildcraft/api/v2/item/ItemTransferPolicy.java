package buildcraft.api.v2.item;

/** Whether a transfer may make partial progress or must satisfy the requested count in full. */
public enum ItemTransferPolicy {
    PARTIAL,
    ALL_OR_NOTHING
}
