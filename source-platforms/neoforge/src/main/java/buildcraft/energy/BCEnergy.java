package buildcraft.energy;

import buildcraft.lib.internal.mj.MjCapabilities;

import buildcraft.api.capabilities.BCCapabilityRegistration;
import buildcraft.api.capabilities.IBCCapabilityProvider;
import buildcraft.core.BCCore;
import buildcraft.energy.tile.TileSpringOil;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.FluidUtilBC;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BCEnergy.MODID)
public class BCEnergy {
    public static final String MODID = "buildcraftenergy";

    private static final ResourceLocation ADVANCEMENT_FIND_OIL_SPOT =
        ResourceLocation.fromNamespaceAndPath(MODID, "fine_riches");
    private static final int OIL_SPOT_CHECK_INTERVAL = 40;
    private static final int OIL_SPOT_CHECK_RADIUS_XZ = 12;
    private static final int OIL_SPOT_CHECK_RADIUS_Y = 8;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public BCEnergy(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(BCEnergyConfig::onLoadConfig);
        modEventBus.addListener(BCEnergyConfig::onReloadConfig);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::registerCapabilities);

        BCEnergyFluids.registry(modEventBus);
        BCEnergyBlocks.init(modEventBus);
        BCEnergyGuis.init();
        BCEnergyWorldGen.preInit(modEventBus);
        BCEnergyConfig.preInit();

        BCCore.BUILDCRAFT_TAB.addItemProvider(BCEnergyBlocks::getCreativeTabItems);
        BCCore.tabFluids.addItemProvider(BCEnergyFluids::getCreativeTabItems);

        modContainer.registerConfig(Type.COMMON, BCEnergyConfig.config);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        BCEnergyFluids.init();
        BCCore.tabFluids.setItem(BCEnergyFluids.OIL_BUCKET.get(0).get());
        BCEnergyRecipes.init();
        BCEnergyConfig.reloadConfig(MODID);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerEngineCapabilities(event, BCEnergyBlocks.ENGINE_STONE_TILE_BC8.get());
        registerEngineCapabilities(event, BCEnergyBlocks.ENGINE_IRON_TILE_BC8.get());
        registerEngineCapabilities(event, BCEnergyBlocks.ENGINE_FE_TILE_BC8.get());
        registerEngineCapabilities(event, BCEnergyBlocks.DYNAMO_MJ_TILE.get());

        BCCapabilityRegistration.registerBlockEntity(
            event, Capabilities.ItemHandler.BLOCK, BCEnergyBlocks.ENGINE_STONE_TILE_BC8.get()
        );
        BCCapabilityRegistration.registerBlockEntity(
            event, Capabilities.FluidHandler.BLOCK, BCEnergyBlocks.ENGINE_IRON_TILE_BC8.get()
        );
    }

    private static <BE extends BlockEntity & IBCCapabilityProvider> void registerEngineCapabilities(
        RegisterCapabilitiesEvent event, BlockEntityType<BE> blockEntityType
    ) {
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_CONNECTOR, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_RECEIVER, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_REDSTONE_RECEIVER, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_READABLE, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_PASSIVE_PROVIDER, blockEntityType);
    }

    public void gatherData(GatherDataEvent event) {
        var output = event.getGenerator().getPackOutput();
        event.getGenerator().addProvider(
            event.includeServer(),
            new BCEnergyRecipes.BCEnergyRecipeProvider(output, event.getLookupProvider())
        );
        event.getGenerator().addProvider(
            event.includeClient(), new BCEnergyProvider.BlockModel(output, event.getExistingFileHelper())
        );
        event.getGenerator().addProvider(
            event.includeClient(), new BCEnergyProvider.BlockState(output, event.getExistingFileHelper())
        );
        event.getGenerator().addProvider(
            event.includeClient(), new BCEnergyProvider.ItemModel(output, event.getExistingFileHelper())
        );
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
        AdvancementHolder advancement = player.getServer().getAdvancements().get(advancementName);
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
}
