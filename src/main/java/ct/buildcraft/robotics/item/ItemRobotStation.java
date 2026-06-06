package ct.buildcraft.robotics.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Base docking station item. The actual pipe pluggable/runtime station logic is intentionally left for the
 * next Robotics runtime port step.
 */
public class ItemRobotStation extends Item {
    public ItemRobotStation(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcraftrobotics.robot_station");
    }
}
