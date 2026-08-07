package buildcraft.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buildcraft.api.BCModules;
import buildcraft.api.enums.EnumSpring;
import buildcraft.api.capabilities.BCCapabilityRegistration;
import buildcraft.api.capabilities.IBCCapabilityProvider;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.items.FluidItemDrops;
import buildcraft.core.client.RenderTickListener;
import buildcraft.core.item.ItemFragileFluidContainer;
import buildcraft.core.client.model.FragileFluidContainerModel;
import buildcraft.core.client.model.ModelEngine;
import buildcraft.core.client.render.RenderEngine_BC8;
import buildcraft.core.client.render.RenderMarkerVolume;
import buildcraft.core.client.render.RenderVolumeBoxes;
import buildcraft.core.list.ContainerList;
import buildcraft.core.list.GuiList;
import buildcraft.core.marker.PathCache;
import buildcraft.core.marker.VolumeCache;
import buildcraft.core.marker.volume.MessageVolumeBoxes;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.tile.TileSpringOil;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.CreativeTabManager.CreativeTabBC;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;
import buildcraft.lib.gui.BCContainerFactory;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.net.MessageManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(BCCore.MODID)
public class BCCore {
	public static final String MODID = "buildcraftcore";
    public static final CreativeTabBC BUILDCRAFT_TAB = CreativeTabManager.createTab("buildcraft.main");
    public static final CreativeTabBC tabFluids = CreativeTabManager.createTab("buildcraft.fluid");

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "buildcraft");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.main"))
            .icon(BUILDCRAFT_TAB::makeIcon)
            .displayItems((parameters, output) -> BUILDCRAFT_TAB.accept(List.of(), output::accept))
            .build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLUID_TAB = CREATIVE_TABS.register("fluid", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.fluid"))
            .icon(tabFluids::makeIcon)
            .displayItems((parameters, output) -> tabFluids.accept(List.of(), output::accept))
            .build());

    public static final Map<String,Object> ENGINE_MAP = new HashMap<>();
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BCCore.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerList>> LIST_MENU = MENUS.register("list_menu",
        () -> BCContainerFactory.create(ContainerList::new));
    
    public BCCore(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        modEventBus.addListener(BCCoreConfig::onLoadConfig);
        modEventBus.addListener(BCCoreConfig::onReloadConfig);
        modEventBus.addListener(this::init);
        modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::registerCapabilities);

        BCCoreBlocks.registry(modEventBus);
        BCCoreItems.registry(modEventBus);
        BUILDCRAFT_TAB.addItemProvider(BCCoreItems::getCreativeTabItems);

        CREATIVE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
//        BCCoreRecipes.init();
        BCCoreConfig.registry();
        modContainer.registerConfig(Type.COMMON, BCCoreConfig.config);
        MessageManager.registerMessageClass(BCModules.CORE, MessageVolumeBoxes.class, MessageVolumeBoxes.HANDLER, MessageVolumeBoxes::toBytes, MessageVolumeBoxes::new/*, Side.CLIENT*/);
        if (dist == Dist.CLIENT) {
            IEventBus eventBus = NeoForge.EVENT_BUS;
            eventBus.addListener(RenderTickListener::renderOverlay);
            eventBus.addListener(RenderTickListener::renderLast);
        }
		BCCoreStatements.preInit();
    }

    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
            event.includeServer(),
            new BCCoreRecipes(event.getGenerator().getPackOutput(), event.getLookupProvider())
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            (stack, context) -> new ItemFragileFluidContainer.FragileFluidHandler(stack),
            BCCoreItems.FRAGILE_FLUID_SHARD.get()
        );

        registerEngineCapabilities(event, BCCoreBlocks.ENGINE_REDSTONE_TILE_BC8.get());
        registerEngineCapabilities(event, BCCoreBlocks.ENGINE_CREATIVE_TILE_BC8.get());
    }

    private static <BE extends BlockEntity & IBCCapabilityProvider> void registerEngineCapabilities(
        RegisterCapabilitiesEvent event, BlockEntityType<BE> blockEntityType
    ) {
        BCCapabilityRegistration.registerBlockEntity(event, MjAPI.CAP_CONNECTOR, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjAPI.CAP_RECEIVER, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjAPI.CAP_REDSTONE_RECEIVER, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjAPI.CAP_READABLE, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, MjAPI.CAP_PASSIVE_PROVIDER, blockEntityType);
        BCCapabilityRegistration.registerBlockEntity(event, Capabilities.ItemHandler.BLOCK, blockEntityType);
    }

    public void init(final FMLCommonSetupEvent event)
    {
        MarkerCache.registerCache(VolumeCache.INSTANCE);
        MarkerCache.registerCache(PathCache.INSTANCE);
    	EnumSpring.OIL.liquidBlock = BCEnergyFluids.OIL_BLOCK.get(0).get().defaultBlockState();
    	EnumSpring.OIL.tileConstructor = TileSpringOil::new;
    	BCCoreConfig.reloadConfig(MODID);
        BUILDCRAFT_TAB.setItem(BCCoreItems.WRENCH.get());
        FluidItemDrops.item = BCCoreItems.FRAGILE_FLUID_SHARD.get();
    }
    
    
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
    	public static final ResourceLocation TRUNK_LIGHT = ResourceLocation.parse("buildcraftcore:blocks/engine/trunk_light");
    	public static final ResourceLocation CHAMBER = ResourceLocation.parse("buildcraftlib:blocks/engine/chamber_base");
    	public static final ResourceLocation TRUNK = ResourceLocation.parse("buildcraftcore:blocks/engine/trunk");
    	public static final ResourceLocation ENGINE_MODEL = ResourceLocation.parse("buildcraftlib:block/engine_base");
        public static final ResourceLocation ENGINE_REDSTONE_TEXTURE_MODEL = ResourceLocation.parse("buildcraftcore:item/engine_redstone");
        public static final ResourceLocation ENGINE_CREATIVE_TEXTURE_MODEL = ResourceLocation.parse("buildcraftcore:item/engine_creative");
        public static final ResourceLocation ENGINE_STONE_TEXTURE_MODEL = ResourceLocation.parse("buildcraftenergy:item/engine_stone");
        public static final ResourceLocation ENGINE_IRON_TEXTURE_MODEL = ResourceLocation.parse("buildcraftenergy:item/engine_iron");
        public static final ResourceLocation ENGINE_LIGHT_SPRITE_MODEL = ResourceLocation.parse("buildcraftcore:block/engine_trunk_light_sprite");
        public static final ResourceLocation ENGINE_CHAMBER_SPRITE_MODEL = ResourceLocation.parse("buildcraftlib:block/engine_chamber_sprite");
    	
    	public ClientModEvents() {

		}
    	
        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(LIST_MENU.get(), GuiList::new);
        }

        @SubscribeEvent
        public static void onTextureStitchPost(TextureAtlasStitchedEvent event) {
            if (InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
                RenderEngine_BC8.reloadSprites(event.getAtlas());
            }
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        	BCCoreSprites.init();
        	DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, RenderVolumeBoxes.INSTANCE);
            event.enqueueWork(BCCoreItems::registerItemProperties);
        }
    	
        @SubscribeEvent
        public static void registryRender(EntityRenderersEvent.RegisterRenderers e) {

        	e.registerBlockEntityRenderer(BCCoreBlocks.ENGINE_REDSTONE_TILE_BC8.get(), RenderEngine_BC8::new);
        	e.registerBlockEntityRenderer(BCCoreBlocks.ENGINE_CREATIVE_TILE_BC8.get(), RenderEngine_BC8::new);
        	e.registerBlockEntityRenderer(BCCoreBlocks.MARKER_VOLUME_TILE_BC8.get(), RenderMarkerVolume::new);
        }
        
        @SubscribeEvent
        public static void onModelBakePre(RegisterAdditional event) {
        	event.register(new ModelResourceLocation(ENGINE_MODEL, "standalone"));
            event.register(new ModelResourceLocation(ENGINE_REDSTONE_TEXTURE_MODEL, "standalone"));
            event.register(new ModelResourceLocation(ENGINE_CREATIVE_TEXTURE_MODEL, "standalone"));
            event.register(new ModelResourceLocation(ENGINE_STONE_TEXTURE_MODEL, "standalone"));
            event.register(new ModelResourceLocation(ENGINE_IRON_TEXTURE_MODEL, "standalone"));
            event.register(new ModelResourceLocation(ENGINE_LIGHT_SPRITE_MODEL, "standalone"));
            event.register(new ModelResourceLocation(ENGINE_CHAMBER_SPRITE_MODEL, "standalone"));
        }
        
        @SubscribeEvent
        public static void onModelBake(ModifyBakingResult event) {
            RenderEngine_BC8.reloadSprites(
                event.getModels().get(new ModelResourceLocation(ENGINE_LIGHT_SPRITE_MODEL, "standalone")),
                event.getModels().get(new ModelResourceLocation(ENGINE_CHAMBER_SPRITE_MODEL, "standalone")),
                event.getModels().get(new ModelResourceLocation(ENGINE_REDSTONE_TEXTURE_MODEL, "standalone")),
                event.getModels().get(new ModelResourceLocation(ENGINE_CREATIVE_TEXTURE_MODEL, "standalone")),
                event.getModels().get(new ModelResourceLocation(ENGINE_STONE_TEXTURE_MODEL, "standalone")),
                event.getModels().get(new ModelResourceLocation(ENGINE_IRON_TEXTURE_MODEL, "standalone"))
            );
        	ModelEngine.init(event.getModels().get(new ModelResourceLocation(ENGINE_MODEL, "standalone")));
        	event.getModels().put(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(BCCore.MODID, "engine"), "type=wood"), new ModelEngine(RenderEngine_BC8.REDSTONE_BACK, RenderEngine_BC8.REDSTONE_SIDE));
        	event.getModels().put(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(BCCore.MODID, "engine"), "type=creative"), new ModelEngine(RenderEngine_BC8.CREATIVE_BACK, RenderEngine_BC8.CREATIVE_SIDE));
        	event.getModels().put(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(BCCore.MODID, "engine"), "type=stone"), new ModelEngine(RenderEngine_BC8.STONE_BACK, RenderEngine_BC8.STONE_SIDE));
        	event.getModels().put(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(BCCore.MODID, "engine"), "type=iron"), new ModelEngine(RenderEngine_BC8.IRON_BACK, RenderEngine_BC8.IRON_SIDE));
        	ModelEngine.release();
        }
        
        @SubscribeEvent
        public static void registerGeometryLoaders(RegisterGeometryLoaders event) {
            event.register(ResourceLocation.fromNamespaceAndPath(MODID, "fragile_fluid_container"), FragileFluidContainerModel.Loader.INSTANCE);
        }

        @SubscribeEvent
        public static void RegisterItemColor(RegisterColorHandlersEvent.Item event) {
            event.register(new FragileFluidContainerModel.Colors(), BCCoreItems.FRAGILE_FLUID_SHARD.get());
        }
        
    }


}

