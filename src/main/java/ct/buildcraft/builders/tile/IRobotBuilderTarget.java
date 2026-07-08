/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ct.buildcraft.builders.tile;

import java.util.List;

import ct.buildcraft.api.robots.EntityRobotBase;
import ct.buildcraft.builders.snapshot.BlueprintBuilder.RobotBuildTask;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Common target interface for BuildCraft 7 style Builder robots. */
public interface IRobotBuilderTarget {
    Level getLevel();

    BlockPos getBlockPos();

    boolean canRobotsBuild();

    List<RobotBuildTask> reserveRobotBuildTasks(EntityRobotBase robot, int maxItems);

    boolean buildRobotTask(EntityRobotBase robot, RobotBuildTask task);

    void releaseRobotBuildTask(EntityRobotBase robot, RobotBuildTask task);
}
