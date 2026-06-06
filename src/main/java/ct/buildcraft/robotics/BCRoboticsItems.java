package ct.buildcraft.robotics;

import ct.buildcraft.robotics.item.ItemRedstoneBoard;
import ct.buildcraft.robotics.item.ItemRobot;
import ct.buildcraft.robotics.item.ItemRobotStation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BCRoboticsItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BCRobotics.MODID);

    public static final RegistryObject<ItemRobot> ROBOT = ITEMS.register("robot",
            () -> new ItemRobot(new Item.Properties().tab(BCRobotics.TAB_ROBOTICS).stacksTo(1)));

    public static final RegistryObject<ItemRobotStation> ROBOT_STATION = ITEMS.register("robot_station",
            () -> new ItemRobotStation(new Item.Properties().tab(BCRobotics.TAB_ROBOTICS)));

    public static final RegistryObject<ItemRedstoneBoard> REDSTONE_BOARD = ITEMS.register("redstone_board",
            () -> new ItemRedstoneBoard(new Item.Properties().tab(BCRobotics.TAB_ROBOTICS).stacksTo(16)));

    public static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }
}
