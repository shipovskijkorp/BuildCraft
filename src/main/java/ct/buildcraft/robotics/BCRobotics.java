package ct.buildcraft.robotics;

import ct.buildcraft.api.BCModules;
import ct.buildcraft.api.statements.StatementManager;
import ct.buildcraft.robotics.BCRoboticsStatements;
import ct.buildcraft.robotics.BCRoboticsSprites;
import ct.buildcraft.robotics.statements.RobotsActionProvider;
import ct.buildcraft.robotics.statements.RobotsTriggerProvider;
import ct.buildcraft.robotics.statements.StatementParameterRobot;
import ct.buildcraft.robotics.statements.StatementParameterMapLocation;
import ct.buildcraft.lib.CreativeTabManager;
import ct.buildcraft.lib.CreativeTabManager.CreativeTabBC;
import ct.buildcraft.robotics.zone.MessageZoneMapRequest;
import ct.buildcraft.robotics.ai.AIRobotBreak;
import ct.buildcraft.robotics.ai.AIRobotFetchAndEquipItemStack;
import ct.buildcraft.robotics.ai.AIRobotFetchItem;
import ct.buildcraft.robotics.ai.AIRobotGotoBlock;
import ct.buildcraft.robotics.ai.AIRobotGotoSleep;
import ct.buildcraft.robotics.ai.AIRobotGotoStation;
import ct.buildcraft.robotics.ai.AIRobotGotoStationAndUnload;
import ct.buildcraft.robotics.ai.AIRobotGotoStationToUnload;
import ct.buildcraft.robotics.ai.AIRobotMain;
import ct.buildcraft.robotics.ai.AIRobotRecharge;
import ct.buildcraft.robotics.ai.AIRobotSearchAndGotoBlock;
import ct.buildcraft.robotics.ai.AIRobotSearchAndGotoStation;
import ct.buildcraft.robotics.ai.AIRobotSearchBlock;
import ct.buildcraft.robotics.ai.AIRobotSearchStation;
import ct.buildcraft.robotics.ai.AIRobotShutdown;
import ct.buildcraft.robotics.ai.AIRobotSleep;
import ct.buildcraft.robotics.ai.AIRobotStraightMoveTo;
import ct.buildcraft.robotics.ai.AIRobotUnload;
import ct.buildcraft.robotics.boards.BoardRobotLumberjack;
import ct.buildcraft.robotics.boards.BoardRobotPicker;
import ct.buildcraft.api.robots.RobotManager;
import ct.buildcraft.robotics.client.render.RenderRobot;
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
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent.BakingCompleted;
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
        modEventBus.addListener(BCRobotics::registerEntityAttributes);

        BCRoboticsBoards.init();
        BCRoboticsPlugs.preInit();
        BCRoboticsItems.registry(modEventBus);
        BCRoboticsEntities.registry(modEventBus);

        // Keep zone planner network messages available for the partially ported robotics zone code.
        ct.buildcraft.lib.net.MessageManager.registerMessageClass(BCModules.ROBOTICS, MessageZoneMapRequest.class,
                MessageZoneMapRequest.HANDLER, MessageZoneMapRequest::toBytes, MessageZoneMapRequest::new);
        ct.buildcraft.lib.net.MessageManager.registerMessageClass(BCModules.ROBOTICS, MessageZoneMapResponse.class,
                MessageZoneMapResponse.HANDLER, MessageZoneMapResponse::toBytes, MessageZoneMapResponse::new);

        RobotManager.registryProvider = SimpleRobotRegistryProvider.INSTANCE;
        RobotManager.registerDockingStation(DockingStationPipe.class, "pipe");
        registerRoboticsAI();
        BoardRobotPicker.onServerStart();

        // No config is registered yet; this keeps the module bootstrap deliberately small.
    }

    private void init(final FMLCommonSetupEvent event) {
        // Register robot statement providers and parameter types
        BCRoboticsStatements.preInit();
        BCRoboticsSprites.preInit();
        StatementManager.registerActionProvider(new RobotsActionProvider());
        StatementManager.registerTriggerProvider(new RobotsTriggerProvider());
        StatementManager.registerParameter(StatementParameterRobot.TAG, StatementParameterRobot::readFromNbt);
        StatementManager.registerParameter(StatementParameterMapLocation.TAG, StatementParameterMapLocation::readFromNbt);

        BCRoboticsBoards.init();
        BCRoboticsPlugs.preInit();
        RobotManager.registryProvider = SimpleRobotRegistryProvider.INSTANCE;
        TAB_ROBOTICS.setItem(BCRoboticsItems.ROBOT.get());
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(BCRoboticsEntities.ROBOT.get(), ct.buildcraft.robotics.entity.EntityRobot.createAttributes().build());
    }


    private static void registerRoboticsAI() {
        if (RobotManager.getAIRobotName(AIRobotMain.class) != null) {
            return;
        }
        RobotManager.registerAIRobot(AIRobotMain.class, "main", "buildcraft.robotics.ai.AIRobotMain");
        RobotManager.registerAIRobot(BoardRobotPicker.class, "boardPicker", "buildcraft.robotics.boards.BoardRobotPicker");
        RobotManager.registerAIRobot(BoardRobotLumberjack.class, "boardLumberjack", "buildcraft.robotics.boards.BoardRobotLumberjack");
        RobotManager.registerAIRobot(AIRobotFetchItem.class, "fetchItem", "buildcraft.robotics.ai.AIRobotFetchItem");
        RobotManager.registerAIRobot(AIRobotFetchAndEquipItemStack.class, "fetchAndEquipItemStack", "buildcraft.robotics.ai.AIRobotFetchAndEquipItemStack");
        RobotManager.registerAIRobot(AIRobotSearchBlock.class, "searchBlock", "buildcraft.robotics.ai.AIRobotSearchBlock");
        RobotManager.registerAIRobot(AIRobotSearchAndGotoBlock.class, "searchAndGotoBlock", "buildcraft.robotics.ai.AIRobotSearchAndGotoBlock");
        RobotManager.registerAIRobot(AIRobotBreak.class, "break", "buildcraft.robotics.ai.AIRobotBreak");
        RobotManager.registerAIRobot(AIRobotGotoBlock.class, "gotoBlock", "buildcraft.robotics.ai.AIRobotGotoBlock");
        RobotManager.registerAIRobot(AIRobotStraightMoveTo.class, "straightMoveTo", "buildcraft.robotics.ai.AIRobotStraightMoveTo");
        RobotManager.registerAIRobot(AIRobotGotoStation.class, "gotoStation", "buildcraft.robotics.ai.AIRobotGotoStation");
        RobotManager.registerAIRobot(AIRobotGotoStationToUnload.class, "gotoStationToUnload", "buildcraft.robotics.ai.AIRobotGotoStationToUnload");
        RobotManager.registerAIRobot(AIRobotGotoStationAndUnload.class, "gotoStationAndUnload", "buildcraft.robotics.ai.AIRobotGotoStationAndUnload");
        RobotManager.registerAIRobot(AIRobotSearchStation.class, "searchStation", "buildcraft.robotics.ai.AIRobotSearchStation");
        RobotManager.registerAIRobot(AIRobotSearchAndGotoStation.class, "searchAndGotoStation", "buildcraft.robotics.ai.AIRobotSearchAndGotoStation");
        RobotManager.registerAIRobot(AIRobotUnload.class, "unload", "buildcraft.robotics.ai.AIRobotUnload");
        RobotManager.registerAIRobot(AIRobotGotoSleep.class, "gotoSleep", "buildcraft.robotics.ai.AIRobotGotoSleep");
        RobotManager.registerAIRobot(AIRobotSleep.class, "sleep", "buildcraft.robotics.ai.AIRobotSleep");
        RobotManager.registerAIRobot(AIRobotRecharge.class, "recharge", "buildcraft.robotics.ai.AIRobotRecharge");
        RobotManager.registerAIRobot(AIRobotShutdown.class, "shutdown", "buildcraft.robotics.ai.AIRobotShutdown");
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
                BCRoboticsModels.init();
            });
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(BCRoboticsEntities.ROBOT.get(), RenderRobot::new);
        }


        @SubscribeEvent
        public static void onModelBake(BakingCompleted event) {
            BCRoboticsModels.onModelBake(event);
        }
    }
}
