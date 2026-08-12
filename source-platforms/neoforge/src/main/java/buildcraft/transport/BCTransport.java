/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import buildcraft.lib.internal.mj.MjCapabilities;

import buildcraft.api.BCModules;
import buildcraft.api.capabilities.BCCapabilityRegistration;
import buildcraft.transport.internal.pipe.PipeApi;
import buildcraft.builders.internal.schematic.legacy.SchematicBlockFactoryRegistry;
import buildcraft.lib.BCLibRegistries;
import buildcraft.lib.CreativeTabManager;
import buildcraft.lib.CreativeTabManager.CreativeTabBC;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.misc.CapUtil;
import buildcraft.core.BCCore;
import buildcraft.transport.net.MessageMultiPipeItem;
import buildcraft.transport.api2.TransportApi2;
import buildcraft.transport.pipe.SchematicBlockPipe;
import buildcraft.transport.wire.MessageWireSystems;
import buildcraft.transport.wire.MessageWireSystemsPowered;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

//@formatter:off
@Mod(BCTransport.MODID)
//@formatter:on
public class BCTransport {
    public static final String MODID = "buildcrafttransport";
    

    public static final CreativeTabBC tabPipes = (CreativeTabBC) CreativeTabManager.createTab("buildcraft.pipes").setRecipeFolderName("pipes");
    public static final CreativeTabBC tabPlugs = (CreativeTabBC) CreativeTabManager.createTab("buildcraft.plugs").setRecipeFolderName("plugs");

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "buildcraft");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIPES_TAB = CREATIVE_TABS.register("pipes", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.pipes"))
            .icon(tabPipes::makeIcon)
            .displayItems((parameters, output) -> tabPipes.accept(List.of(), output::accept))
            .build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLUGS_TAB = CREATIVE_TABS.register("plugs", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraft.plugs"))
            .icon(tabPlugs::makeIcon)
            .displayItems((parameters, output) -> tabPlugs.accept(List.of(), output::accept))
            .build());

    public BCTransport(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(BCTransportConfig::onConfigLoad);
        modEventBus.addListener(BCTransportConfig::onConfigReload);
        modEventBus.addListener(this::init);
    	modEventBus.addListener(this::gatherData);
        modEventBus.addListener(this::registerCapabilities);
        // Data generators are registered below.

        BCLibRegistries.initApiRegistries();
        TransportApi2.install();
        BCTransportRegistries.preInit();
        BCTransportConfig.preInit();
        BCTransportRecipes.preInit(modEventBus);
        BCTransportPipes.preInit();
        BCTransportPlugs.preInit();
        BCTransportBlocks.registry(modEventBus);
        BCTransportItems.registry(modEventBus);
        tabPipes.addItemProvider(BCTransportItems::getPipeTabItems);
        tabPlugs.addItemProvider(BCTransportItems::getPlugTabItems);
        BCCore.BUILDCRAFT_TAB.addItemProvider(BCTransportBlocks::getCreativeTabItems);
        BCTransportGuis.preInit(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        BCTransportStatements.preInit();

        modContainer.registerConfig(Type.COMMON, BCTransportConfig.config);

        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageWireSystems.class, MessageWireSystems.HANDLER, MessageWireSystems::toBytes, MessageWireSystems::new);
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageWireSystemsPowered.class, MessageWireSystemsPowered.HANDLER, MessageWireSystemsPowered::toBytes, MessageWireSystemsPowered::new);
    	MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageMultiPipeItem.class, MessageMultiPipeItem.HANDLER, MessageMultiPipeItem::toBytes, MessageMultiPipeItem::new);
        NeoForge.EVENT_BUS.register(BCTransportEventDist.class);
        
        SchematicBlockFactoryRegistry.registerFactory("pipe", 300, SchematicBlockPipe::predicate,
                SchematicBlockPipe::new);
    }
    
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        BlockEntityType<buildcraft.transport.tile.TilePipeHolder> pipeHolderType = BCTransportBlocks.PIPE_HOLDER_BE.get();

        BCCapabilityRegistration.registerBlockEntity(event, PipeApi.CAP_PIPE_HOLDER, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, PipeApi.CAP_PIPE, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, PipeApi.CAP_PLUG, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, PipeApi.CAP_INJECTABLE, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, CapUtil.CAP_ITEM_TRANSACTOR, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, CapUtil.CAP_FLUIDS, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_CONNECTOR, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_RECEIVER, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_REDSTONE_RECEIVER, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_READABLE, pipeHolderType);
        BCCapabilityRegistration.registerBlockEntity(event, MjCapabilities.CAP_PASSIVE_PROVIDER, pipeHolderType);

        BCCapabilityRegistration.registerBlockEntity(
            event, Capabilities.ItemHandler.BLOCK, BCTransportBlocks.FILTERREDBUFFER_BE.get()
        );
    }

    public void init(final FMLCommonSetupEvent event) {
    	BCTransportConfig.reloadConfig();
    	BCTransportRegistries.init();
    	tabPipes.setItem(BCTransportItems.PIPE_ITEM_DIAMOND.get());
    	tabPlugs.setItem(BCTransportItems.plugBlocker.get());
    }

    public void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
            event.includeServer(),
            new BCTransportRecipesProvider(event.getGenerator().getPackOutput(), event.getLookupProvider())
        );
    }
}
