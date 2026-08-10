/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders;

import buildcraft.builders.gui.MenuReplacer;
import buildcraft.builders.menu.ContainerArchitectTable;
import buildcraft.builders.menu.ContainerBuilder;
import buildcraft.builders.menu.ContainerElectronicLibrary;
import buildcraft.builders.menu.ContainerFiller;
import buildcraft.lib.gui.BCContainerFactory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BCBuildersGuis {
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BCBuilders.MODID);
    public static final RegistryObject<MenuType<ContainerArchitectTable>> MENU_ARCHITECT_TABLE = MENUS.register("architect_menu", () -> BCContainerFactory.create(ContainerArchitectTable::new));
    public static final RegistryObject<MenuType<ContainerBuilder>> MENU_BUILDER = MENUS.register("builder_menu", () -> BCContainerFactory.create(ContainerBuilder::new));
    public static final RegistryObject<MenuType<ContainerElectronicLibrary>> MENU_ELIBRARY = MENUS.register("elibrary_menu", () -> BCContainerFactory.create(ContainerElectronicLibrary::new));
    public static final RegistryObject<MenuType<ContainerFiller>> MENU_FILLER = MENUS.register("filler_menu", () -> BCContainerFactory.create(ContainerFiller::new));
    public static final RegistryObject<MenuType<MenuReplacer>> MENU_REPLACER = MENUS.register("replacer_menu", () -> BCContainerFactory.create(MenuReplacer::new));
    static void preInit(IEventBus modEventBus) {
    	MENUS.register(modEventBus);
    }
}
