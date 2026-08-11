package buildcraft.api.mj;

/** Runtime status used by the legacy MJ API to expose FE conversion without depending on a loader config class. */
public interface IMjToFeStatus {
    MjFeConversion getConversion();
    boolean isAutoconvertEnabled();
}
