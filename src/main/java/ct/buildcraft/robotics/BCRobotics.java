package ct.buildcraft.robotics;

import ct.buildcraft.api.BCModules;
import ct.buildcraft.lib.CreativeTabManager;
import ct.buildcraft.lib.CreativeTabManager.CreativeTabBC;
import ct.buildcraft.robotics.zone.MessageZoneMapRequest;
import ct.buildcraft.robotics.zone.MessageZoneMapResponse;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Minimal BuildCraft Robotics bootstrap for the 1.19.2 port.
 *
 * This intentionally ports only the base item layer: the robotics creative tab, robot item variants,
 * docking station item, redstone board variants, item models and vanilla crafting recipes.
 */
@Mod(BCRobotics.MODID)
public class BCRobotics {
    public static final String MODID = "buildcraftrobotics";

    /** Reuses the historical BuildCraft "boards" tab name, translated as "BuildCraft Robots". */
    public static final CreativeTabBC TAB_ROBOTICS = CreativeTabManager.createTab("buildcraft.boards");

    public BCRobotics() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::init);

        BCRoboticsBoards.init();
        BCRoboticsItems.registry(modEventBus);

        // Keep zone planner network messages available for the partially ported robotics zone code.
        ct.buildcraft.lib.net.MessageManager.registerMessageClass(BCModules.ROBOTICS, MessageZoneMapRequest.class,
                MessageZoneMapRequest.HANDLER, MessageZoneMapRequest::toBytes, MessageZoneMapRequest::new);
        ct.buildcraft.lib.net.MessageManager.registerMessageClass(BCModules.ROBOTICS, MessageZoneMapResponse.class,
                MessageZoneMapResponse.HANDLER, MessageZoneMapResponse::toBytes, MessageZoneMapResponse::new);

        // No config is registered yet; this keeps the module bootstrap deliberately small.
    }

    private void init(final FMLCommonSetupEvent event) {
        BCRoboticsBoards.init();
        TAB_ROBOTICS.setItem(BCRoboticsItems.ROBOT.get());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        private static final ResourceLocation ROBOT_MODEL = new ResourceLocation(MODID, "robot");
        private static final ResourceLocation BOARD_MODEL = new ResourceLocation(MODID, "board");

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemProperties.register(BCRoboticsItems.ROBOT.get(), ROBOT_MODEL,
                        (stack, level, entity, seed) -> BCRoboticsBoards.getRobotModelValue(stack));
                ItemProperties.register(BCRoboticsItems.REDSTONE_BOARD.get(), BOARD_MODEL,
                        (stack, level, entity, seed) -> BCRoboticsBoards.getBoardModelValue(stack));
            });
        }
    }
}
