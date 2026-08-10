/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License.
 */
package buildcraft.api.events;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

import buildcraft.api.robots.EntityRobotBase;

public abstract class RobotEvent extends Event {
    public final EntityRobotBase robot;

    public RobotEvent(EntityRobotBase robot) {
        this.robot = robot;
    }

    public static class Place extends RobotEvent implements ICancellableEvent {
        public final Player player;

        public Place(EntityRobotBase robot, Player player) {
            super(robot);
            this.player = player;
        }
    }

    public static class Interact extends RobotEvent implements ICancellableEvent {
        public final Player player;
        public final ItemStack item;

        public Interact(EntityRobotBase robot, Player player, ItemStack item) {
            super(robot);
            this.player = player;
            this.item = item;
        }
    }

    public static class Dismantle extends RobotEvent implements ICancellableEvent {
        public final Player player;

        public Dismantle(EntityRobotBase robot, Player player) {
            super(robot);
            this.player = player;
        }
    }
}
