/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.List;

import buildcraft.api.BCModules;
import buildcraft.api.facades.FacadeAPI;
import buildcraft.core.BCCore;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.CreativeTabManager.CreativeTabBC;
import buildcraft.silicon.plug.FacadeBlockStateInfo;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.transport.BCTransport;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(BCSilicon.MODID)
public class BCSilicon {
    public static final String MODID = "buildcraftsilicon";

    public static final CreativeTabBC tabPlugs = BCTransport.tabPlugs;
    public static final CreativeTabBC tabFacades = CreativeTabManager.createTab("buildcraft.facades")
        .setRecipeFolderName("facades");

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "buildcraft");
    public static final RegistryObject<CreativeModeTab> FACADES_TAB = CREATIVE_TABS.register("facades", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.facades"))
            .icon(tabFacades::makeIcon)
            .displayItems((parameters, output) -> tabFacades.accept(List.of(), output::accept))
            .build());

    public BCSilicon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(BCSilicon::commonSetup);
        modEventBus.addListener(BCSilicon::postInit);
        modEventBus.addListener(BCSilicon::gatherData);

        FacadeAPI.registry = FacadeStateManager.INSTANCE;

        BCSiliconStatements.preInit();
        BCSiliconPlugs.preInit();
        BCSiliconBlocks.registry(modEventBus);
        BCSiliconItems.registry(modEventBus);
        BCSiliconGuis.preInit(modEventBus);
        BCSiliconRecipes.preInit(modEventBus);
        CREATIVE_TABS.register(modEventBus);

        BCCore.BUILDCRAFT_TAB.addItemProvider(BCSiliconItems::getMainTabItems);
        tabPlugs.addItemProvider(BCSiliconItems::getPlugTabItems);
        tabFacades.addItemProvider(BCSiliconItems::getFacadeTabItems);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (BCSiliconItems.PLUG_FACADE_ITEM.isPresent()) {
                FacadeAPI.facadeItem = BCSiliconItems.PLUG_FACADE_ITEM.get();
            }
            FacadeStateManager.init();
        });
    }

    public static void postInit(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            if (BCSiliconItems.PLUG_FACADE_ITEM.isPresent()) {
                FacadeBlockStateInfo state = FacadeStateManager.previewState;
                if (state != null) {
                    FacadeInstance instance = FacadeInstance.createSingle(state, false);
                    tabFacades.setItem(BCSiliconItems.PLUG_FACADE_ITEM.get().createItemStack(instance));
                }
            }
            if (!BCModules.TRANSPORT.isLoaded() && BCSiliconItems.PLUG_GATE_ITEM.isPresent()) {
                tabPlugs.setItem(BCSiliconItems.PLUG_GATE_ITEM.get());
            }
        });
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
            event.includeServer(),
            new BCSiliconRecipesProvider(event.getGenerator().getPackOutput(), event.getLookupProvider())
        );
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        static {
            BCSiliconSprites.fmlPreInit();
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            BCSiliconClientGuis.clientInit(event);
            event.enqueueWork(() -> {
                BCSiliconItems.registerItemProperties();
                BCSiliconModels.init();
                ItemBlockRenderTypes.setRenderLayer(BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_BLOCK.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(BCSiliconBlocks.INTERGRATION_TABLE_BLOCK.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(BCSiliconBlocks.CHARGING_TABLE_BLOCK.get(), RenderType.cutout());
                ItemBlockRenderTypes.setRenderLayer(BCSiliconBlocks.PROGRAMMING_TABLE_BLOCK.get(), RenderType.translucent());
            });
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            BCSiliconModels.onBlockEntityRender(event);
        }

        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
            BCSiliconModels.registerItemColor(event);
        }

        @SubscribeEvent
        public static void onModelBake(ModifyBakingResult event) {
            BCSiliconModels.onModelBake(event);
        }
    }
}
