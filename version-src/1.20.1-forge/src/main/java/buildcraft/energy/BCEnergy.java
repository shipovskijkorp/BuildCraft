package buildcraft.energy;

import buildcraft.core.BCCore;
import buildcraft.energy.tile.TileSpringOil;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.FluidUtilBC;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(BCEnergy.MODID)
public class BCEnergy {
    public static final String MODID = "buildcraftenergy";

    private static final ResourceLocation ADVANCEMENT_FIND_OIL_SPOT = new ResourceLocation(MODID, "fine_riches");
    private static final int OIL_SPOT_CHECK_INTERVAL = 40;
    private static final int OIL_SPOT_CHECK_RADIUS_XZ = 12;
    private static final int OIL_SPOT_CHECK_RADIUS_Y = 8;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public BCEnergy() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(BCEnergyConfig::onLoadConfig);
        modEventBus.addListener(BCEnergyConfig::onReloadConfig);
        modEventBus.addListener(this::gatherData);

        BCEnergyFluids.registry(modEventBus);
        BCEnergyBlocks.init(modEventBus);
        BCEnergyGuis.init();
        BCEnergyWorldGen.preInit(modEventBus);
        BCEnergyConfig.preInit();

        BCCore.BUILDCRAFT_TAB.addItemProvider(BCEnergyBlocks::getCreativeTabItems);
        BCCore.tabFluids.addItemProvider(BCEnergyFluids::getCreativeTabItems);

        ModLoadingContext.get().registerConfig(Type.COMMON, BCEnergyConfig.config);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        BCEnergyFluids.init();
        BCCore.tabFluids.setItem(BCEnergyFluids.OIL_BUCKET.get(0).get());
        BCEnergyRecipes.init();
        BCEnergyConfig.reloadConfig(MODID);
    }

    public void gatherData(GatherDataEvent event) {
        var output = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(event.includeServer(), new BCEnergyRecipes.BCEnergyRecipeProvider(output));
        event.getGenerator().addProvider(event.includeClient(), new BCEnergyProvider.BlockModel(output, event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new BCEnergyProvider.BlockState(output, event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new BCEnergyProvider.ItemModel(output, event.getExistingFileHelper()));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % OIL_SPOT_CHECK_INTERVAL != 0) {
            return;
        }
        if (hasAdvancement(player, ADVANCEMENT_FIND_OIL_SPOT)) {
            return;
        }
        if (isNearOilSpot(player)) {
            AdvancementUtil.unlockAdvancement(player, ADVANCEMENT_FIND_OIL_SPOT);
        }
    }

    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancementName) {
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(advancementName);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static boolean isNearOilSpot(ServerPlayer player) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = -OIL_SPOT_CHECK_RADIUS_Y; y <= OIL_SPOT_CHECK_RADIUS_Y; y++) {
            for (int x = -OIL_SPOT_CHECK_RADIUS_XZ; x <= OIL_SPOT_CHECK_RADIUS_XZ; x++) {
                for (int z = -OIL_SPOT_CHECK_RADIUS_XZ; z <= OIL_SPOT_CHECK_RADIUS_XZ; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (isOilSpotBlock(level, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isOilSpotBlock(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        Fluid fluid = level.getFluidState(pos).getType();
        if (fluid != Fluids.EMPTY && BCEnergyFluids.crudeOil[0] != null
            && FluidUtilBC.areFluidsEqual(fluid, BCEnergyFluids.crudeOil[0])) {
            return true;
        }
        if (level.getBlockState(pos).hasBlockEntity()) {
            BlockEntity tile = level.getBlockEntity(pos);
            return tile instanceof TileSpringOil;
        }
        return false;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
