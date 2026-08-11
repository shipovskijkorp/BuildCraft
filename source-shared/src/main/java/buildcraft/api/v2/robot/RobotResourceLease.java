package buildcraft.api.v2.robot;

public interface RobotResourceLease extends AutoCloseable {
    long robotId();
    RobotResource resource();
    long amount();
    boolean active();
    @Override void close();
}
