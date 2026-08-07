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
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BCBuildersGuis {
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BCBuilders.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerArchitectTable>> MENU_ARCHITECT_TABLE = MENUS.register("architect_menu", () -> BCContainerFactory.create(ContainerArchitectTable::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerBuilder>> MENU_BUILDER = MENUS.register("builder_menu", () -> BCContainerFactory.create(ContainerBuilder::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerElectronicLibrary>> MENU_ELIBRARY = MENUS.register("elibrary_menu", () -> BCContainerFactory.create(ContainerElectronicLibrary::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerFiller>> MENU_FILLER = MENUS.register("filler_menu", () -> BCContainerFactory.create(ContainerFiller::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MenuReplacer>> MENU_REPLACER = MENUS.register("replacer_menu", () -> BCContainerFactory.create(MenuReplacer::new));

    

    public void openGui(Player player) {
        openGui(player, 0, -1, 0);
    }

    public void openGui(Player player, BlockPos pos) {
        openGui(player, pos.getX(), pos.getY(), pos.getZ());
    }

    public void openGui(Player player, int x, int y, int z) {
//    	player.openMenu(null);
//        player.openGui(BCTransport, ordinal(), player.getLevel(), x, y, z);
    }
    static void preInit(IEventBus modEventBus) {
    	MENUS.register(modEventBus);
    }
}
