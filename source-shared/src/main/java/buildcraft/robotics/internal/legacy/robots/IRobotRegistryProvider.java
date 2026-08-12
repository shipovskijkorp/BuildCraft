package buildcraft.robotics.internal.legacy.robots;

import net.minecraft.world.level.Level;

public interface IRobotRegistryProvider {
    IRobotRegistry getRegistry(Level level);
}
