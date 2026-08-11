package buildcraft.api.v2.robot;

@FunctionalInterface
public interface RobotEventListener {
    RobotEventDecision onRobotEvent(RobotEventContext context);
}
