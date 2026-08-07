package buildcraft.robotics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import buildcraft.robotics.item.ItemRedstoneBoard;
import buildcraft.robotics.item.ItemRobot;
import buildcraft.robotics.item.ItemRobotStation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class BCRoboticsItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BCRobotics.MODID);

    public static final DeferredHolder<Item, ItemRobot> ROBOT = ITEMS.register("robot",
            () -> new ItemRobot(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, ItemRobotStation> ROBOT_STATION = ITEMS.register("robot_station",
            () -> new ItemRobotStation(new Item.Properties()));

    public static final DeferredHolder<Item, ItemRedstoneBoard> REDSTONE_BOARD = ITEMS.register("redstone_board",
            () -> new ItemRedstoneBoard(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, BlockItem> ZONE_PLANNER = ITEMS.register("zone_planner",
            () -> new BlockItem(BCRoboticsBlocks.ZONE_PLANNER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> REQUESTER = ITEMS.register("requester",
            () -> new BlockItem(BCRoboticsBlocks.REQUESTER.get(), new Item.Properties()));

    private BCRoboticsItems() {
    }

    public static void registry(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** All robot, board and station stacks displayed in the dedicated robotics tab. */
    public static Collection<ItemStack> getRoboticsTabItems() {
        List<ItemStack> stacks = new ArrayList<>();
        ROBOT.get().addCreativeTabItems(stacks::add);
        REDSTONE_BOARD.get().addCreativeTabItems(stacks::add);
        stacks.add(ROBOT_STATION.get().getDefaultInstance());
        return List.copyOf(stacks);
    }

    /** Robotics blocks that historically belong to BuildCraft's main tab. */
    public static Collection<ItemStack> getMainTabItems() {
        return List.of(
                ZONE_PLANNER.get().getDefaultInstance(),
                REQUESTER.get().getDefaultInstance()
        );
    }
}
